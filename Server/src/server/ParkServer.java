package server;

import java.io.IOException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import common.SubscriberRequest;

import common.ParkSubmittedReport;
import common.ParkReportRequest;
import common.ParkVisitorsReportResult;
import common.ParkUsageReportResult;
import common.ParkChangeRequest;
import common.Booking;
import common.ContactInfoValidator;
import common.Employee;
import common.Message;
import common.Order;
import common.TravelerProfile;
import common.ParkOption;
import common.WalkInRequest;
import common.ExitRequest;
import common.VisitorLoginResult;
import common.Promotion;
import common.PromotionRequest;
import common.VisitsReportRequest;
import common.VisitsReportResult;
import gui.ServerPortFrameController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class ParkServer extends AbstractServer {

    // Thread-safe set to track disconnected clients
    private final Set<ConnectionToClient> disconnectedClients =
        Collections.synchronizedSet(new HashSet<>());

    public ParkServer(int port) {
        super(port);
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        String clientInfo = "IP: " + ip + " | Host: " + host;
        client.setInfo("IP", clientInfo);
        System.out.println("Client connected: " + clientInfo);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.clientConnected(clientInfo);
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        handleDisconnection(client, "orderly disconnect");
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
        handleDisconnection(client, "connection lost");
    }

    private void handleDisconnection(ConnectionToClient client, String reason) {
        if (disconnectedClients.add(client)) {
            String clientInfo = "unknown";
            Object savedIP = client.getInfo("IP");
            if (savedIP != null)
                clientInfo = (String) savedIP;

            System.out.println("Processing disconnection for: " + clientInfo + " reason: " + reason);

            UserSessionManager.getInstance().removeConnection(client);

            if (ServerPortFrameController.instance != null) {
                ServerPortFrameController.instance.clientDisconnected(clientInfo);
            }
        }
        Object employeeUsername = client.getInfo("EMPLOYEE_USERNAME");
        if (employeeUsername != null) {
            EmployeeLoginRepository.logoutEmployee((String) employeeUsername);
        }
    }

    private ParkVisitorsReportResult getParkVisitorsReport(int parkId, java.time.LocalDate fromDate, java.time.LocalDate toDate) throws SQLException {
        int individualVisitors = 0;
        int organizedVisitors = 0;

        Connection conn = DBConnection.getStaticConnection();

        String sql =
                "SELECT organizedBooking, SUM(numberOfVisitors) AS totalVisitors " +
                "FROM booking " +
                "WHERE park_id = ? " +
                "AND DATE(visitorTime) BETWEEN ? AND ? " +
                "AND status IN ('APPROVED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "GROUP BY organizedBooking";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setDate(2, java.sql.Date.valueOf(fromDate));
        ps.setDate(3, java.sql.Date.valueOf(toDate));

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            boolean organized = rs.getBoolean("organizedBooking");
            int total = rs.getInt("totalVisitors");

            if (organized) {
                organizedVisitors += total;
            } else {
                individualVisitors += total;
            }
        }

        return new ParkVisitorsReportResult(individualVisitors, organizedVisitors);
    }

    private ArrayList<ParkUsageReportResult> getParkUsageReport(int parkId, java.time.LocalDate fromDate, java.time.LocalDate toDate) throws SQLException {
        ArrayList<ParkUsageReportResult> results = new ArrayList<>();

        Connection conn = DBConnection.getStaticConnection();

        String sql =
                "SELECT DATE(b.visitorTime) AS visitDate, " +
                "SUM(b.numberOfVisitors) AS visitorsCount, " +
                "p.maxCapacity AS maxCapacity, " +
                "(SUM(b.numberOfVisitors) / p.maxCapacity) * 100 AS usagePercent " +
                "FROM booking b " +
                "JOIN park p ON b.park_id = p.park_id " +
                "WHERE b.park_id = ? " +
                "AND DATE(b.visitorTime) BETWEEN ? AND ? " +
                "AND b.status IN ('APPROVED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "GROUP BY DATE(b.visitorTime), p.maxCapacity " +
                "HAVING usagePercent < 100 " +
                "ORDER BY visitDate";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setDate(2, java.sql.Date.valueOf(fromDate));
        ps.setDate(3, java.sql.Date.valueOf(toDate));

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            results.add(new ParkUsageReportResult(
                    rs.getString("visitDate"),
                    rs.getInt("visitorsCount"),
                    rs.getInt("maxCapacity"),
                    rs.getDouble("usagePercent")
            ));
        }

        return results;
    }

    private ArrayList<VisitsReportResult> getVisitsReport(VisitsReportRequest req) throws SQLException {
        ArrayList<VisitsReportResult> results = new ArrayList<>();

        Connection conn = DBConnection.getStaticConnection();

        String sql =
            "SELECT " +
            "CASE WHEN organizedBooking = 1 THEN 'Organized Group' ELSE 'Individual Visitors' END AS visitorType, " +
            "COUNT(*) AS visitsCount, " +
            "AVG(TIMESTAMPDIFF(MINUTE, entryTime, exitTime)) AS avgStayMinutes, " +
            "MIN(entryTime) AS firstEntryTime, " +
            "MAX(entryTime) AS lastEntryTime " +
            "FROM booking " +
            "WHERE park_id = ? " +
            "AND status = 'CHECKED_OUT' " +
            "AND entryTime IS NOT NULL " +
            "AND exitTime IS NOT NULL " +
            "AND DATE(entryTime) BETWEEN ? AND ? " +
            "GROUP BY visitorType";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, req.getParkId());
        ps.setDate(2, java.sql.Date.valueOf(req.getFromDate()));
        ps.setDate(3, java.sql.Date.valueOf(req.getToDate()));

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            results.add(new VisitsReportResult(
                rs.getString("visitorType"),
                rs.getInt("visitsCount"),
                rs.getDouble("avgStayMinutes"),
                rs.getString("firstEntryTime"),
                rs.getString("lastEntryTime")
            ));
        }

        return results;
    }

    private ArrayList<ParkSubmittedReport> getSubmittedReports(int parkId) throws SQLException {

        ArrayList<ParkSubmittedReport> reports = new ArrayList<>();

        String sql =
                "SELECT park_id, reportTitle, content, employee_id " +
                "FROM report " +
                "WHERE park_id = ?";

        Connection conn = DBConnection.getStaticConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            reports.add(new ParkSubmittedReport(
                    rs.getInt("park_id"),
                    rs.getString("reportTitle"),
                    rs.getString("content"),
                    rs.getInt("employee_id")
            ));
        }

        return reports;
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received from client: " + msg);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.log("Command received: " + ((msg instanceof Message) ? ((Message) msg).getCommand() : msg.toString()));

        if (!(msg instanceof Message)) return;

        Message message = (Message) msg;

        try {
            switch (message.getCommand()) {

                case "GET_ALL_ORDERS":
                    ArrayList<Order> orders = getAllOrders();
                    client.sendToClient(new Message("ORDERS_LIST", orders));
                    break;

                case "CHECK_ORDER_EXISTS":
                    int orderNum = (int) message.getData();
                    boolean exists = checkOrderExists(orderNum);
                    client.sendToClient(new Message("ORDER_EXISTS_RESULT", exists));
                    break;

                case "GET_ORDER":
                    int orderNumber = (int) message.getData();
                    Order order = getOrder(orderNumber);
                    if (order != null) {
                        client.sendToClient(new Message("ORDER_RESULT", order));
                    } else {
                        client.sendToClient(new Message("ERROR", "Order not found"));
                    }
                    break;

                case "UPDATE_ORDER":
                    Order updatedOrder = (Order) message.getData();
                    boolean updated = updateOrder(updatedOrder);
                    client.sendToClient(new Message("UPDATE_ORDER_RESULT", updated));
                    break;

                case "CREATE_BOOKING":
                    Booking bookingToCreate = (Booking) message.getData();
                    BookingAvailabilityResult bookingAvailabilityResult = createBooking(bookingToCreate, false);
                    if (bookingAvailabilityResult.requiresWaitlistConfirmation) {
                        client.sendToClient(new Message("CREATE_BOOKING_REQUIRES_WAITLIST_CONFIRMATION",
                            new Object[] { bookingAvailabilityResult.booking,
                                "The selected park is full for this date and time. You can join the waiting list or choose another time." }));
                    } else {
                        client.sendToClient(new Message("CREATE_BOOKING_RESULT", bookingAvailabilityResult.booking));
                    }
                    break;

                case "CREATE_WAITLIST_BOOKING":
                    Booking waitlistBooking = (Booking) message.getData();
                    Booking createdWaitlistBooking = createBooking(waitlistBooking);
                    client.sendToClient(new Message("CREATE_BOOKING_RESULT", createdWaitlistBooking));
                    break;

                case "GET_TRAVELER_BOOKINGS":
                    String travelerId = (String) message.getData();
                    client.sendToClient(new Message("TRAVELER_BOOKINGS_RESULT", getTravelerBookings(travelerId)));
                    break;

                case "GET_PARKS":
                    client.sendToClient(new Message("PARKS_RESULT", getParkOptions()));
                    break;

                case "GET_PARK_PRICES":
                    client.sendToClient(new Message("PARK_PRICES_RESULT", getParkPrices()));
                    break;

                case "GET_TRAVELER_PROFILE":
                    String profileTravelerId = (String) message.getData();
                    client.sendToClient(new Message("TRAVELER_PROFILE_RESULT", getTravelerProfile(profileTravelerId)));
                    break;

                case "UPDATE_TRAVELER_PROFILE":
                    TravelerProfile profileToUpdate = (TravelerProfile) message.getData();
                    client.sendToClient(new Message("UPDATE_TRAVELER_PROFILE_RESULT", updateTravelerProfile(profileToUpdate)));
                    break;

                case "UPDATE_BOOKING":
                    Booking bookingToUpdate = (Booking) message.getData();
                    client.sendToClient(new Message("UPDATE_BOOKING_RESULT", updateBooking(bookingToUpdate)));
                    break;

                case "CANCEL_BOOKING":
                    Booking bookingToCancel = (Booking) message.getData();
                    client.sendToClient(new Message("CANCEL_BOOKING_RESULT", cancelBooking(bookingToCancel)));
                    break;

                case "CONFIRM_BOOKING":
                    String[] confirmationData = (String[]) message.getData();
                    boolean confirmed = BookingLifecycleService.confirmBooking(confirmationData[0], confirmationData[1]);
                    client.sendToClient(new Message("CONFIRM_BOOKING_RESULT", confirmed));
                    break;

                case "PAY_IN_ADVANCE":
                    Booking payBooking = (Booking) message.getData();
                    boolean payResult = payInAdvance(payBooking);
                    client.sendToClient(new Message("PAY_IN_ADVANCE_RESULT", payResult));
                    break;

                case "TRAVELER_LOGIN":
                    String nationalId = (String) message.getData();
                    VisitorLoginResult loginResult = loginOrRegisterVisitor(nationalId);

                    ConnectionToClient oldConnection = UserSessionManager.getInstance()
                        .loginUser(loginResult.getTravelerId(), client);

                    if (oldConnection != null) {
                        try {
                            oldConnection.sendToClient(new Message("FORCE_LOGOUT",
                                "You have logged in from another computer."));
                            oldConnection.close();
                        } catch (IOException e) {
                            System.out.println("Error disconnecting old session: " + e.getMessage());
                        }
                    }

                    client.sendToClient(new Message("VISITOR_LOGIN_RESULT", loginResult));
                    break;

                case "TRAVELER_LOGOUT":
                    String travelerIdToLogout = (String) message.getData();
                    UserSessionManager.getInstance().logoutUser(travelerIdToLogout);
                    System.out.println("[ParkServer] User " + travelerIdToLogout + " logged out from client.");
                    client.sendToClient(new Message("LOGOUT_RESULT", true));
                    break;

                case "EMPLOYEE_LOGIN":
                    String[] credentials = (String[]) message.getData();
                    Object loginResult1 = EmployeeLoginRepository.loginEmployee(credentials[0], credentials[1]);
                    if (loginResult1 == null) {
                        client.sendToClient(new Message("EMPLOYEE_LOGIN_FAILED", "Invalid username or password."));
                    } else if (loginResult1.equals("ALREADY_LOGGED_IN")) {
                        client.sendToClient(new Message("EMPLOYEE_LOGIN_FAILED", "This user is already logged in."));
                    } else {
                        Employee employee = (Employee) loginResult1;
                        client.setInfo("EMPLOYEE_USERNAME", employee.getUsername());
                        client.setInfo("EMPLOYEE_PARK_ID", employee.getParkId());
                        client.setInfo("EMPLOYEE_ROLE", employee.getRole());
                        client.sendToClient(new Message("EMPLOYEE_LOGIN_SUCCESS", loginResult1));
                    }
                    break;

                case "EMPLOYEE_LOGOUT":
                    String logoutUsername = (String) message.getData();
                    EmployeeLoginRepository.logoutEmployee(logoutUsername);
                    break;


                 case "GET_PARK_CURRENT_VISITORS":
                     int parkIdForVisitors = (int) message.getData();
                     int currentVisitors = getParkCurrentVisitors(parkIdForVisitors);
                     client.sendToClient(new Message("PARK_VISITORS_RESULT", currentVisitors));
                     break;

                 case "GET_PARK_SETTINGS":
                     int parkIdForSettings = (int) message.getData();
                     client.sendToClient(new Message("PARK_SETTINGS_RESULT", getParkSettings(parkIdForSettings)));
                     break;

                 case "GET_EFFECTIVE_AVAILABLE_SPOTS":
                     int parkIdForSpots = (int) message.getData();
                     int effectiveSpots = getEffectiveAvailableSpots(parkIdForSpots);
                     client.sendToClient(new Message("EFFECTIVE_AVAILABLE_SPOTS_RESULT", effectiveSpots));
                     break;

                 case "GET_BOOKING_BY_ID":
                     String bookingIdToFind = (String) message.getData();
                     Booking foundBooking = getBookingById(bookingIdToFind);
                     client.sendToClient(new Message("BOOKING_RESULT", foundBooking));
                     break;

                 case "CHECK_IN_VISITOR":
                     Booking bookingToCheckIn = (Booking) message.getData();
                     Integer employeeParkId = (Integer) client.getInfo("EMPLOYEE_PARK_ID");
                     if (employeeParkId == null) {
                         throw new IllegalArgumentException("Employee park is missing. Please log in again.");
                     }
                     boolean checkInSuccess = checkInVisitor(bookingToCheckIn, employeeParkId);
                     client.sendToClient(new Message("CHECK_IN_RESULT", checkInSuccess));
                     break;

                 case "CHECK_OUT_VISITOR":
                     ExitRequest exitRequest = (ExitRequest) message.getData();
                     boolean checkOutSuccess = checkOutVisitor(exitRequest);
                     client.sendToClient(new Message("CHECK_OUT_RESULT", checkOutSuccess));
                     break;

                 case "WALK_IN_VISITOR":
                     WalkInRequest walkInRequest = (WalkInRequest) message.getData();
                     Booking walkInBooking = processWalkIn(walkInRequest);
                     client.sendToClient(new Message("WALK_IN_RESULT", walkInBooking));
                     break;

                 case "PARK_CHANGE_REQUEST":
                     ParkChangeRequest changeRequest = (ParkChangeRequest) message.getData();
                     handleParkChangeRequest(changeRequest, client);
                     break;

                 case "PARK_CHANGE_APPROVAL":
                     Object[] approvalData = (Object[]) message.getData();
                     handleParkChangeApproval((String) approvalData[0], (boolean) approvalData[1], client);
                     break;

                 case "GET_PROMOTIONS":
                     int parkIdForPromos = (int) message.getData();
                     client.sendToClient(new Message("PROMOTIONS_LIST_RESULT", getPromotions(parkIdForPromos)));
                     break;

                 case "GET_PENDING_REQUESTS":
                     int parkIdForPending = (int) message.getData();
                     sendPendingRequestsToManager(parkIdForPending, client);
                     break;

                 case "PARK_VISITORS_REPORT":
                	    ParkReportRequest visitorsRequest = (ParkReportRequest) message.getData();

                	    ParkVisitorsReportResult visitorsResult =
                	            getParkVisitorsReport(
                	                    visitorsRequest.getParkId(),
                	                    visitorsRequest.getFromDate(),
                	                    visitorsRequest.getToDate());
                	    saveParkVisitorsReport(
                	            visitorsRequest.getParkId(),
                	            visitorsRequest.getEmployeeId(),
                	            visitorsRequest.getFromDate(),
                	            visitorsRequest.getToDate(),
                	            visitorsResult
                	    );

                	    client.sendToClient(
                	            new Message("PARK_VISITORS_REPORT_RESULT", visitorsResult));
                	    break;

                	case "PARK_USAGE_REPORT":
                	    ParkReportRequest usageRequest = (ParkReportRequest) message.getData();

                	    ArrayList<ParkUsageReportResult> usageResults =
                	            getParkUsageReport(
                	                    usageRequest.getParkId(),
                	                    usageRequest.getFromDate(),
                	                    usageRequest.getToDate());
                	    saveParkUsageReport(
                	            usageRequest.getParkId(),
                	            usageRequest.getEmployeeId(),
                	            usageRequest.getFromDate(),
                	            usageRequest.getToDate(),
                	            usageResults
                	    );

                	    client.sendToClient(
                	            new Message("PARK_USAGE_REPORT_RESULT", usageResults));
                	    break;

                 case "GET_VISITS_REPORT":
                     VisitsReportRequest visitsReq = (VisitsReportRequest) message.getData();
                     ArrayList<VisitsReportResult> visitsResult = getVisitsReport(visitsReq);
                     client.sendToClient(new Message("VISITS_REPORT_RESULT", visitsResult));
                     break;

                 case "PROMOTION_REQUEST":
                     PromotionRequest promoRequest = (PromotionRequest) message.getData();
                     handlePromotionRequest(promoRequest, client);
                     break;

                 case "PROMOTION_APPROVAL":
                     Object[] promoApprovalData = (Object[]) message.getData();
                     handlePromotionApproval((String) promoApprovalData[0], (boolean) promoApprovalData[1], client);
                     break;

                 case "GET_SUBMITTED_REPORTS":
                	    int parkIdForReports = (int) message.getData();
                	    ArrayList<ParkSubmittedReport> reports = getSubmittedReports(parkIdForReports);
                	    client.sendToClient(new Message("SUBMITTED_REPORTS_RESULT", reports));
                	    break;

                 case "GET_TODAY_BOOKINGS":
                	    int parkIdForToday = (int) message.getData();
                	    ArrayList<Booking> todayBookings = getTodayBookings(parkIdForToday);
                	    client.sendToClient(new Message("TODAY_BOOKINGS_RESULT", todayBookings));
                	    break;

                 case "REGISTER_TRAVELER":
                	    SubscriberRequest subReq = (SubscriberRequest) message.getData();
                	    String registerResult = registerTraveler(subReq);
                	    client.sendToClient(new Message("REGISTER_TRAVELER_RESULT", registerResult));
                	    break;

                default:
                    client.sendToClient(new Message("ERROR", "Unknown command: " + message.getCommand()));
            }

        } catch (Exception e) {
            try {
                client.sendToClient(new Message("ERROR", e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void saveParkVisitorsReport(int parkId, int employeeId,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            ParkVisitorsReportResult result) throws SQLException {

            Connection conn = DBConnection.getStaticConnection();

            String reportTitle = "Park Visitors Report - " + fromDate.getMonth() + " " + fromDate.getYear();

            String content = "Visitors Report\n\n" + "Period: " + fromDate + " to " + toDate + "\n" + "Individual Visitors: " + result.getIndividualVisitors() + "\n" + "Organized Groups: " + result.getOrganizedVisitors() + "\n" + "Total Visitors: " + result.getTotalVisitors();

            String sql = "INSERT INTO report (park_id, reportTitle, content, employee_id) " + "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parkId);
            ps.setString(2, reportTitle);
            ps.setString(3, content);
            ps.setInt(4, employeeId);

            ps.executeUpdate();
    }

    private void saveParkUsageReport(int parkId, int employeeId,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            ArrayList<ParkUsageReportResult> results) throws SQLException {

            Connection conn = DBConnection.getStaticConnection();

            String reportTitle = "Park Usage Report - " + fromDate.getMonth() + " " + fromDate.getYear();

            StringBuilder content = new StringBuilder();
            content.append("Usage Report\n\n");
            content.append("Period: ").append(fromDate).append(" to ").append(toDate).append("\n\n");
            for (ParkUsageReportResult r : results) {
                content.append(r.getDate()).append(": ")
                       .append(r.getVisitorsCount()).append("/").append(r.getMaxCapacity())
                       .append(" (").append(String.format("%.1f", r.getUsagePercent())).append("%)\n");
            }

            String sql = "INSERT INTO report (park_id, reportTitle, content, employee_id) " + "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parkId);
            ps.setString(2, reportTitle);
            ps.setString(3, content.toString());
            ps.setInt(4, employeeId);

            ps.executeUpdate();
    }


    private String registerTraveler(SubscriberRequest req) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        int nationalIdNumber;
        try {
            nationalIdNumber = Integer.parseInt(req.getNationalId().trim());
        } catch (NumberFormatException e) {
            return "National ID must be a number.";
        }

        boolean isGuide = SubscriberRequest.TYPE_GUIDE.equals(req.getType());
        boolean isClubMember = SubscriberRequest.TYPE_CLUB_MEMBER.equals(req.getType());

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            String selectSql = "SELECT traveler_id, user_id, guide, clubMember FROM traveler WHERE nationalId = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setInt(1, nationalIdNumber);
            ResultSet rs = selectPs.executeQuery();

            if (rs.next()) {
                String travelerId = rs.getString("traveler_id");
                String userId = rs.getString("user_id");
                boolean alreadyGuide = rs.getBoolean("guide");
                boolean alreadyClub = rs.getBoolean("clubMember");

                if (isGuide && alreadyGuide) {
                    conn.rollback();
                    return "This person is already registered as a tour guide.";
                }
                if (isClubMember && alreadyClub) {
                    conn.rollback();
                    return "This person is already registered as a club member.";
                }

                String updateTravelerSql =
                    "UPDATE traveler SET guide = ?, clubMember = ?, familyMembers = ?, creditCard = ? WHERE traveler_id = ?";
                PreparedStatement updTravelerPs = conn.prepareStatement(updateTravelerSql);
                updTravelerPs.setBoolean(1, alreadyGuide || isGuide);
                updTravelerPs.setBoolean(2, alreadyClub || isClubMember);
                if (isClubMember) {
                    updTravelerPs.setInt(3, req.getFamilyMembers());
                    if (req.getCreditCard() == null) updTravelerPs.setNull(4, Types.VARCHAR);
                    else updTravelerPs.setString(4, req.getCreditCard());
                } else {
                    updTravelerPs.setNull(3, Types.INTEGER);
                    updTravelerPs.setNull(4, Types.VARCHAR);
                }
                updTravelerPs.setString(5, travelerId);
                updTravelerPs.executeUpdate();

                String updateUserSql = "UPDATE `user` SET firstName = ?, lastName = ?, email = ?, phoneNumber = ? WHERE user_id = ?";
                PreparedStatement updUserPs = conn.prepareStatement(updateUserSql);
                updUserPs.setString(1, req.getFirstName());
                updUserPs.setString(2, req.getLastName());
                updUserPs.setString(3, req.getEmail());
                updUserPs.setString(4, req.getPhoneNumber());
                updUserPs.setString(5, userId);
                updUserPs.executeUpdate();

                conn.commit();
                return "SUCCESS";
            } else {
                String travelerId = UUID.randomUUID().toString();

                String insertUserSql = "INSERT INTO `user` (user_id, firstName, lastName, email, phoneNumber) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertUserPs = conn.prepareStatement(insertUserSql);
                insertUserPs.setString(1, travelerId);
                insertUserPs.setString(2, req.getFirstName());
                insertUserPs.setString(3, req.getLastName());
                insertUserPs.setString(4, req.getEmail());
                insertUserPs.setString(5, req.getPhoneNumber());
                insertUserPs.executeUpdate();

                String insertTravelerSql =
                    "INSERT INTO traveler (traveler_id, nationalId, guide, clubMember, user_id, familyMembers, creditCard) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement insertTravelerPs = conn.prepareStatement(insertTravelerSql);
                insertTravelerPs.setString(1, travelerId);
                insertTravelerPs.setInt(2, nationalIdNumber);
                insertTravelerPs.setBoolean(3, isGuide);
                insertTravelerPs.setBoolean(4, isClubMember);
                insertTravelerPs.setString(5, travelerId);
                if (isClubMember) {
                    insertTravelerPs.setInt(6, req.getFamilyMembers());
                    if (req.getCreditCard() == null) insertTravelerPs.setNull(7, Types.VARCHAR);
                    else insertTravelerPs.setString(7, req.getCreditCard());
                } else {
                    insertTravelerPs.setNull(6, Types.INTEGER);
                    insertTravelerPs.setNull(7, Types.VARCHAR);
                }
                insertTravelerPs.executeUpdate();

                conn.commit();
                return "SUCCESS";
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }


    private ArrayList<Booking> getTodayBookings(int parkId) throws SQLException {
        ArrayList<Booking> list = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();

        String sql = "SELECT * FROM booking " +
                     "WHERE park_id = ? " +
                     "AND DATE(visitorTime) = CURDATE() " +
                     "AND status IN ('PENDING', 'CHECKED_IN') " +
                     "ORDER BY visitorTime ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(Utils.mapBooking(rs));
        }
        return list;
    }


    private ArrayList<Order> getAllOrders() throws SQLException {
        ArrayList<Order> list = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM `Order`");
        while (rs.next()) {
            list.add(new Order(
                rs.getInt("order_number"), rs.getDate("order_date"),
                rs.getInt("number_of_visitors"), rs.getInt("confirmation_code"),
                rs.getInt("subscriber_id"), rs.getDate("date_of_placing_order")
            ));
        }
        return list;
    }

    private Order getOrder(int orderNumber) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM `Order` WHERE order_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, orderNumber);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Order(
                rs.getInt("order_number"), rs.getDate("order_date"),
                rs.getInt("number_of_visitors"), rs.getInt("confirmation_code"),
                rs.getInt("subscriber_id"), rs.getDate("date_of_placing_order")
            );
        }
        return null;
    }

    private boolean checkOrderExists(int orderNumber) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT order_number FROM `Order` WHERE order_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, orderNumber);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private boolean updateOrder(Order order) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "UPDATE `Order` SET order_date = ?, number_of_visitors = ? WHERE order_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDate(1, order.getOrderDate());
        ps.setInt(2, order.getNumberOfVisitors());
        ps.setInt(3, order.getOrderNumber());
        return ps.executeUpdate() > 0;
    }

    private synchronized BookingAvailabilityResult createBooking(Booking booking, boolean allowWaitlistCreation) throws SQLException {
        validateBooking(booking);

        Connection conn = DBConnection.getStaticConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            if (Utils.hasActiveBookingAtTime(conn, booking.getTravelerId(), booking.getVisitorTime())) {
                conn.rollback();
                throw new IllegalArgumentException(
                    "You already have a booking at this date and time. Please choose a different time slot.");
            }

            String bookingId = String.valueOf(1000000 + new java.util.Random().nextInt(9000000));
            int capacity = Utils.getParkEffectiveCapacity(conn, booking.getParkId());
            int confirmedVisitors = getConfirmedVisitorsForSlot(conn, booking.getParkId(), booking.getVisitorTime());
            boolean isFull = confirmedVisitors + booking.getNumberOfVisitors() > capacity;
            String status = isFull
                ? Booking.STATUS_WAITING_LIST
                : Booking.STATUS_CONFIRMED;

            if (isFull && !allowWaitlistCreation) {
                conn.rollback();
                return new BookingAvailabilityResult(true, new Booking(null, booking.getTravelerId(), booking.getTravelerName(),
                    booking.getTravelerEmail(), booking.getTravelerPhoneNumber(), booking.getParkId(), booking.getNumberOfVisitors(),
                    booking.getVisitorTime(), Booking.STATUS_WAITING_LIST, false, 0));
            }
            String travelerID = booking.getTravelerId();

            double price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors(), travelerID, true);

            String sql = "INSERT INTO booking (booking_id, traveler_id, travelerName, travelerEmail, travelerPhoneNumber, "
                + "park_id, numberOfVisitors, visitorTime, status, organizedBooking, price) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, bookingId);
            ps.setString(2, travelerID);
            ps.setString(3, booking.getTravelerName().trim());
            ps.setString(4, booking.getTravelerEmail().trim());
            ps.setString(5, booking.getTravelerPhoneNumber().trim());
            ps.setInt(6, booking.getParkId());
            ps.setInt(7, booking.getNumberOfVisitors());
            ps.setTimestamp(8, Timestamp.valueOf(booking.getVisitorTime()));
            ps.setString(9, status);
            ps.setBoolean(10, false);
            ps.setDouble(11, price);
            ps.executeUpdate();

            if (Booking.STATUS_WAITING_LIST.equals(status)) {
                String waitingListId = getOrCreateWaitingList(conn, booking.getParkId(), booking.getVisitorTime());
                addWaitingListEntry(conn, waitingListId, bookingId);
            }

            conn.commit();
            return new BookingAvailabilityResult(false, new Booking(bookingId, booking.getTravelerId(), booking.getTravelerName(),
                booking.getTravelerEmail(), booking.getTravelerPhoneNumber(),
                booking.getParkId(), booking.getNumberOfVisitors(), booking.getVisitorTime(), status, false, price));
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private synchronized Booking createBooking(Booking booking) throws SQLException {
        return createBooking(booking, true).booking;
    }

    private int getConfirmedVisitorsForSlot(Connection conn, int parkId, LocalDateTime visitorTime) throws SQLException {
        String sql = "SELECT COALESCE(SUM(numberOfVisitors), 0) AS confirmedVisitors "
            + "FROM booking WHERE park_id = ? AND visitorTime = ? AND status = ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setTimestamp(2, Timestamp.valueOf(visitorTime));
        ps.setString(3, Booking.STATUS_CONFIRMED);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("confirmedVisitors") : 0;
    }

    private String getOrCreateWaitingList(Connection conn, int parkId, LocalDateTime slotTime) throws SQLException {
        String selectSql = "SELECT waitingList_id FROM WaitingList WHERE park_id = ? AND slot_time = ? FOR UPDATE";
        PreparedStatement selectPs = conn.prepareStatement(selectSql);
        selectPs.setInt(1, parkId);
        selectPs.setTimestamp(2, Timestamp.valueOf(slotTime));
        ResultSet rs = selectPs.executeQuery();
        if (rs.next()) {
            return rs.getString("waitingList_id");
        }

        String waitingListId = UUID.randomUUID().toString();
        String insertSql = "INSERT INTO WaitingList (waitingList_id, park_id, slot_time, status, created_at) VALUES (?, ?, ?, ?, NOW())";
        PreparedStatement insertPs = conn.prepareStatement(insertSql);
        insertPs.setString(1, waitingListId);
        insertPs.setInt(2, parkId);
        insertPs.setTimestamp(3, Timestamp.valueOf(slotTime));
        insertPs.setString(4, "OPEN");
        insertPs.executeUpdate();
        return waitingListId;
    }

    private void addWaitingListEntry(Connection conn, String waitingListId, String bookingId) throws SQLException {
        String sql = "INSERT INTO WaitingListEntry (id, waitingList_id, booking_id, registered_at, status, updated_at) "
            + "VALUES (?, ?, ?, NOW(), ?, NOW())";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, UUID.randomUUID().toString());
        ps.setString(2, waitingListId);
        ps.setString(3, bookingId);
        ps.setString(4, "WAITING");
        ps.executeUpdate();
    }

    private static final class BookingAvailabilityResult {
        private final boolean requiresWaitlistConfirmation;
        private final Booking booking;

        private BookingAvailabilityResult(boolean requiresWaitlistConfirmation, Booking booking) {
            this.requiresWaitlistConfirmation = requiresWaitlistConfirmation;
            this.booking = booking;
        }
    }

    private ArrayList<Booking> getTravelerBookings(String travelerId) throws SQLException {
        ArrayList<Booking> bookings = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM booking WHERE traveler_id = ? ORDER BY visitorTime";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            bookings.add(Utils.mapBooking(rs));
        }

        return bookings;
    }

    private boolean payInAdvance(Booking booking) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "UPDATE booking SET paid = 1, price = ? WHERE booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDouble(1, booking.getPrice());
        ps.setString(2, booking.getBookingId());
        return ps.executeUpdate() > 0;
    }

    private boolean updateBooking(Booking booking) throws SQLException {

        Connection conn = DBConnection.getStaticConnection();
        double price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors(), booking.getTravelerId(), true);

        String sql = "UPDATE booking SET travelerName = ?, travelerEmail = ?, travelerPhoneNumber = ?, "
            + "park_id = ?, numberOfVisitors = ?, visitorTime = ?, status = ?, price = ? "
            + "WHERE booking_id = ? AND traveler_id = ? "
            + "AND status NOT IN (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, booking.getTravelerName().trim());
        ps.setString(2, booking.getTravelerEmail().trim());
        ps.setString(3, booking.getTravelerPhoneNumber().trim());
        ps.setInt(4, booking.getParkId());
        ps.setInt(5, booking.getNumberOfVisitors());
        ps.setTimestamp(6, Timestamp.valueOf(booking.getVisitorTime()));
        ps.setString(7, Booking.STATUS_PENDING);
        ps.setDouble(8, price);
        ps.setString(9, booking.getBookingId());
        ps.setString(10, booking.getTravelerId());
        ps.setString(11, Booking.STATUS_CANCELLED);
        ps.setString(12, Booking.STATUS_CHECKED_IN);
        ps.setString(13, Booking.STATUS_CHECKED_OUT);
        return ps.executeUpdate() > 0;
    }

    private synchronized boolean cancelBooking(Booking booking) throws SQLException {
        if (booking == null || booking.getBookingId() == null || booking.getTravelerId() == null) {
            throw new IllegalArgumentException("Booking details are missing.");
        }

        Connection conn = DBConnection.getStaticConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            Booking existingBooking = Utils.getBookingByIdForUpdate(conn, booking.getBookingId());
            if (existingBooking == null || !booking.getTravelerId().equals(existingBooking.getTravelerId())) {
                conn.rollback();
                return false;
            }

            String sql = "UPDATE booking SET status = ? WHERE booking_id = ? AND traveler_id = ? "
                + "AND status NOT IN (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Booking.STATUS_CANCELLED);
            ps.setString(2, booking.getBookingId());
            ps.setString(3, booking.getTravelerId());
            ps.setString(4, Booking.STATUS_CANCELLED);
            ps.setString(5, Booking.STATUS_CHECKED_IN);
            ps.setString(6, Booking.STATUS_CHECKED_OUT);
            boolean cancelled = ps.executeUpdate() > 0;

            if (cancelled) {
                Utils.updateWaitingListEntryByBooking(conn, booking.getBookingId(), "WAITING", "CANCELLED");
                if (Booking.STATUS_CONFIRMED.equals(existingBooking.getStatus())
                    || Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(existingBooking.getStatus())
                    || Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(existingBooking.getStatus())) {
                    BookingLifecycleService.handleConfirmedBookingCancelled(conn, existingBooking);
                }
            }

            conn.commit();
            return cancelled;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private void validateBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking details are missing.");
        }
        if (booking.getTravelerId() == null || booking.getTravelerId().isBlank()) {
            throw new IllegalArgumentException("Traveler is missing.");
        }
        ContactInfoValidator.requireName(booking.getTravelerName(), "Traveler name");
        ContactInfoValidator.requireEmail(booking.getTravelerEmail());
        ContactInfoValidator.requirePhoneNumber(booking.getTravelerPhoneNumber());

        if (booking.getNumberOfVisitors() < 1 || booking.getNumberOfVisitors() > 16) {
            throw new IllegalArgumentException("Visitors must be between 1 and 16.");
        }
        if (booking.getVisitorTime() == null || !booking.getVisitorTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time must be in the future.");
        }
        if (booking.getVisitorTime().toLocalTime().isAfter(java.time.LocalTime.of(16, 0))) {
            throw new IllegalArgumentException("Bookings can only be made until 16:00.");
        }
    }

    private double calculatePrice(Connection conn, int parkId, int numberOfVisitors, String travelerId, boolean digitalBooking) throws SQLException {
        String sql = "SELECT pricePerPerson FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }

        double parkPrice = rs.getInt("pricePerPerson");
        boolean guide = Utils.isGuide(conn, travelerId);
        if (digitalBooking){
            parkPrice = (double) (parkPrice * Utils.DIGITAL_BOOKING_DISCOUNT); // Apply 15% discount for digital bookings
            System.out.println("Applying digital booking discount for traveler: " + travelerId);
            if (guide) {
                parkPrice = (double) (parkPrice * Utils.GUIDE_DISCOUNT); // Apply 25% discount for guides
                if (numberOfVisitors > 1){
                    numberOfVisitors -= 1;
                }
            }
        }
        if (!guide && Utils.isClubMember(conn, travelerId)) {
            System.out.println("Applying club member discount for traveler: " + travelerId);
            parkPrice = (double) (parkPrice * Utils.CLUB_MEMBER_DISCOUNT); // Apply 10% discount for club members
        }

        return (double) (parkPrice * numberOfVisitors);
    }

    private ArrayList<ParkOption> getParkOptions() throws SQLException {
        ArrayList<ParkOption> parks = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT park_id, name, pricePerPerson FROM park ORDER BY park_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            parks.add(new ParkOption(rs.getInt("park_id"), rs.getString("name"), rs.getInt("pricePerPerson")));
        }

        return parks;
    }

    private Map<Integer, Integer> getParkPrices() throws SQLException {
        Map<Integer, Integer> prices = new HashMap<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT park_id, pricePerPerson FROM park ORDER BY park_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            prices.put(rs.getInt("park_id"), rs.getInt("pricePerPerson"));
        }

        return prices;
    }

    private TravelerProfile getTravelerProfile(String travelerId) throws SQLException {
        if (travelerId == null || travelerId.isBlank()) {
            throw new IllegalArgumentException("Traveler is missing.");
        }

        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT u.firstName, u.lastName, u.email, u.phoneNumber, t.clubMember "
            + "FROM traveler t JOIN `user` u ON u.user_id = t.traveler_id "
            + "WHERE t.traveler_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Traveler profile was not found.");
        }

        return new TravelerProfile(
            travelerId,
            rs.getString("firstName"),
            rs.getString("lastName"),
            rs.getString("email"),
            rs.getString("phoneNumber"),
            rs.getBoolean("clubMember")
        );
    }

    private boolean updateTravelerProfile(TravelerProfile profile) throws SQLException {
        if (profile == null || profile.getTravelerId() == null || profile.getTravelerId().isBlank()) {
            throw new IllegalArgumentException("Traveler profile is missing.");
        }

        String firstName = ContactInfoValidator.requireName(profile.getFirstName(), "First name");
        String lastName = ContactInfoValidator.requireName(profile.getLastName(), "Last name");
        String email = ContactInfoValidator.requireEmail(profile.getEmail());
        String phoneNumber = ContactInfoValidator.requirePhoneNumber(profile.getPhoneNumber());

        Connection conn = DBConnection.getStaticConnection();
        String sql = "UPDATE `user` SET firstName = ?, lastName = ?, email = ?, phoneNumber = ? WHERE user_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, firstName);
        ps.setString(2, lastName);
        ps.setString(3, email);
        ps.setString(4, phoneNumber);
        ps.setString(5, profile.getTravelerId());
        try {
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Email address is already used by another user.");
        }
    }

    private VisitorLoginResult loginOrRegisterVisitor(String nationalId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        int nationalIdNumber = Integer.parseInt(nationalId);

        String selectSql = "SELECT traveler_id, guide, clubMember FROM traveler WHERE nationalId = ?";
        PreparedStatement selectPs = conn.prepareStatement(selectSql);
        selectPs.setInt(1, nationalIdNumber);
        ResultSet rs = selectPs.executeQuery();
        if (rs.next()) {
            boolean isGuide = rs.getBoolean("guide");
            boolean isClubMember = rs.getBoolean("clubMember");
            return new VisitorLoginResult(rs.getString("traveler_id"), nationalId, false, isGuide, isClubMember);
        }

        String travelerId = UUID.randomUUID().toString();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
        	String insertUserSql = "INSERT INTO `user` (user_id, firstName, lastName, email, phoneNumber) VALUES (?, ?, ?, ?, ?)";
        	PreparedStatement insertUserPs = conn.prepareStatement(insertUserSql);
            insertUserPs.setString(1, travelerId);
        	insertUserPs.setString(2, "Visitor");
        	insertUserPs.setString(3, "Guest");
        	insertUserPs.setString(4, "visitor-" + nationalId + "@gonature.local");
        	insertUserPs.setNull(5, Types.VARCHAR);
        	insertUserPs.executeUpdate();
            System.out.println("Inserted user row for visitor: " + travelerId);

            String insertTravelerSql = "INSERT INTO traveler (traveler_id, nationalId, guide, clubMember, user_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement insertTravelerPs = conn.prepareStatement(insertTravelerSql);
            System.out.println("Registering traveler details for national ID: " + nationalId);
            insertTravelerPs.setString(1, travelerId);
            insertTravelerPs.setInt(2, nationalIdNumber);
            insertTravelerPs.setBoolean(3, false);
            insertTravelerPs.setBoolean(4, false);
            insertTravelerPs.setString(5, travelerId); //user_id
            insertTravelerPs.executeUpdate();
            System.out.println("Inserted traveler row for visitor: " + travelerId);

            conn.commit();
            return new VisitorLoginResult(travelerId, nationalId, true);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private int getParkCurrentVisitors(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT currentVisitors FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("currentVisitors");
        return 0;
    }

    private Object[] getParkSettings(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT maxCapacity, gap, defaultStayTime FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }
        return new Object[]{
            getNullableInt(rs, "maxCapacity"),
            getNullableInt(rs, "gap"),
            getNullableInt(rs, "defaultStayTime")
        };
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private int getEffectiveAvailableSpots(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String parkSql = "SELECT maxCapacity, currentVisitors FROM park WHERE park_id = ?";
        PreparedStatement parkPs = conn.prepareStatement(parkSql);
        parkPs.setInt(1, parkId);
        ResultSet parkRs = parkPs.executeQuery();
        if (!parkRs.next()) return 0;

        int maxCapacity = parkRs.getInt("maxCapacity");
        int currentVisitors = parkRs.getInt("currentVisitors");

        String bookingSql = "SELECT COALESCE(SUM(numberOfVisitors), 0) AS bookedVisitors " +
            "FROM booking WHERE park_id = ? " +
            "AND status NOT IN (?, ?) " +
            "AND visitorTime BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 4 HOUR)";
        PreparedStatement bookingPs = conn.prepareStatement(bookingSql);
        bookingPs.setInt(1, parkId);
        bookingPs.setString(2, Booking.STATUS_CANCELLED);
        bookingPs.setString(3, Booking.STATUS_CHECKED_IN);
        ResultSet bookingRs = bookingPs.executeQuery();
        int bookedVisitors = 0;
        if (bookingRs.next()) bookedVisitors = bookingRs.getInt("bookedVisitors");

        int effectiveAvailable = maxCapacity - currentVisitors - bookedVisitors;
        return Math.max(0, effectiveAvailable);
    }

    private Booking getBookingById(String bookingId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM booking WHERE booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Utils.mapBooking(rs);
        return null;
    }


    private boolean checkInVisitor(Booking booking, int employeeParkId) throws SQLException {
        if (booking.getParkId() != employeeParkId) {
            throw new IllegalArgumentException("This booking belongs to another park.");
        }

        Connection conn = DBConnection.getStaticConnection();

        String updateBooking = "UPDATE booking SET status = ?, visitorsInside = numberOfVisitors, entryTime = NOW() " +
                               "WHERE booking_id = ? AND status = ? AND park_id = ?";
        PreparedStatement ps = conn.prepareStatement(updateBooking);
        ps.setString(1, Booking.STATUS_CHECKED_IN);
        ps.setString(2, booking.getBookingId());
        ps.setString(3, Booking.STATUS_CONFIRMED);
        ps.setInt(4, employeeParkId);
        int rows = ps.executeUpdate();

        if (rows == 0) return false;

        String updatePark = "UPDATE park SET currentVisitors = currentVisitors + ? WHERE park_id = ?";
        PreparedStatement parkPs = conn.prepareStatement(updatePark);
        parkPs.setInt(1, booking.getNumberOfVisitors());
        parkPs.setInt(2, booking.getParkId());
        parkPs.executeUpdate();

        return true;
    }


    private boolean checkOutVisitor(ExitRequest request) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String getSql = "SELECT visitorsInside, status FROM booking WHERE booking_id = ?";
        PreparedStatement getPs = conn.prepareStatement(getSql);
        getPs.setString(1, request.getBookingId());
        ResultSet rs = getPs.executeQuery();

        if (!rs.next()) {
            throw new IllegalArgumentException("Booking not found.");
        }

        int visitorsInside = rs.getInt("visitorsInside");
        String status = rs.getString("status");

        if (!Booking.STATUS_CHECKED_IN.equals(status)) {
            throw new IllegalArgumentException(
                "This booking is not checked in. Status: " + status);
        }

        if (request.getVisitorsLeaving() > visitorsInside) {
            throw new IllegalArgumentException(
                "Cannot exit " + request.getVisitorsLeaving() + " visitors. " +
                "Only " + visitorsInside + " visitor(s) from this booking are still inside."
            );
        }

        int remaining = visitorsInside - request.getVisitorsLeaving();

        String newStatus = (remaining == 0) ? Booking.STATUS_CHECKED_OUT : Booking.STATUS_CHECKED_IN;
        String updateBooking = "UPDATE booking SET visitorsInside = ?, status = ?, exitTime = CASE WHEN ? = 0 THEN NOW() ELSE exitTime END WHERE booking_id = ?";
        PreparedStatement updatePs = conn.prepareStatement(updateBooking);
        updatePs.setInt(1, remaining);
        updatePs.setString(2, newStatus);
        updatePs.setInt(3, remaining);
        updatePs.setString(4, request.getBookingId());
        updatePs.executeUpdate();

        String updatePark = "UPDATE park SET currentVisitors = GREATEST(0, currentVisitors - ?) WHERE park_id = ?";
        PreparedStatement parkPs = conn.prepareStatement(updatePark);
        parkPs.setInt(1, request.getVisitorsLeaving());
        parkPs.setInt(2, request.getParkId());
        parkPs.executeUpdate();

        Booking updatedBooking = getBookingById(request.getBookingId());
        if (updatedBooking != null && Booking.STATUS_CHECKED_OUT.equals(updatedBooking.getStatus())) {
            BookingLifecycleService.handleSpotFreed(conn, updatedBooking.getParkId(), updatedBooking.getVisitorTime());
        }

        return true;
    }


    private Booking processWalkIn(WalkInRequest request) throws SQLException {
        int available = getEffectiveAvailableSpots(request.getParkId());
        if (available < request.getNumberOfVisitors()) {
            throw new IllegalArgumentException("Not enough spots available. Available: " + available);
        }

        VisitorLoginResult traveler = loginOrRegisterVisitor(request.getNationalId());
        String travelerId = traveler.getTravelerId();

        Connection conn = DBConnection.getStaticConnection();

        // Calculate price (full price for walk-in, no discount)
        double price = calculatePrice(conn, request.getParkId(), request.getNumberOfVisitors(), travelerId, false);

        String bookingId = UUID.randomUUID().toString();
        String sql = "INSERT INTO booking (booking_id, traveler_id, park_id, numberOfVisitors, visitorTime, status, organizedBooking, price, visitorsInside, entryTime) " +
            "VALUES (?, ?, ?, ?, NOW(), ?, false, ?, ?, NOW())";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ps.setString(2, travelerId);
        ps.setInt(3, request.getParkId());
        ps.setInt(4, request.getNumberOfVisitors());
        ps.setString(5, Booking.STATUS_CHECKED_IN);
        ps.setDouble(6, price);
        ps.setInt(7, request.getNumberOfVisitors()); // visitorsInside = all visitors
        ps.executeUpdate();

        String updatePark = "UPDATE park SET currentVisitors = currentVisitors + ? WHERE park_id = ?";
        PreparedStatement parkPs = conn.prepareStatement(updatePark);
        parkPs.setInt(1, request.getNumberOfVisitors());
        parkPs.setInt(2, request.getParkId());
        parkPs.executeUpdate();

        return new Booking(bookingId, travelerId, request.getParkId(),
            request.getNumberOfVisitors(), java.time.LocalDateTime.now(),
            Booking.STATUS_CHECKED_IN, false, price);
    }

    private void sendPendingRequestsToManager(int parkId, ConnectionToClient client) {
        try {
            Connection conn = DBConnection.getStaticConnection();
            System.out.println("[DEBUG] sendPendingRequestsToManager STARTED for parkId: " + parkId);

            String paramSql = "SELECT * FROM managerRequests WHERE park_id = ? AND approved IS NULL";
            PreparedStatement paramPs = conn.prepareStatement(paramSql);
            paramPs.setInt(1, parkId);
            ResultSet paramRs = paramPs.executeQuery();
            int paramCount = 0;
            while (paramRs.next()) {
                paramCount++;
                ParkChangeRequest req = new ParkChangeRequest(
                    paramRs.getInt("park_id"),
                    ParkChangeRequest.ParameterType.valueOf(paramRs.getString("parameter_type")),
                    paramRs.getInt("new_value"),
                    getUsernameForEmployeeId(conn, paramRs.getInt("employee_id")),
                    paramRs.getInt("employee_id"),
                    paramRs.getInt("dep_manager_id"),
                    paramRs.getString("request_Id"),
                    paramRs.getString("requestTitle")
                );
                client.sendToClient(new Message("PARK_CHANGE_REQUEST_NOTIFICATION", req));
            }
            System.out.println("[DEBUG] Param requests found and sent: " + paramCount);

            String promoSql = "SELECT * FROM promotion_request WHERE park_id = ? AND approved IS NULL";
            PreparedStatement promoPs = conn.prepareStatement(promoSql);
            promoPs.setInt(1, parkId);
            ResultSet promoRs = promoPs.executeQuery();
            int promoCount = 0;
            while (promoRs.next()) {
                promoCount++;
                Timestamp endTs = promoRs.getTimestamp("endDate");
                int promotionId = promoRs.getInt("promotion_id");
                String actionType = promoRs.getString("action_type");
                String title = (actionType.equals("ADD") ? "Add promotion #" :
                                actionType.equals("UPDATE") ? "Update promotion #" : "Delete promotion #")
                                + promoRs.getInt("promo_code");

                PromotionRequest req = new PromotionRequest(
                    promoRs.getString("request_id"),
                    PromotionRequest.ActionType.valueOf(actionType),
                    promotionId,
                    promoRs.getInt("park_id"),
                    promoRs.getInt("promo_code"),
                    promoRs.getInt("percentage"),
                    endTs != null ? endTs.toLocalDateTime() : null,
                    promoRs.getString("description"),
                    getUsernameForEmployeeId(conn, promoRs.getInt("employee_id")),
                    promoRs.getInt("employee_id"),
                    title
                );
                client.sendToClient(new Message("PROMOTION_REQUEST_NOTIFICATION", req));
            }
            System.out.println("[DEBUG] Promo requests found and sent: " + promoCount);
            System.out.println("[DEBUG] sendPendingRequestsToManager FINISHED for parkId: " + parkId);

        } catch (Exception e) {
            System.out.println("[DEBUG] EXCEPTION in sendPendingRequestsToManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getUsernameForEmployeeId(Connection conn, int employeeId) throws SQLException {
        String sql = "SELECT username FROM employee WHERE employee_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, employeeId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("username");
        return "unknown";
    }

    private void handleParkChangeRequest(ParkChangeRequest request, ConnectionToClient client) throws Exception {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "INSERT INTO managerRequests (request_Id, employee_id, dep_manager_id, " +
                     "requestTitle, parameter_type, new_value, park_id, approved) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, request.getRequestId());
        ps.setInt(2, request.getEmployeeId());
        ps.setInt(3, request.getDepManagerId());
        ps.setString(4, request.getRequestTitle());
        ps.setString(5, request.getParameterType().name());
        ps.setInt(6, request.getNewValue());
        ps.setInt(7, request.getParkId());
        ps.executeUpdate();

        boolean depManagerOnline = false;
        for (Thread t : getClientConnections()) {
            if (!(t instanceof ConnectionToClient)) continue;
            ConnectionToClient c = (ConnectionToClient) t;
            Object role = c.getInfo("EMPLOYEE_ROLE");
            Object parkId = c.getInfo("EMPLOYEE_PARK_ID");
            if ("department_manager".equals(role) &&
                parkId != null && (int) parkId == request.getParkId()) {
                c.sendToClient(new Message("PARK_CHANGE_REQUEST_NOTIFICATION", request));
                depManagerOnline = true;
                break;
            }
        }

        if (depManagerOnline) {
            client.sendToClient(new Message("PARK_CHANGE_REQUEST_RESULT", true));
        } else {
            client.sendToClient(new Message("PARK_CHANGE_REQUEST_RESULT", false));
        }
    }

    private void handleParkChangeApproval(String requestId, boolean approved, ConnectionToClient client) throws Exception {
        Connection conn = DBConnection.getStaticConnection();

        String updateSql = "UPDATE managerRequests SET approved = ? WHERE request_Id = ? AND approved IS NULL";
        PreparedStatement updatePs = conn.prepareStatement(updateSql);
        updatePs.setInt(1, approved ? 1 : 0);
        updatePs.setString(2, requestId);
        int updatedRows = updatePs.executeUpdate();
        if (updatedRows == 0) {
            client.sendToClient(new Message("ERROR", "This request has already been processed."));
            return;
        }

        if (approved) {
            String getSql = "SELECT park_id, parameter_type, new_value FROM managerRequests WHERE request_Id = ?";
            PreparedStatement getPs = conn.prepareStatement(getSql);
            getPs.setString(1, requestId);
            ResultSet rs = getPs.executeQuery();
            if (rs.next()) {
                int parkId = rs.getInt("park_id");
                String paramType = rs.getString("parameter_type");
                int newValue = rs.getInt("new_value");

                String column;
                switch (paramType) {
                    case "MAX_CAPACITY":      column = "maxCapacity";    break;
                    case "GAP":               column = "gap";            break;
                    case "DEFAULT_STAY_TIME": column = "defaultStayTime"; break;
                    default: throw new IllegalArgumentException("Unknown parameter: " + paramType);
                }

                String applySql = "UPDATE park SET " + column + " = ? WHERE park_id = ?";
                PreparedStatement applyPs = conn.prepareStatement(applySql);
                applyPs.setInt(1, newValue);
                applyPs.setInt(2, parkId);
                applyPs.executeUpdate();
            }
        }

        String getRequesterSql = "SELECT e.username FROM managerRequests r " +
                                  "JOIN employee e ON r.employee_id = e.employee_id " +
                                  "WHERE r.request_Id = ?";
        PreparedStatement getRequesterPs = conn.prepareStatement(getRequesterSql);
        getRequesterPs.setString(1, requestId);
        ResultSet requesterRs = getRequesterPs.executeQuery();

        if (requesterRs.next()) {
            String requesterUsername = requesterRs.getString("username");
            System.out.println("[APPROVAL] Looking for park manager with username: " + requesterUsername);

            boolean found = false;
            for (Thread t : getClientConnections()) {
                if (!(t instanceof ConnectionToClient)) continue;
                ConnectionToClient c = (ConnectionToClient) t;
                Object username = c.getInfo("EMPLOYEE_USERNAME");
                System.out.println("[APPROVAL] Checking connection with username: " + username);
                if (username != null && username.equals(requesterUsername)) {
                    c.sendToClient(new Message("PARK_CHANGE_APPROVAL_RESULT",
                        new Object[]{requestId, approved}));
                    found = true;
                    System.out.println("[APPROVAL] Notification sent successfully!");
                    break;
                }
            }
            if (!found) {
                System.out.println("[APPROVAL] WARNING: Could not find connection for " + requesterUsername);
            }
        } else {
            System.out.println("[APPROVAL] WARNING: Could not find requester for request " + requestId);
        }
    }

    // -------------------------------------------------------------------------
    // Promotions
    // -------------------------------------------------------------------------

    private ArrayList<Promotion> getPromotions(int parkId) throws SQLException {
        ArrayList<Promotion> list = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM promotion WHERE park_id = ? ORDER BY promotion_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapPromotion(rs));
        }
        return list;
    }

    private Promotion mapPromotion(ResultSet rs) throws SQLException {
        Timestamp endTimestamp = rs.getTimestamp("endDate");
        return new Promotion(
            rs.getInt("promotion_id"),
            rs.getInt("park_id"),
            rs.getInt("promo_code"),
            rs.getInt("percentage"),
            endTimestamp != null ? endTimestamp.toLocalDateTime() : null,
            rs.getString("description")
        );
    }

    private void handlePromotionRequest(PromotionRequest request, ConnectionToClient client) throws Exception {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "INSERT INTO promotion_request (request_id, employee_id, action_type, " +
                     "promotion_id, park_id, promo_code, percentage, endDate, description, approved) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, request.getRequestId());
        ps.setInt(2, request.getEmployeeId());
        ps.setString(3, request.getActionType().name());
        if (request.getPromotionId() > 0) {
            ps.setInt(4, request.getPromotionId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.setInt(5, request.getParkId());
        ps.setInt(6, request.getPromoCode());
        ps.setInt(7, request.getPercentage());
        ps.setTimestamp(8, request.getEndDate() != null ? Timestamp.valueOf(request.getEndDate()) : null);
        ps.setString(9, request.getDescription());
        ps.executeUpdate();

        boolean depManagerOnline = false;
        for (Thread t : getClientConnections()) {
            if (!(t instanceof ConnectionToClient)) continue;
            ConnectionToClient c = (ConnectionToClient) t;
            Object role = c.getInfo("EMPLOYEE_ROLE");
            Object parkId = c.getInfo("EMPLOYEE_PARK_ID");
            if ("department_manager".equals(role) &&
                parkId != null && (int) parkId == request.getParkId()) {
                c.sendToClient(new Message("PROMOTION_REQUEST_NOTIFICATION", request));
                depManagerOnline = true;
                break;
            }
        }

        client.sendToClient(new Message("PROMOTION_REQUEST_RESULT", depManagerOnline));
    }

    private void handlePromotionApproval(String requestId, boolean approved, ConnectionToClient client) throws Exception {
        Connection conn = DBConnection.getStaticConnection();

        String updateSql = "UPDATE promotion_request SET approved = ? WHERE request_id = ?";
        PreparedStatement updatePs = conn.prepareStatement(updateSql);
        updatePs.setInt(1, approved ? 1 : 0);
        updatePs.setString(2, requestId);
        updatePs.executeUpdate();

        if (approved) {
            String getSql = "SELECT * FROM promotion_request WHERE request_id = ?";
            PreparedStatement getPs = conn.prepareStatement(getSql);
            getPs.setString(1, requestId);
            ResultSet rs = getPs.executeQuery();
            if (rs.next()) {
                String actionType = rs.getString("action_type");
                int parkId = rs.getInt("park_id");
                int promoCode = rs.getInt("promo_code");
                int percentage = rs.getInt("percentage");
                Timestamp endDate = rs.getTimestamp("endDate");
                String description = rs.getString("description");
                int promotionId = rs.getInt("promotion_id");

                switch (actionType) {
                    case "ADD": {
                        String insertSql = "INSERT INTO promotion (park_id, promo_code, percentage, endDate, description) " +
                                            "VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement insertPs = conn.prepareStatement(insertSql);
                        insertPs.setInt(1, parkId);
                        insertPs.setInt(2, promoCode);
                        insertPs.setInt(3, percentage);
                        insertPs.setTimestamp(4, endDate);
                        insertPs.setString(5, description);
                        insertPs.executeUpdate();
                        break;
                    }
                    case "UPDATE": {
                        String updatePromoSql = "UPDATE promotion SET promo_code = ?, percentage = ?, " +
                                                 "endDate = ?, description = ? WHERE promotion_id = ?";
                        PreparedStatement updatePromoPs = conn.prepareStatement(updatePromoSql);
                        updatePromoPs.setInt(1, promoCode);
                        updatePromoPs.setInt(2, percentage);
                        updatePromoPs.setTimestamp(3, endDate);
                        updatePromoPs.setString(4, description);
                        updatePromoPs.setInt(5, promotionId);
                        updatePromoPs.executeUpdate();
                        break;
                    }
                    case "DELETE": {
                        String deleteSql = "DELETE FROM promotion WHERE promotion_id = ?";
                        PreparedStatement deletePs = conn.prepareStatement(deleteSql);
                        deletePs.setInt(1, promotionId);
                        deletePs.executeUpdate();
                        break;
                    }
                }
            }
        }

        String getRequesterSql = "SELECT e.username FROM promotion_request r " +
                                  "JOIN employee e ON r.employee_id = e.employee_id " +
                                  "WHERE r.request_id = ?";
        PreparedStatement getRequesterPs = conn.prepareStatement(getRequesterSql);
        getRequesterPs.setString(1, requestId);
        ResultSet requesterRs = getRequesterPs.executeQuery();

        if (requesterRs.next()) {
            String requesterUsername = requesterRs.getString("username");
            for (Thread t : getClientConnections()) {
                if (!(t instanceof ConnectionToClient)) continue;
                ConnectionToClient c = (ConnectionToClient) t;
                Object username = c.getInfo("EMPLOYEE_USERNAME");
                if (username != null && username.equals(requesterUsername)) {
                    c.sendToClient(new Message("PROMOTION_APPROVAL_RESULT",
                        new Object[]{requestId, approved}));
                    break;
                }
            }
        }
    }
}
