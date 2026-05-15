package server;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import common.Message;
import common.Order;
import gui.ServerPortFrameController;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class ParkServer extends AbstractServer {

    public ParkServer(int port) {
        super(port);
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        String clientInfo = client.getInetAddress().getHostAddress();
        client.setInfo("IP", clientInfo); // save IP immediately
        System.out.println("Client connected: " + clientInfo);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.clientConnected(clientInfo);
    }

    // Case 1: Orderly disconnect — called manually or by OCSF
    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        handleDisconnection(client, "orderly disconnect");
    }

    // Case 2: OCSF always calls this when connection is lost (orderly or abrupt)
    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
        handleDisconnection(client, "connection lost");
    }

    private synchronized void handleDisconnection(ConnectionToClient client, String reason) {
        System.out.println("handleDisconnection called! reason: " + reason);
        if (client.getInfo("Disconnected") == null) {
            client.setInfo("Disconnected", true);

            // Get saved IP instead of reading from socket (which may be closed)
            String clientInfo = "unknown";
            Object savedIP = client.getInfo("IP");
            if (savedIP != null)
                clientInfo = (String) savedIP;

            System.out.println("Processing disconnection for: " + clientInfo + " reason: " + reason);

            if (ServerPortFrameController.instance != null) {
                ServerPortFrameController.instance.clientDisconnected(clientInfo);
                ServerPortFrameController.instance.log("Client disconnected (" + reason + "): " + clientInfo);
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
}