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

import common.ParkSubmittedReport;
import common.ParkReportRequest;
import common.ParkVisitorsReportResult;
import common.ParkUsageReportResult;
import common.ParkChangeRequest;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import common.WalkInRequest;
import common.ExitRequest;
import common.VisitorLoginResult;
import common.Promotion;
import common.PromotionRequest;
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
                    Booking createdBooking = createBooking(bookingToCreate);
                    client.sendToClient(new Message("CREATE_BOOKING_RESULT", createdBooking));
                    break;

                case "GET_TRAVELER_BOOKINGS":
                    String travelerId = (String) message.getData();
                    client.sendToClient(new Message("TRAVELER_BOOKINGS_RESULT", getTravelerBookings(travelerId)));
                    break;

                case "GET_PARK_PRICES":
                    client.sendToClient(new Message("PARK_PRICES_RESULT", getParkPrices()));
                    break;

                case "UPDATE_BOOKING":
                    Booking bookingToUpdate = (Booking) message.getData();
                    client.sendToClient(new Message("UPDATE_BOOKING_RESULT", updateBooking(bookingToUpdate)));
                    break;

                case "CANCEL_BOOKING":
                    Booking bookingToCancel = (Booking) message.getData();
                    client.sendToClient(new Message("CANCEL_BOOKING_RESULT", cancelBooking(bookingToCancel)));
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

                	    client.sendToClient(
                	            new Message("PARK_USAGE_REPORT_RESULT", usageResults));
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

                default:
                    client.sendToClient(new Message("ERROR", "Unknown command"));
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
            list.add(mapBooking(rs));
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

    private Booking createBooking(Booking booking) throws SQLException {
        validateBooking(booking);

        Connection conn = DBConnection.getStaticConnection();
        String bookingId = UUID.randomUUID().toString();
        int price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors());

        String sql = "INSERT INTO booking (booking_id, traveler_id, park_id, numberOfVisitors, visitorTime, status, organizedBooking, price) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ps.setString(2, booking.getTravelerId());
        ps.setInt(3, booking.getParkId());
        ps.setInt(4, booking.getNumberOfVisitors());
        ps.setTimestamp(5, Timestamp.valueOf(booking.getVisitorTime()));
        ps.setString(6, Booking.STATUS_PENDING);
        ps.setBoolean(7, false);
        ps.setInt(8, price);
        ps.executeUpdate();

        return new Booking(bookingId, booking.getTravelerId(), booking.getParkId(),
            booking.getNumberOfVisitors(), booking.getVisitorTime(), Booking.STATUS_PENDING, false, price);
    }

    private ArrayList<Booking> getTravelerBookings(String travelerId) throws SQLException {
        ArrayList<Booking> bookings = new ArrayList<>();
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT * FROM booking WHERE traveler_id = ? ORDER BY visitorTime";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            bookings.add(mapBooking(rs));
        }

        return bookings;
    }

    private boolean updateBooking(Booking booking) throws SQLException {
        validateBooking(booking);

        Connection conn = DBConnection.getStaticConnection();
        int price = calculatePrice(conn, booking.getParkId(), booking.getNumberOfVisitors());

        String sql = "UPDATE booking SET park_id = ?, numberOfVisitors = ?, visitorTime = ?, status = ?, price = ? "
            + "WHERE booking_id = ? AND traveler_id = ? "
            + "AND status NOT IN (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, booking.getParkId());
        ps.setInt(2, booking.getNumberOfVisitors());
        ps.setTimestamp(3, Timestamp.valueOf(booking.getVisitorTime()));
        ps.setString(4, Booking.STATUS_PENDING);
        ps.setInt(5, price);
        ps.setString(6, booking.getBookingId());
        ps.setString(7, booking.getTravelerId());
        ps.setString(8, Booking.STATUS_CANCELLED);
        ps.setString(9, Booking.STATUS_CHECKED_IN);
        ps.setString(10, Booking.STATUS_CHECKED_OUT);
        return ps.executeUpdate() > 0;
    }

    private boolean cancelBooking(Booking booking) throws SQLException {
        if (booking == null || booking.getBookingId() == null || booking.getTravelerId() == null) {
            throw new IllegalArgumentException("Booking details are missing.");
        }

        Connection conn = DBConnection.getStaticConnection();
        String sql = "UPDATE booking SET status = ? WHERE booking_id = ? AND traveler_id = ? "
            + "AND status NOT IN (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, Booking.STATUS_CANCELLED);
        ps.setString(2, booking.getBookingId());
        ps.setString(3, booking.getTravelerId());
        ps.setString(4, Booking.STATUS_CANCELLED);
        ps.setString(5, Booking.STATUS_CHECKED_IN);
        ps.setString(6, Booking.STATUS_CHECKED_OUT);
        return ps.executeUpdate() > 0;
    }

    private void validateBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking details are missing.");
        }
        if (booking.getTravelerId() == null || booking.getTravelerId().isBlank()) {
            throw new IllegalArgumentException("Traveler is missing.");
        }
        if (booking.getParkId() < 1 || booking.getParkId() > 4) {
            throw new IllegalArgumentException("Please choose a valid park.");
        }
        if (booking.getNumberOfVisitors() < 1 || booking.getNumberOfVisitors() > 15) {
            throw new IllegalArgumentException("Visitors must be between 1 and 15.");
        }
        if (booking.getVisitorTime() == null || !booking.getVisitorTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time must be in the future.");
        }
        if (booking.getVisitorTime().toLocalTime().isAfter(java.time.LocalTime.of(21, 0))) {
            throw new IllegalArgumentException("Bookings can only be made until 21:00.");
        }
    }

    private int calculatePrice(Connection conn, int parkId, int numberOfVisitors) throws SQLException {
        String sql = "SELECT pricePerPerson FROM park WHERE park_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }

        return rs.getInt("pricePerPerson") * numberOfVisitors;
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

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Timestamp visitorTimestamp = rs.getTimestamp("visitorTime");
        return new Booking(
            rs.getString("booking_id"),
            rs.getString("traveler_id"),
            rs.getInt("park_id"),
            rs.getInt("numberOfVisitors"),
            visitorTimestamp.toLocalDateTime(),
            rs.getString("status"),
            rs.getBoolean("organizedBooking"),
            rs.getInt("price")
        );
    }

    private VisitorLoginResult loginOrRegisterVisitor(String nationalId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        int nationalIdNumber = Integer.parseInt(nationalId);

        String selectSql = "SELECT traveler_id FROM traveler WHERE nationalId = ?";
        PreparedStatement selectPs = conn.prepareStatement(selectSql);
        selectPs.setInt(1, nationalIdNumber);
        ResultSet rs = selectPs.executeQuery();
        if (rs.next()) {
            return new VisitorLoginResult(rs.getString("traveler_id"), nationalId, false);
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

            String insertTravelerSql = "INSERT INTO traveler (traveler_id, nationalId, guide, clubMember) VALUES (?, ?, ?, ?)";
            PreparedStatement insertTravelerPs = conn.prepareStatement(insertTravelerSql);
            System.out.println("Registering traveler details for national ID: " + nationalId);
            insertTravelerPs.setString(1, travelerId);
            insertTravelerPs.setInt(2, nationalIdNumber);
            insertTravelerPs.setBoolean(3, false);
            insertTravelerPs.setBoolean(4, false);
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
        if (rs.next()) return mapBooking(rs);
        return null;
    }


    private boolean checkInVisitor(Booking booking, int employeeParkId) throws SQLException {
        if (booking.getParkId() != employeeParkId) {
            throw new IllegalArgumentException("This booking belongs to another park.");
        }

        Connection conn = DBConnection.getStaticConnection();

        String updateBooking = "UPDATE booking SET status = ?, visitorsInside = numberOfVisitors " +
                               "WHERE booking_id = ? AND status = ? AND park_id = ?";
        PreparedStatement ps = conn.prepareStatement(updateBooking);
        ps.setString(1, Booking.STATUS_CHECKED_IN);
        ps.setString(2, booking.getBookingId());
        ps.setString(3, Booking.STATUS_PENDING);
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
     String updateBooking = "UPDATE booking SET visitorsInside = ?, status = ? WHERE booking_id = ?";
     PreparedStatement updatePs = conn.prepareStatement(updateBooking);
     updatePs.setInt(1, remaining);
     updatePs.setString(2, newStatus);
     updatePs.setString(3, request.getBookingId());
     updatePs.executeUpdate();

     String updatePark = "UPDATE park SET currentVisitors = GREATEST(0, currentVisitors - ?) WHERE park_id = ?";
     PreparedStatement parkPs = conn.prepareStatement(updatePark);
     parkPs.setInt(1, request.getVisitorsLeaving());
     parkPs.setInt(2, request.getParkId());
     parkPs.executeUpdate();

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
	
	  int price = calculatePrice(conn, request.getParkId(), request.getNumberOfVisitors());
	
	  String bookingId = UUID.randomUUID().toString();
	  String sql = "INSERT INTO booking (booking_id, traveler_id, park_id, numberOfVisitors, visitorTime, status, organizedBooking, price, visitorsInside) " +
	      "VALUES (?, ?, ?, ?, NOW(), ?, false, ?, ?)";
	  PreparedStatement ps = conn.prepareStatement(sql);
	  ps.setString(1, bookingId);
	  ps.setString(2, travelerId);
	  ps.setInt(3, request.getParkId());
	  ps.setInt(4, request.getNumberOfVisitors());
	  ps.setString(5, Booking.STATUS_CHECKED_IN);
	  ps.setInt(6, price);
	  ps.setInt(7, request.getNumberOfVisitors());
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

	            // Send pending park parameter change requests
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

	            // Send pending promotion requests
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

	        String updateSql = "UPDATE managerRequests SET approved = ? WHERE request_Id = ?";
	        PreparedStatement updatePs = conn.prepareStatement(updateSql);
	        updatePs.setInt(1, approved ? 1 : 0);
	        updatePs.setString(2, requestId);
	        updatePs.executeUpdate();

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
	                    case "PRICE_PER_PERSON":  column = "pricePerPerson"; break;
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