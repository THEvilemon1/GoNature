package server;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

                case "TRAVELER_LOGIN":
                    String nationalId = (String) message.getData();
                    VisitorLoginResult loginResult = loginOrRegisterVisitor(nationalId);
                    client.sendToClient(new Message("VISITOR_LOGIN_RESULT", loginResult));
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
