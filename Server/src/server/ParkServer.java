package server;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import common.Booking;
import common.Message;
import common.Order;
import common.VisitorLoginResult;
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

    // Case 1: Orderly disconnect — client called closeConnection()
    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        handleDisconnection(client, "orderly disconnect");
    }

    // Case 2: Abrupt disconnect — client crashed or closed window
    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
        handleDisconnection(client, "connection lost");
    }

    // Shared handler — uses a Set to prevent double processing
    private void handleDisconnection(ConnectionToClient client, String reason) {
        // disconnectedClients.add() returns false if already in set — prevents double processing
        if (disconnectedClients.add(client)) {
            String clientInfo = "unknown";
            Object savedIP = client.getInfo("IP");
            if (savedIP != null)
                clientInfo = (String) savedIP;

            System.out.println("Processing disconnection for: " + clientInfo + " reason: " + reason);
            
            // Remove this connection from active sessions
            UserSessionManager.getInstance().removeConnection(client);

            if (ServerPortFrameController.instance != null) {
                ServerPortFrameController.instance.clientDisconnected(clientInfo);
            }
        }
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
                    
                    // Check if this user is already logged in from another computer
                    ConnectionToClient oldConnection = UserSessionManager.getInstance()
                        .loginUser(loginResult.getTravelerId(), client);
                    
                    if (oldConnection != null) {
                        // User already logged in elsewhere — force logout the old connection
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
            + "WHERE booking_id = ? AND traveler_id = ? AND status <> ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, booking.getParkId());
        ps.setInt(2, booking.getNumberOfVisitors());
        ps.setTimestamp(3, Timestamp.valueOf(booking.getVisitorTime()));
        ps.setString(4, Booking.STATUS_PENDING);
        ps.setInt(5, price);
        ps.setString(6, booking.getBookingId());
        ps.setString(7, booking.getTravelerId());
        ps.setString(8, Booking.STATUS_CANCELLED);
        return ps.executeUpdate() > 0;
    }

    private boolean cancelBooking(Booking booking) throws SQLException {
        if (booking == null || booking.getBookingId() == null || booking.getTravelerId() == null) {
            throw new IllegalArgumentException("Booking details are missing.");
        }

        Connection conn = DBConnection.getStaticConnection();
        String sql = "UPDATE booking SET status = ? WHERE booking_id = ? AND traveler_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, Booking.STATUS_CANCELLED);
        ps.setString(2, booking.getBookingId());
        ps.setString(3, booking.getTravelerId());
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
            System.out.println("Registering new visitor user_id: " + travelerId);
            insertUserPs.setString(1, travelerId);
            insertUserPs.setString(2, "Visitor");
            insertUserPs.setString(3, "Guest");
            insertUserPs.setString(4, "visitor-" + travelerId + "@gonature.local");
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
}
