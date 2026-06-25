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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import common.SubscriberRequest;

import common.ParkSubmittedReport;
import common.ParkSubmittedReportDetails;
import common.ParkReportRequest;
import common.ParkVisitorsReportResult;
import common.ParkUsageReportResult;
import common.ParkChangeRequest;
import common.ParkManagerActivityLogEntry;
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
import common.ParkVisitorsCount;
import common.CancellationsReportRequest;
import common.CancellationsReportResult;
import gui.ServerPortFrameController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class ParkServer extends AbstractServer {
    private static final java.time.LocalTime BOOKING_CLOSE_TIME = java.time.LocalTime.of(16, 0);

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
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
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
                "AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
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
            "CASE WHEN organizedBooking = 1 THEN 'Organized Group' ELSE 'Individual' END AS visitorType, " +
            "DATE_FORMAT(entryTime, '%Y-%m-%d %H:%i') AS entryTime, " +
            "CASE WHEN exitTime IS NOT NULL " +
            "     THEN TIMESTAMPDIFF(MINUTE, entryTime, exitTime) " +
            "     ELSE NULL END AS stayMinutes " +
            "FROM booking " +
            "WHERE park_id = ? " +
            "AND entryTime IS NOT NULL " +
            "AND DATE(entryTime) BETWEEN ? AND ? " +
            "ORDER BY organizedBooking, entryTime";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, req.getParkId());
        ps.setDate(2, java.sql.Date.valueOf(req.getFromDate()));
        ps.setDate(3, java.sql.Date.valueOf(req.getToDate()));

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            int stayRaw = rs.getInt("stayMinutes");
            Integer stayMinutes = rs.wasNull() ? null : stayRaw;
            results.add(new VisitsReportResult(
                rs.getString("visitorType"),
                rs.getString("entryTime"),
                stayMinutes
            ));
        }

        return results;
    }

    /**
     * Reads the live visitor count and capacity of every park.
     * Used by the department manager overview screen so the manager can see,
     * at a glance, how busy each park in the region is right now.
     */
    private ArrayList<ParkVisitorsCount> getAllParksVisitors(int managerEmployeeId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        return Utils.getAllParksVisitorsForDepartment(conn, managerEmployeeId);
    }

    private void pushVisitorCountsToDepartmentManager(int parkId) {
        try {
            Connection conn = DBConnection.getStaticConnection();
            String sql = "SELECT department_manager_id FROM park WHERE park_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parkId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;
            int managerId = rs.getInt("department_manager_id");
            if (rs.wasNull() || managerId == 0) return;

            ArrayList<ParkVisitorsCount> counts = getAllParksVisitors(managerId);
            for (Thread t : getClientConnections()) {
                if (!(t instanceof ConnectionToClient)) continue;
                ConnectionToClient c = (ConnectionToClient) t;
                Object storedId = c.getInfo("EMPLOYEE_ID");
                if (storedId != null && (int) storedId == managerId) {
                    c.sendToClient(new Message("ALL_PARKS_VISITORS_RESULT", counts));
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[PUSH] Failed to push visitor counts to department manager: " + e.getMessage());
        }
    }

    /**
     * Pushes real-time park state updates to all connected clients monitoring the specified park.
     * Retrieves current visitor count, available spots, and today's bookings, then broadcasts
     * this information to all ConnectionToClient instances associated with the given park ID (Park Employees).
     * Also notifies department managers of visitor count changes.
     *
     * @param parkId the ID of the park for which to push live state updates
     */
    private void pushLiveParkState(int parkId) {
        try {
            Connection conn = DBConnection.getStaticConnection();
            int currentVisitors = Utils.getParkCurrentVisitors(conn, parkId);
            int effectiveSpots = Utils.getEffectiveAvailableSpots(conn, parkId);
            ArrayList<Booking> todayBookings = getTodayBookings(parkId);

            for (Thread t : getClientConnections()) {
                if (!(t instanceof ConnectionToClient)) continue;
                ConnectionToClient c = (ConnectionToClient) t;
                Object storedParkId = c.getInfo("EMPLOYEE_PARK_ID");
                if (storedParkId != null && (int) storedParkId == parkId) {
                    c.sendToClient(new Message("PARK_VISITORS_RESULT", currentVisitors));
                    c.sendToClient(new Message("EFFECTIVE_AVAILABLE_SPOTS_RESULT", effectiveSpots));
                    c.sendToClient(new Message("TODAY_BOOKINGS_RESULT", todayBookings));
                }
            }

            pushVisitorCountsToDepartmentManager(parkId);
        } catch (Exception e) {
            System.out.println("[PUSH] Failed to push live park state: " + e.getMessage());
        }
    }

    /**
     * Builds the cancellations report for every park in the given date range.
     * For each park it counts:
     *   - CANCELLED bookings (the traveler cancelled),
     *   - SYSTEM_CANCEL bookings (expired without the traveler confirming),
     *   - the total number of bookings (to work out the cancellation rate).
     * A LEFT JOIN is used so parks with no bookings still appear with zeros.
     */
    private ArrayList<CancellationsReportResult> getCancellationsReport(CancellationsReportRequest req) throws SQLException {
        ArrayList<CancellationsReportResult> results = new ArrayList<>();

        Connection conn = DBConnection.getStaticConnection();
        String sql =
            "SELECT p.name AS parkName, " +
            "SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledCount, " +
            "SUM(CASE WHEN b.status = 'SYSTEM_CANCEL' THEN 1 ELSE 0 END) AS expiredCount, " +
            "COUNT(b.booking_id) AS totalBookings " +
            "FROM park p " +
            "LEFT JOIN booking b ON b.park_id = p.park_id " +
            "AND DATE(b.visitorTime) BETWEEN ? AND ? " +
            "GROUP BY p.park_id, p.name " +
            "ORDER BY p.park_id";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDate(1, java.sql.Date.valueOf(req.getFromDate()));
        ps.setDate(2, java.sql.Date.valueOf(req.getToDate()));

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            results.add(new CancellationsReportResult(
                rs.getString("parkName"),
                rs.getInt("cancelledCount"),
                rs.getInt("expiredCount"),
                rs.getInt("totalBookings")
            ));
        }

        return results;
    }

    private ArrayList<ParkSubmittedReport> getSubmittedReports(int parkId) throws SQLException {

        ArrayList<ParkSubmittedReport> reports = new ArrayList<>();

        String sql =
                "SELECT r.report_id, r.park_id, COALESCE(p.name, 'Unknown Park') AS park_name, " +
                "r.reportTitle, r.content, r.employee_id, r.report_type, " +
                "r.from_date, r.to_date, r.submitted_at " +
                "FROM report r " +
                "LEFT JOIN park p ON r.park_id = p.park_id " +
                "WHERE r.park_id = ? " +
                "ORDER BY r.submitted_at DESC, r.report_id DESC";

        Connection conn = DBConnection.getStaticConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            reports.add(new ParkSubmittedReport(
                    rs.getInt("report_id"),
                    rs.getInt("park_id"),
                    rs.getString("park_name"),
                    rs.getString("reportTitle"),
                    rs.getString("content"),
                    rs.getInt("employee_id"),
                    rs.getString("report_type"),
                    toLocalDate(rs.getDate("from_date")),
                    toLocalDate(rs.getDate("to_date")),
                    toLocalDateTime(rs.getTimestamp("submitted_at"))
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
                        // Live-update park employees so a newly confirmed booking shows up immediately.
                        pushLiveParkState(bookingAvailabilityResult.booking.getParkId());
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
                    // Capture the original park before the edit in case the booking moves to another park.
                    Booking originalBooking = getBookingById(bookingToUpdate.getBookingId());
                    Booking updatedBookingResult = updateBooking(bookingToUpdate);
                    client.sendToClient(new Message("UPDATE_BOOKING_RESULT", updatedBookingResult));
                    if (updatedBookingResult != null) {
                        // Live-update park employees: the name, visitors, time or even the park may have changed.
                        pushLiveParkState(updatedBookingResult.getParkId());
                        if (originalBooking != null
                                && originalBooking.getParkId() != updatedBookingResult.getParkId()) {
                            pushLiveParkState(originalBooking.getParkId());
                        }
                    }
                    break;

                case "CANCEL_BOOKING":
                    Booking bookingToCancel = (Booking) message.getData();
                    boolean cancelledOk = cancelBooking(bookingToCancel);
                    client.sendToClient(new Message("CANCEL_BOOKING_RESULT", cancelledOk));
                    if (cancelledOk) pushLiveParkState(bookingToCancel.getParkId());
                    break;

                case "CONFIRM_BOOKING":
                    String[] confirmationData = (String[]) message.getData();
                    boolean confirmed = BookingLifecycleService.confirmBooking(Integer.parseInt(confirmationData[0]), confirmationData[1]);
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
                        client.setInfo("EMPLOYEE_ID", employee.getEmployeeId());
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
                 // Department manager: live visitor counts for every park in the region.
                 case "GET_ALL_PARKS_VISITORS":
                    int managerEmployeeId = (int) message.getData();
                     ArrayList<ParkVisitorsCount> allParksVisitors = getAllParksVisitors(managerEmployeeId);
                     client.sendToClient(new Message("ALL_PARKS_VISITORS_RESULT", allParksVisitors));
                     break;

                 case "GET_EFFECTIVE_AVAILABLE_SPOTS":
                     int parkIdForSpots = (int) message.getData();
                     int effectiveSpots = getEffectiveAvailableSpots(parkIdForSpots);
                     client.sendToClient(new Message("EFFECTIVE_AVAILABLE_SPOTS_RESULT", effectiveSpots));
                     break;

                 case "GET_BOOKING_BY_ID":
                     String bookingIdToFind = (String) message.getData();
                     Booking foundBooking = getBookingById(Integer.parseInt(bookingIdToFind));
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
                     if (checkInSuccess) pushLiveParkState(employeeParkId);
                     break;

                 case "CHECK_OUT_VISITOR":
                     ExitRequest exitRequest = (ExitRequest) message.getData();
                     boolean checkOutSuccess = checkOutVisitor(exitRequest);
                     client.sendToClient(new Message("CHECK_OUT_RESULT", checkOutSuccess));
                     if (checkOutSuccess) pushLiveParkState(exitRequest.getParkId());
                     break;

                 case "SET_VISITORS_INSIDE":
                     Booking visitorsUpdateBooking = (Booking) message.getData();
                     Integer parkIdForUpdate = (Integer) client.getInfo("EMPLOYEE_PARK_ID");
                     if (parkIdForUpdate == null) {
                         throw new IllegalArgumentException("Employee park is missing. Please log in again.");
                     }
                     boolean updateInsideSuccess = setVisitorsInside(visitorsUpdateBooking, parkIdForUpdate);
                     client.sendToClient(new Message("SET_VISITORS_INSIDE_RESULT", updateInsideSuccess));
                     if (updateInsideSuccess) pushLiveParkState(parkIdForUpdate);
                     break;

                 case "WALK_IN_VISITOR":
                     WalkInRequest walkInRequest = (WalkInRequest) message.getData();
                     Booking walkInBooking = processWalkIn(walkInRequest);
                     client.sendToClient(new Message("WALK_IN_RESULT", walkInBooking));
                     pushLiveParkState(walkInRequest.getParkId());
                     break;

                 case "CANCEL_WALK_IN":
                     Booking cancelWalkInBooking = (Booking) message.getData();
                     Integer parkIdForCancel = (Integer) client.getInfo("EMPLOYEE_PARK_ID");
                     if (parkIdForCancel == null) {
                         throw new IllegalArgumentException("Employee park is missing. Please log in again.");
                     }
                     boolean cancelWalkInSuccess = cancelWalkIn(cancelWalkInBooking, parkIdForCancel);
                     client.sendToClient(new Message("CANCEL_WALK_IN_RESULT", cancelWalkInSuccess));
                     if (cancelWalkInSuccess) pushLiveParkState(parkIdForCancel);
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

                 case "GET_PARK_MANAGER_ACTIVITY_LOG":
                     int activityLogEmployeeId = (int) message.getData();
                     client.sendToClient(new Message("PARK_MANAGER_ACTIVITY_LOG_RESULT",
                         getParkManagerActivityLog(activityLogEmployeeId)));
                     break;

                 case "PARK_VISITORS_REPORT":
                	    ParkReportRequest visitorsRequest = (ParkReportRequest) message.getData();

                	    ParkVisitorsReportResult visitorsResult =
                	            getParkVisitorsReport(
                	                    visitorsRequest.getParkId(),
                	                    visitorsRequest.getFromDate(),
                	                    visitorsRequest.getToDate());

                	    client.sendToClient(
                	            new Message("PARK_VISITORS_REPORT_RESULT", visitorsResult));
                	    break;

                 case "SUBMIT_PARK_VISITORS_REPORT":
                     ParkReportRequest submitVisitorsRequest = (ParkReportRequest) message.getData();
                     ParkVisitorsReportResult submitVisitorsResult =
                             getParkVisitorsReport(
                                     submitVisitorsRequest.getParkId(),
                                     submitVisitorsRequest.getFromDate(),
                                     submitVisitorsRequest.getToDate());

                     if (submitVisitorsResult.getTotalVisitors() <= 0) {
                         client.sendToClient(new Message("PARK_VISITORS_REPORT_SUBMIT_RESULT", false));
                         break;
                     }

                     saveParkVisitorsReport(
                             submitVisitorsRequest.getParkId(),
                             submitVisitorsRequest.getEmployeeId(),
                             submitVisitorsRequest.getFromDate(),
                             submitVisitorsRequest.getToDate(),
                             submitVisitorsResult
                     );
                     client.sendToClient(new Message("PARK_VISITORS_REPORT_SUBMIT_RESULT", true));
                     break;

                	case "PARK_USAGE_REPORT":
                	    ParkReportRequest usageRequest = (ParkReportRequest) message.getData();

                	    ArrayList<ParkUsageReportResult> usageResults =
                	            getParkUsageReport(
                	                    usageRequest.getParkId(),
                	                    usageRequest.getFromDate(),
                	                    usageRequest.getToDate());

                	    client.sendToClient(
                	            new Message("PARK_USAGE_REPORT_RESULT", usageResults));
                	    break;

                 case "SUBMIT_PARK_USAGE_REPORT":
                     ParkReportRequest submitUsageRequest = (ParkReportRequest) message.getData();
                     ArrayList<ParkUsageReportResult> submitUsageResults =
                             getParkUsageReport(
                                     submitUsageRequest.getParkId(),
                                     submitUsageRequest.getFromDate(),
                                     submitUsageRequest.getToDate());

                     if (submitUsageResults == null || submitUsageResults.isEmpty()) {
                         client.sendToClient(new Message("PARK_USAGE_REPORT_SUBMIT_RESULT", false));
                         break;
                     }

                     saveParkUsageReport(
                             submitUsageRequest.getParkId(),
                             submitUsageRequest.getEmployeeId(),
                             submitUsageRequest.getFromDate(),
                             submitUsageRequest.getToDate(),
                             submitUsageResults
                     );
                     client.sendToClient(new Message("PARK_USAGE_REPORT_SUBMIT_RESULT", true));
                     break;

                 case "GET_VISITS_REPORT":
                     VisitsReportRequest visitsReq = (VisitsReportRequest) message.getData();
                     ArrayList<VisitsReportResult> visitsResult = getVisitsReport(visitsReq);
                     client.sendToClient(new Message("VISITS_REPORT_RESULT", visitsResult));
                     break;

                 // Department manager: cancelled vs expired orders for every park.
                 case "GET_CANCELLATIONS_REPORT":
                     CancellationsReportRequest cancelReq = (CancellationsReportRequest) message.getData();
                     ArrayList<CancellationsReportResult> cancelResult = getCancellationsReport(cancelReq);
                     client.sendToClient(new Message("CANCELLATIONS_REPORT_RESULT", cancelResult));
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

                 case "GET_SUBMITTED_REPORT_DETAILS":
                     int reportId = (int) message.getData();
                     ParkSubmittedReportDetails details = getSubmittedReportDetails(reportId);
                     if (details == null) {
                         client.sendToClient(new Message("ERROR", "Report details not found."));
                     } else {
                         client.sendToClient(new Message("SUBMITTED_REPORT_DETAILS_RESULT", details));
                     }
                     break;

                 case "GET_TODAY_BOOKINGS":
                	    int parkIdForToday = (int) message.getData();
                	    ArrayList<Booking> todayBookings = getTodayBookings(parkIdForToday);
                	    client.sendToClient(new Message("TODAY_BOOKINGS_RESULT", todayBookings));
                	    break;

                 case "GET_ALL_CHECKED_IN_BOOKINGS":
                	    int parkIdForCheckedIn = (int) message.getData();
                	    ArrayList<Booking> checkedInNow = getAllCheckedInBookings(parkIdForCheckedIn);
                	    client.sendToClient(new Message("ALL_CHECKED_IN_BOOKINGS_RESULT", checkedInNow));
                	    break;

                 case "REGISTER_SUBSCRIBER":
                	    SubscriberRequest subReq = (SubscriberRequest) message.getData();
                	    Object[] subResult = registerSubscriber(subReq);
                	    client.sendToClient(new Message("REGISTER_SUBSCRIBER_RESULT", subResult));
                	    break;

                 case "GET_TRAVELER_STATUS":
                     String statusNationalId = (String) message.getData();
                     Object[] travelerStatus = getTravelerStatus(statusNationalId);
                     client.sendToClient(new Message("TRAVELER_STATUS_RESULT", travelerStatus));
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

            if (reportAlreadyExists(conn, parkId, "VISITORS", fromDate, toDate)) return;

            String sql = "INSERT INTO report (park_id, reportTitle, content, employee_id, report_type, from_date, to_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parkId);
            ps.setString(2, reportTitle);
            ps.setString(3, content);
            ps.setInt(4, employeeId);
            ps.setString(5, "VISITORS");
            ps.setDate(6, java.sql.Date.valueOf(fromDate));
            ps.setDate(7, java.sql.Date.valueOf(toDate));

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
                       .append(" (").append(String.format(java.util.Locale.US, "%.1f", r.getUsagePercent())).append("%)\n");
            }

            if (reportAlreadyExists(conn, parkId, "USAGE", fromDate, toDate)) return;

            String sql = "INSERT INTO report (park_id, reportTitle, content, employee_id, report_type, from_date, to_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parkId);
            ps.setString(2, reportTitle);
            ps.setString(3, content.toString());
            ps.setInt(4, employeeId);
            ps.setString(5, "USAGE");
            ps.setDate(6, java.sql.Date.valueOf(fromDate));
            ps.setDate(7, java.sql.Date.valueOf(toDate));

            ps.executeUpdate();
    }

    private ArrayList<Booking> getAllCheckedInBookings(int parkId) throws SQLException {
        ArrayList<Booking> list = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM booking WHERE park_id = ? AND status = 'CHECKED_IN' ORDER BY entryTime ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(Utils.mapBooking(rs));
        }
        return list;
    }
    
    private boolean reportAlreadyExists(Connection conn, int parkId, String reportType,
            java.time.LocalDate fromDate, java.time.LocalDate toDate) throws SQLException {
        String sql = "SELECT report_id FROM report WHERE park_id = ? AND report_type = ? " +
                     "AND from_date = ? AND to_date = ? LIMIT 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setString(2, reportType);
        ps.setDate(3, java.sql.Date.valueOf(fromDate));
        ps.setDate(4, java.sql.Date.valueOf(toDate));
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private ParkSubmittedReportDetails getSubmittedReportDetails(int reportId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT r.report_id, r.park_id, COALESCE(p.name, 'Unknown Park') AS park_name, " +
                     "r.reportTitle, r.content, r.employee_id, r.report_type, " +
                     "r.from_date, r.to_date, r.submitted_at " +
                     "FROM report r " +
                     "LEFT JOIN park p ON r.park_id = p.park_id " +
                     "WHERE r.report_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, reportId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;

        ParkSubmittedReport report = new ParkSubmittedReport(
            rs.getInt("report_id"),
            rs.getInt("park_id"),
            rs.getString("park_name"),
            rs.getString("reportTitle"),
            rs.getString("content"),
            rs.getInt("employee_id"),
            inferReportType(rs.getString("report_type"), rs.getString("reportTitle"), rs.getString("content")),
            resolveReportFromDate(rs.getDate("from_date"), rs.getString("content")),
            resolveReportToDate(rs.getDate("to_date"), rs.getString("content")),
            toLocalDateTime(rs.getTimestamp("submitted_at"))
        );

        if ("VISITORS".equals(report.getReportType())) {
            ParkVisitorsReportResult result = parseVisitorsReportResult(report.getContent());
            if (result == null && report.getFromDate() != null && report.getToDate() != null) {
                result = getParkVisitorsReport(report.getParkId(), report.getFromDate(), report.getToDate());
            }
            return new ParkSubmittedReportDetails(
                report,
                result,
                null
            );
        }

        if ("USAGE".equals(report.getReportType())) {
            ArrayList<ParkUsageReportResult> results = parseUsageReportResults(report.getContent());
            if (results.isEmpty() && report.getFromDate() != null && report.getToDate() != null) {
                results = getParkUsageReport(report.getParkId(), report.getFromDate(), report.getToDate());
            }
            return new ParkSubmittedReportDetails(
                report,
                null,
                results
            );
        }

        return new ParkSubmittedReportDetails(report, null, null);
    }

    private String inferReportType(String reportType, String reportTitle, String content) {
        if (reportType != null && !reportType.trim().isEmpty()) {
            return reportType.trim().toUpperCase();
        }

        String text = ((reportTitle == null ? "" : reportTitle) + "\n" +
                       (content == null ? "" : content)).toUpperCase();
        if (text.contains("USAGE REPORT")) return "USAGE";
        if (text.contains("VISITORS REPORT") || text.contains("VISITORS")) return "VISITORS";
        return null;
    }

    private java.time.LocalDate resolveReportFromDate(java.sql.Date storedDate, String content) {
        if (storedDate != null) return storedDate.toLocalDate();
        java.time.LocalDate[] period = parseReportPeriod(content);
        return period[0];
    }

    private java.time.LocalDate resolveReportToDate(java.sql.Date storedDate, String content) {
        if (storedDate != null) return storedDate.toLocalDate();
        java.time.LocalDate[] period = parseReportPeriod(content);
        return period[1];
    }

    private java.time.LocalDate[] parseReportPeriod(String content) {
        java.time.LocalDate[] period = new java.time.LocalDate[] { null, null };
        if (content == null) return period;

        Pattern pattern = Pattern.compile("Period:\\s*(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            period[0] = java.time.LocalDate.parse(matcher.group(1));
            period[1] = java.time.LocalDate.parse(matcher.group(2));
        }
        return period;
    }

    private ParkVisitorsReportResult parseVisitorsReportResult(String content) {
        if (content == null) return null;
        int individual = parseIntAfterLabel(content, "Individual Visitors:");
        int organized = parseIntAfterLabel(content, "Organized Groups:");
        return new ParkVisitorsReportResult(individual, organized);
    }

    private int parseIntAfterLabel(String content, String label) {
        int index = content.indexOf(label);
        if (index < 0) return 0;
        int start = index + label.length();
        while (start < content.length() && Character.isWhitespace(content.charAt(start))) start++;
        int end = start;
        while (end < content.length() && Character.isDigit(content.charAt(end))) end++;
        if (end == start) return 0;
        return Integer.parseInt(content.substring(start, end));
    }

    private ArrayList<ParkUsageReportResult> parseUsageReportResults(String content) {
        ArrayList<ParkUsageReportResult> results = new ArrayList<>();
        if (content == null) return results;

        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}):\\s+(\\d+)/(\\d+)\\s+\\(([-\\d.]+)%\\)$");
        String[] lines = content.split("\\R");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.matches()) {
                results.add(new ParkUsageReportResult(
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Double.parseDouble(matcher.group(4))
                ));
            }
        }

        return results;
    }

    private ArrayList<Booking> getTodayBookings(int parkId) throws SQLException {
        ArrayList<Booking> list = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();

        // Returns two sets in one query:
        // - Today's CONFIRMED bookings (shown in the Pending list, awaiting check-in)
        // - ALL CHECKED_IN bookings for this park regardless of date
        //   (people physically inside, including anyone who checked in on a previous day)
        String sql = "SELECT * FROM booking " +
                 "WHERE park_id = ? " +
                 "AND (" +
                 "  (DATE(visitorTime) = CURDATE() " +
                 "   AND status = 'CONFIRMED' " +
                 "   AND visitorTime BETWEEN DATE_SUB(NOW(), INTERVAL 30 MINUTE) AND DATE_ADD(NOW(), INTERVAL 30 MINUTE)) " +
                 "  OR status = 'CHECKED_IN'" +
                 ") " +
                 "ORDER BY status DESC, visitorTime ASC";
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

            int bookingId = 1000000 + new java.util.Random().nextInt(9000000);
            int capacity = Utils.getParkEffectiveCapacity(conn, booking.getParkId());
            int confirmedVisitors = getConfirmedVisitorsForSlot(conn, booking.getParkId(), booking.getVisitorTime());
            boolean isFull = confirmedVisitors + booking.getNumberOfVisitors() > capacity;
            String status = isFull
                ? Booking.STATUS_WAITING_LIST
                : Booking.STATUS_CONFIRMED;

            if (isFull && !allowWaitlistCreation) {
                conn.rollback();
                return new BookingAvailabilityResult(true, new Booking(0, booking.getTravelerId(), booking.getTravelerName(),
                    booking.getTravelerEmail(), booking.getTravelerPhoneNumber(), booking.getParkId(), booking.getNumberOfVisitors(),
                    booking.getVisitorTime(), Booking.STATUS_WAITING_LIST, false, 0));
            }
            String travelerID = booking.getTravelerId();

            double price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors(), travelerID, true);

            String sql = "INSERT INTO booking (booking_id, traveler_id, travelerName, travelerEmail, travelerPhoneNumber, "
                + "park_id, numberOfVisitors, visitorTime, status, organizedBooking, price) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bookingId);
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

    // Check the capacity - how many confirmed bookings are there.
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

    private void addWaitingListEntry(Connection conn, String waitingListId, int bookingId) throws SQLException {
        String sql = "INSERT INTO WaitingListEntry (id, waitingList_id, booking_id, registered_at, status, updated_at) "
            + "VALUES (?, ?, ?, NOW(), ?, NOW())";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, UUID.randomUUID().toString());
        ps.setString(2, waitingListId);
        ps.setInt(3, bookingId);
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
        ps.setInt(2, booking.getBookingId());
        return ps.executeUpdate() > 0;
    }

    private Booking updateBooking(Booking booking) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            Booking existing = Utils.getBookingByIdForUpdate(conn, booking.getBookingId());
            if (existing == null || !booking.getTravelerId().equals(existing.getTravelerId())) {
                conn.rollback();
                return null;
            }

            // Capacity check: confirm the new slot can accommodate the requested visitors.
            boolean slotChanged = existing.getParkId() != booking.getParkId()
                || !existing.getVisitorTime().equals(booking.getVisitorTime());
            int confirmedInNewSlot = getConfirmedVisitorsForSlot(conn, booking.getParkId(), booking.getVisitorTime());
            
            // If same slot and existing was confirmed, those visitors will be freed when we reset to PENDING.
            if (!slotChanged && Booking.STATUS_CONFIRMED.equals(existing.getStatus())) {
                confirmedInNewSlot -= existing.getNumberOfVisitors();
            }
            int capacity = Utils.getParkEffectiveCapacity(conn, booking.getParkId());

            if (booking.getNumberOfVisitors() > capacity - confirmedInNewSlot) {
                conn.rollback();
                if (!slotChanged) {
                    throw new IllegalArgumentException(
                        "The park is full for your current time slot. "
                        + "To book with more visitors, please cancel this booking and create a new one to join the waiting list.");
                } else {
                    throw new IllegalArgumentException(
                        "The selected time slot is full for the number of visitors you requested. "
                        + "Please choose a different time, or cancel this booking and create a new one to join the waiting list.");
                }
            }

            // Cancel the old waiting list entry if the booking was waiting.
            if (Booking.STATUS_WAITING_LIST.equals(existing.getStatus())) {
                Utils.updateWaitingListEntryByBooking(conn, booking.getBookingId(), "WAITING", "CANCELLED");
            }

            double price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors(), booking.getTravelerId(), true);

            // Slot unchanged → keep existing status; slot changed → re-confirm since capacity was already verified.
            String newStatus = slotChanged ? Booking.STATUS_CONFIRMED : existing.getStatus();

            String sql = "UPDATE booking SET travelerName = ?, travelerEmail = ?, travelerPhoneNumber = ?, "
                + "park_id = ?, numberOfVisitors = ?, visitorTime = ?, status = ?, price = ?, paid = 0 "
                + "WHERE booking_id = ? AND traveler_id = ? "
                + "AND status NOT IN (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, booking.getTravelerName().trim());
            ps.setString(2, booking.getTravelerEmail().trim());
            ps.setString(3, booking.getTravelerPhoneNumber().trim());
            ps.setInt(4, booking.getParkId());
            ps.setInt(5, booking.getNumberOfVisitors());
            ps.setTimestamp(6, Timestamp.valueOf(booking.getVisitorTime()));
            ps.setString(7, newStatus);
            ps.setDouble(8, price);
            ps.setInt(9, booking.getBookingId());
            ps.setString(10, booking.getTravelerId());
            ps.setString(11, Booking.STATUS_CANCELLED);
            ps.setString(12, Booking.STATUS_CHECKED_IN);
            ps.setString(13, Booking.STATUS_CHECKED_OUT);

            boolean updated = ps.executeUpdate() > 0;

            if (!updated) return null;

            if (slotChanged && Booking.STATUS_CONFIRMED.equals(existing.getStatus())) {
                BookingLifecycleService.handleSpotFreed(conn, existing.getParkId(), existing.getVisitorTime());
            }

            conn.commit();

            return new Booking(booking.getBookingId(), booking.getTravelerId(),
                booking.getTravelerName(), booking.getTravelerEmail(), booking.getTravelerPhoneNumber(),
                booking.getParkId(), booking.getNumberOfVisitors(), booking.getVisitorTime(),
                newStatus, false, price);
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private synchronized boolean cancelBooking(Booking booking) throws SQLException {
        if (booking == null || booking.getTravelerId() == null) {
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
            ps.setInt(2, booking.getBookingId());
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
        LocalDateTime now = LocalDateTime.now();
        if (booking.getVisitorTime() != null
            && booking.getVisitorTime().toLocalDate().isEqual(now.toLocalDate())
            && !now.toLocalTime().isBefore(BOOKING_CLOSE_TIME)) {
            throw new IllegalArgumentException("Booking is no longer available for today because the park is already closed.");
        }
        if (booking.getVisitorTime() == null || !booking.getVisitorTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time must be in the future.");
        }
        if (booking.getVisitorTime().toLocalTime().isAfter(BOOKING_CLOSE_TIME)) {
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

        double parkPrice = rs.getDouble("pricePerPerson");
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
            parks.add(new ParkOption(rs.getInt("park_id"), rs.getString("name"), rs.getDouble("pricePerPerson")));
        }

        return parks;
    }

    private Map<Integer, Double> getParkPrices() throws SQLException {
        Map<Integer, Double> prices = new HashMap<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT park_id, pricePerPerson FROM park ORDER BY park_id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            prices.put(rs.getInt("park_id"), rs.getDouble("pricePerPerson"));
        }

        return prices;
    }

    private TravelerProfile getTravelerProfile(String travelerId) throws SQLException {
        if (travelerId == null || travelerId.isBlank()) {
            throw new IllegalArgumentException("Traveler is missing.");
        }

        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT u.firstName, u.lastName, u.email, u.phoneNumber, t.clubMember "
            + "FROM traveler t JOIN `user` u ON u.user_id = t.user_id "
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
        String sql = "UPDATE `user` SET firstName = ?, lastName = ?, email = ?, phoneNumber = ? WHERE user_id = (SELECT user_id FROM traveler WHERE traveler_id = ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, firstName);
        ps.setString(2, lastName);
        ps.setString(3, email);
        ps.setString(4, phoneNumber);
        ps.setString(5, profile.getTravelerId());
        try {
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Email already exists in the system.");
        }
    }

    private VisitorLoginResult loginOrRegisterVisitor(String nationalId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String selectSql = "SELECT t.traveler_id, t.guide, t.clubMember "
            + "FROM traveler t "
            + "INNER JOIN `user` u ON t.user_id = u.user_id "
            + "WHERE u.nationalId = ?";
        PreparedStatement selectPs = conn.prepareStatement(selectSql);
        selectPs.setString(1, nationalId);
        ResultSet rs = selectPs.executeQuery();
        if (rs.next()) {
            boolean isGuide = rs.getBoolean("guide");
            boolean isClubMember = rs.getBoolean("clubMember");
            return new VisitorLoginResult(rs.getString("traveler_id"), nationalId, false, isGuide, isClubMember);
        }

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            String insertUserSql = "INSERT INTO `user` (user_id, firstName, lastName, email, phoneNumber, nationalId) VALUES (?, ?, ?, ?, ?, ?)";
            String userId = generateUniqueId(conn, "`user`", "user_id");
        	PreparedStatement insertUserPs = conn.prepareStatement(insertUserSql);
            insertUserPs.setString(1, userId);
        	insertUserPs.setString(2, "Visitor");
        	insertUserPs.setString(3, "Guest");
        	insertUserPs.setNull(4, Types.VARCHAR);
        	insertUserPs.setNull(5, Types.VARCHAR);
            insertUserPs.setString(6, nationalId);
        	insertUserPs.executeUpdate();

            String insertTravelerSql = "INSERT INTO traveler (traveler_id, guide, clubMember, user_id) VALUES (?, ?, ?, ?)";
            PreparedStatement insertTravelerPs = conn.prepareStatement(insertTravelerSql);

            String travelerId = generateUniqueId(conn, "traveler", "traveler_id");
            insertTravelerPs.setString(1, travelerId);
            insertTravelerPs.setBoolean(2, false);
            insertTravelerPs.setBoolean(3, false);
            insertTravelerPs.setString(4, userId);
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

    // Looks a person up by their national ID (which lives on the `user` table).
    // Employees take priority over travelers, so the same national ID is never
    // offered for club-member / guide registration when it belongs to staff.
    // Returns one of:
    //   Employee  -> Object[]{ "EMPLOYEE", String role, Integer salary, Integer parkId, String parkName,
    //                          String firstName, String lastName, String email, String phoneNumber }
    //   Traveler  -> Object[]{ "TRAVELER", Boolean isGuide, Boolean isClubMember, Integer familyMembers,
    //                          String firstName, String lastName, String email, String phoneNumber }
    //   not found -> null
    private Object[] getTravelerStatus(String nationalId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        int nationalIdNumber;
        try {
            nationalIdNumber = Integer.parseInt(nationalId);
        } catch (NumberFormatException e) {
            return null;
        }

        // 1) Employees first — if this national ID belongs to staff, return employee info.
        String empSql = "SELECT e.role, e.salary, e.park_id, p.name AS parkName, "
                      + "u.firstName, u.lastName, u.email, u.phoneNumber "
                      + "FROM employee e "
                      + "INNER JOIN `user` u ON e.user_id = u.user_id "
                      + "LEFT JOIN park p ON e.park_id = p.park_id "
                      + "WHERE u.nationalId = ?";
        try (PreparedStatement ps = conn.prepareStatement(empSql)) {
            ps.setInt(1, nationalIdNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int parkId = rs.getInt("park_id");
                Integer parkIdOrNull = rs.wasNull() ? null : parkId;
                return new Object[]{
                    "EMPLOYEE",
                    rs.getString("role"),
                    rs.getInt("salary"),
                    parkIdOrNull,
                    rs.getString("parkName"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("email"),
                    rs.getString("phoneNumber")
                };
            }
        }

        // 2) Otherwise fall back to the traveler record.
        String travSql = "SELECT t.guide, t.familyMembers, t.clubMember, u.firstName, u.lastName, u.email, u.phoneNumber "
                       + "FROM traveler t INNER JOIN `user` u ON t.user_id = u.user_id "
                       + "WHERE u.nationalId = ?";
        try (PreparedStatement ps = conn.prepareStatement(travSql)) {
            ps.setInt(1, nationalIdNumber);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new Object[]{
                "TRAVELER",
                rs.getBoolean("guide"),
                rs.getBoolean("clubMember"),
                rs.getInt("familyMembers"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                rs.getString("email"),
                rs.getString("phoneNumber")
            };
        }
    }

    private Object[] registerSubscriber(SubscriberRequest req) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String nationalId = req.getNationalId();
        if (nationalId == null || !nationalId.matches("\\d+")) {
            return new Object[]{false, "National ID must be a number."};
        }

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            String selectSql = "SELECT t.traveler_id, t.guide, t.clubMember, t.user_id "
                + "FROM traveler t "
                + "INNER JOIN `user` u ON t.user_id = u.user_id "
                + "WHERE u.nationalId = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectSql);
            selectPs.setString(1, nationalId);
            ResultSet rs = selectPs.executeQuery();

            String userId;
            String travelerId;
            boolean alreadyClubMember;
            boolean alreadyGuide;

            if (rs.next()) {
                travelerId     = rs.getString("traveler_id");
                userId         = rs.getString("user_id");
                alreadyClubMember = rs.getBoolean("clubMember");
                alreadyGuide   = rs.getBoolean("guide");

                // Clear the opposite role so guide and club member are mutually exclusive
                // (same-role re-submission just updates the person's info — no error)
                if (req.getType().equals(SubscriberRequest.TYPE_CLUB_MEMBER) && alreadyGuide) {
                    PreparedStatement clearGuide = conn.prepareStatement("UPDATE traveler SET guide = false WHERE traveler_id = ?");
                    clearGuide.setString(1, travelerId);
                    clearGuide.executeUpdate();
                } else if (req.getType().equals(SubscriberRequest.TYPE_GUIDE) && alreadyClubMember) {
                    PreparedStatement clearClub = conn.prepareStatement("UPDATE traveler SET clubMember = false WHERE traveler_id = ?");
                    clearClub.setString(1, travelerId);
                    clearClub.executeUpdate();
                }

                String updateUserSql = "UPDATE `user` SET firstName=?, lastName=?, email=?, phoneNumber=? WHERE user_id=?";
                PreparedStatement updateUserPs = conn.prepareStatement(updateUserSql);
                updateUserPs.setString(1, req.getFirstName());
                updateUserPs.setString(2, req.getLastName());
                updateUserPs.setString(3, req.getEmail());
                updateUserPs.setString(4, req.getPhoneNumber());
                updateUserPs.setString(5, userId);
                updateUserPs.executeUpdate();
            } else {
                userId     = generateUniqueId(conn, "`user`", "user_id");
                travelerId = generateUniqueId(conn, "traveler", "traveler_id");

                String insertUserSql = "INSERT INTO `user` (user_id, firstName, lastName, email, phoneNumber, nationalId) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement insertUserPs = conn.prepareStatement(insertUserSql);
                insertUserPs.setString(1, userId);
                insertUserPs.setString(2, req.getFirstName());
                insertUserPs.setString(3, req.getLastName());
                insertUserPs.setString(4, req.getEmail());
                insertUserPs.setString(5, req.getPhoneNumber());
                insertUserPs.setString(6, nationalId);
                insertUserPs.executeUpdate();

                String insertTravelerSql = "INSERT INTO traveler (traveler_id, guide, clubMember, familyMembers, user_id) VALUES (?, false, false, ?, ?)";
                PreparedStatement insertTravelerPs = conn.prepareStatement(insertTravelerSql);
                insertTravelerPs.setString(1, travelerId);
                insertTravelerPs.setInt(2, req.getFamilyMembers());
                insertTravelerPs.setString(3, userId);
                insertTravelerPs.executeUpdate();
            }

            if (req.getType().equals(SubscriberRequest.TYPE_CLUB_MEMBER)) {
                String updateSql = "UPDATE traveler SET clubMember = true, familyMembers = ? WHERE traveler_id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setInt(1, req.getFamilyMembers());
                updatePs.setString(2, travelerId);
                updatePs.executeUpdate();
            } else {
                String updateSql = "UPDATE traveler SET guide = true WHERE traveler_id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setString(1, travelerId);
                updatePs.executeUpdate();
            }

            conn.commit();
            String label = req.getType().equals(SubscriberRequest.TYPE_CLUB_MEMBER) ? "club member" : "tour guide";
            return new Object[]{true, req.getFirstName() + " " + req.getLastName() + " registered successfully as " + label + "."};
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private String generateUniqueId(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        String id;
        do {
            id = UUID.randomUUID().toString();
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) break;
        } while (true);
        return id;
    }

    private int getParkCurrentVisitors(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        return Utils.getParkCurrentVisitors(conn, parkId);
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
        return Utils.getEffectiveAvailableSpots(conn, parkId);
    }

    private int getParkGap(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT gap FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }
        return rs.getInt("gap");
    }

    private int getCheckedInWalkInVisitors(int parkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT COALESCE(SUM(visitorsInside), 0) AS walkedInVisitors " +
            "FROM booking WHERE park_id = ? AND status = ? AND walk_in = 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setString(2, Booking.STATUS_CHECKED_IN);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("walkedInVisitors") : 0;
    }

    private Booking getBookingById(int bookingId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM booking WHERE booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, bookingId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Utils.mapBooking(rs);
        return null;
    }


    private boolean checkInVisitor(Booking booking, int employeeParkId) throws SQLException {
        if (booking.getParkId() != employeeParkId) {
            throw new IllegalArgumentException("This booking belongs to another park.");
        }

        Connection conn = DBConnection.getStaticConnection();

        // The worker may enter fewer visitors than booked (e.g. someone did not show up).
        // When no explicit count is supplied (visitorsInside <= 0) we fall back to the booked amount,
        // which keeps the older Enter Visitor screen working unchanged.
        int visitorsEntering = booking.getVisitorsInside() > 0
            ? booking.getVisitorsInside() : booking.getNumberOfVisitors();

        String updateBooking = "UPDATE booking SET status = ?, visitorsInside = ?, entryTime = NOW() " +
                               "WHERE booking_id = ? AND status = ? AND park_id = ?";
        PreparedStatement ps = conn.prepareStatement(updateBooking);
        ps.setString(1, Booking.STATUS_CHECKED_IN);
        ps.setInt(2, visitorsEntering);
        ps.setInt(3, booking.getBookingId());
        ps.setString(4, Booking.STATUS_CONFIRMED);
        ps.setInt(5, employeeParkId);
        int rows = ps.executeUpdate();

        if (rows == 0) return false;

        Utils.syncParkCurrentVisitors(conn, employeeParkId);

        return true;
    }


    private boolean checkOutVisitor(ExitRequest request) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String getSql = "SELECT visitorsInside, status FROM booking WHERE booking_id = ?";
        PreparedStatement getPs = conn.prepareStatement(getSql);
        getPs.setInt(1, Integer.parseInt(request.getBookingId()));
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
        updatePs.setInt(4, Integer.parseInt(request.getBookingId()));
        updatePs.executeUpdate();

        Utils.syncParkCurrentVisitors(conn, request.getParkId());

        Booking updatedBooking = getBookingById(Integer.parseInt(request.getBookingId()));
        if (updatedBooking != null && Booking.STATUS_CHECKED_OUT.equals(updatedBooking.getStatus())) {
            BookingLifecycleService.handleSpotFreed(conn, updatedBooking.getParkId(), updatedBooking.getVisitorTime());
        }

        return true;
    }


    // Set the absolute number of visitors currently inside for a checked-in booking.
    // Lets a worker correct the count after check-in (e.g. a visitor arrived late, or some left).
    // Setting it to 0 walks everyone out and checks the booking out.
    private boolean setVisitorsInside(Booking booking, int employeeParkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String getSql = "SELECT status, park_id, numberOfVisitors FROM booking WHERE booking_id = ?";
        PreparedStatement getPs = conn.prepareStatement(getSql);
        getPs.setInt(1, booking.getBookingId());
        ResultSet rs = getPs.executeQuery();

        if (!rs.next()) {
            throw new IllegalArgumentException("Booking not found.");
        }

        String status = rs.getString("status");
        int parkId = rs.getInt("park_id");
        int booked = rs.getInt("numberOfVisitors");

        if (parkId != employeeParkId) {
            throw new IllegalArgumentException("This booking belongs to another park.");
        }
        if (!Booking.STATUS_CHECKED_IN.equals(status)) {
            throw new IllegalArgumentException("This booking is not checked in. Status: " + status);
        }

        int newCount = booking.getVisitorsInside();
        if (newCount < 0) newCount = 0;
        if (newCount > booked) {
            throw new IllegalArgumentException(
                "Cannot have more than " + booked + " visitor(s) inside for this booking.");
        }

        String newStatus = (newCount == 0) ? Booking.STATUS_CHECKED_OUT : Booking.STATUS_CHECKED_IN;
        String updateBooking = "UPDATE booking SET visitorsInside = ?, status = ?, " +
                               "exitTime = CASE WHEN ? = 0 THEN NOW() ELSE exitTime END WHERE booking_id = ?";
        PreparedStatement updatePs = conn.prepareStatement(updateBooking);
        updatePs.setInt(1, newCount);
        updatePs.setString(2, newStatus);
        updatePs.setInt(3, newCount);
        updatePs.setInt(4, booking.getBookingId());
        updatePs.executeUpdate();

        Utils.syncParkCurrentVisitors(conn, employeeParkId);

        if (newCount == 0) {
            Booking updatedBooking = getBookingById(booking.getBookingId());
            if (updatedBooking != null) {
                BookingLifecycleService.handleSpotFreed(conn, updatedBooking.getParkId(), updatedBooking.getVisitorTime());
            }
        }

        return true;
    }


    private boolean cancelWalkIn(Booking booking, int employeeParkId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();

        String getSql = "SELECT status, park_id FROM booking WHERE booking_id = ?";
        PreparedStatement getPs = conn.prepareStatement(getSql);
        getPs.setInt(1, booking.getBookingId());
        ResultSet rs = getPs.executeQuery();

        if (!rs.next()) {
            throw new IllegalArgumentException("Booking not found.");
        }

        String status = rs.getString("status");
        int parkId = rs.getInt("park_id");

        if (parkId != employeeParkId) {
            throw new IllegalArgumentException("This booking belongs to another park.");
        }
        if (!Booking.STATUS_CHECKED_IN.equals(status)) {
            throw new IllegalArgumentException("This booking is not checked in. Status: " + status);
        }

        String updateSql = "UPDATE booking SET visitorsInside = 0, status = ?, exitTime = NOW() WHERE booking_id = ?";
        PreparedStatement updatePs = conn.prepareStatement(updateSql);
        updatePs.setString(1, Booking.STATUS_CANCELLED);
        updatePs.setInt(2, booking.getBookingId());
        updatePs.executeUpdate();

        Utils.syncParkCurrentVisitors(conn, employeeParkId);
        pushLiveParkState(employeeParkId);

        return true;
    }


    private Booking processWalkIn(WalkInRequest request) throws SQLException {
        int walkInLimit = getParkGap(request.getParkId());
        int checkedInWalkInVisitors = getCheckedInWalkInVisitors(request.getParkId());
        if (checkedInWalkInVisitors >= walkInLimit) {
            throw new IllegalArgumentException(
                "This park has already reached its walk-in limit of " + walkInLimit + " visitor(s)."
            );
        }
        if (checkedInWalkInVisitors + request.getNumberOfVisitors() > walkInLimit) {
            int remainingWalkInSpots = walkInLimit - checkedInWalkInVisitors;
            throw new IllegalArgumentException(
                "Not enough walk-in spots available. Remaining walk-in spots: " + remainingWalkInSpots + "."
            );
        }

        int available = getEffectiveAvailableSpots(request.getParkId());
        if (available < request.getNumberOfVisitors()) {
            throw new IllegalArgumentException("Not enough spots available. Available: " + available);
        }

        VisitorLoginResult traveler = loginOrRegisterVisitor(request.getNationalId());
        String travelerId = traveler.getTravelerId();

        Connection conn = DBConnection.getStaticConnection();

        // Calculate price (full price for walk-in, no discount)
        double price = calculatePrice(conn, request.getParkId(), request.getNumberOfVisitors(), travelerId, false);

        int bookingId = (int)(Math.random() * 9000000) + 1000000;
        String sql = "INSERT INTO booking (booking_id, traveler_id, park_id, numberOfVisitors, visitorTime, status, organizedBooking, price, visitorsInside, entryTime, paid, walk_in) " +
            "VALUES (?, ?, ?, ?, NOW(), ?, false, ?, ?, NOW(), 1, 1)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, bookingId);
        ps.setString(2, travelerId);
        ps.setInt(3, request.getParkId());
        ps.setInt(4, request.getNumberOfVisitors());
        ps.setString(5, Booking.STATUS_CHECKED_IN);
        ps.setDouble(6, price);
        ps.setInt(7, request.getNumberOfVisitors()); // visitorsInside = all visitors
        ps.executeUpdate();

        Utils.syncParkCurrentVisitors(conn, request.getParkId());

        return new Booking(bookingId, travelerId, request.getParkId(),
            request.getNumberOfVisitors(), java.time.LocalDateTime.now(),
            Booking.STATUS_CHECKED_IN, false, price);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private java.time.LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
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
                    paramRs.getString("requestTitle"),
                    toLocalDateTime(paramRs.getTimestamp("request_date"))
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

    private ArrayList<ParkManagerActivityLogEntry> getParkManagerActivityLog(int employeeId) throws SQLException {
        ArrayList<ParkManagerActivityLogEntry> entries = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();

        String sql = "SELECT request_Id, requestTitle, request_date, parameter_type, new_value, approved " +
                     "FROM managerRequests " +
                     "WHERE employee_id = ? " +
                     "ORDER BY request_date DESC, request_Id DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, employeeId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            int approvedValue = rs.getInt("approved");
            Boolean approved = rs.wasNull() ? null : approvedValue == 1;

            entries.add(new ParkManagerActivityLogEntry(
                rs.getString("request_Id"),
                rs.getString("requestTitle"),
                toLocalDateTime(rs.getTimestamp("request_date")),
                ParkChangeRequest.ParameterType.valueOf(rs.getString("parameter_type")),
                rs.getInt("new_value"),
                approved
            ));
        }

        return entries;
    }

    private String getUsernameForEmployeeId(Connection conn, int employeeId) throws SQLException {
        String sql = "SELECT u.username FROM employee e " +
                     "JOIN `user` u ON e.user_id = u.user_id " +
                     "WHERE e.employee_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, employeeId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("username");
        return "unknown";
    }

    private void handleParkChangeRequest(ParkChangeRequest request, ConnectionToClient client) throws Exception {
        Connection conn = DBConnection.getStaticConnection();
        Integer depManagerId = getDepartmentManagerIdForPark(conn, request.getParkId());
        LocalDateTime requestDate = request.getRequestDate() != null
                ? request.getRequestDate()
                : LocalDateTime.now();
        String sql = "INSERT INTO managerRequests (request_Id, employee_id, dep_manager_id, " +
                     "requestTitle, parameter_type, new_value, park_id, request_date, approved) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, request.getRequestId());
        ps.setInt(2, request.getEmployeeId());
        ps.setInt(3, getDepartmentManagerIdForPark(conn, request.getParkId()));
        ps.setString(4, request.getRequestTitle());
        ps.setString(5, request.getParameterType().name());
        ps.setInt(6, request.getNewValue());
        ps.setInt(7, request.getParkId());
        ps.setTimestamp(8, Timestamp.valueOf(requestDate));
        ps.executeUpdate();

        ParkChangeRequest notificationRequest = new ParkChangeRequest(
            request.getParkId(),
            request.getParameterType(),
            request.getNewValue(),
            request.getRequestedByUsername(),
            request.getEmployeeId(),
            depManagerId == null ? 0 : depManagerId,
            request.getRequestId(),
            request.getRequestTitle(),
            requestDate
        );

        boolean depManagerOnline = false;
        for (Thread t : getClientConnections()) {
            if (!(t instanceof ConnectionToClient)) continue;
            ConnectionToClient c = (ConnectionToClient) t;
            Object role = c.getInfo("EMPLOYEE_ROLE");
            Object parkId = c.getInfo("EMPLOYEE_PARK_ID");
            if ("department_manager".equals(role) &&
                parkId != null && (int) parkId == request.getParkId()) {
                c.sendToClient(new Message("PARK_CHANGE_REQUEST_NOTIFICATION", notificationRequest));
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

    private Integer getDepartmentManagerIdForPark(Connection conn, int parkId) throws SQLException {
        String sql = "SELECT department_manager_id FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }
        int depManagerId = rs.getInt("department_manager_id");
        return rs.wasNull() ? null : depManagerId;
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

        String getRequesterSql = "SELECT u.username FROM managerRequests r " +
                                  "JOIN employee e ON r.employee_id = e.employee_id " +
                                  "JOIN `user` u ON e.user_id = u.user_id " +
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

        String getRequesterSql = "SELECT u.username FROM promotion_request r " +
                                  "JOIN employee e ON r.employee_id = e.employee_id " +
                                  "JOIN `user` u ON e.user_id = u.user_id " +
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
