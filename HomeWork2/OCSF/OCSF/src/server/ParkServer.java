package server;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import common.Message;
import common.Order;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class ParkServer extends AbstractServer {

    public ParkServer(int port) {
        super(port);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
    	
    	System.out.println("Message received from client: " + msg);

        if (!(msg instanceof Message))
            return;

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
        Connection conn = DBConnection.getConnection();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM `Order`");

        while (rs.next()) {
            Order o = new Order(
                rs.getInt("order_number"),
                rs.getDate("order_date"),
                rs.getInt("number_of_visitors"),
                rs.getInt("confirmation_code"),
                rs.getInt("subscriber_id"),
                rs.getDate("date_of_placing_order")
            );
            list.add(o);
        }

        return list;
    }

    private void addOrder(Order order) throws SQLException {

        Connection conn = DBConnection.getConnection();

        String sql = "INSERT INTO `Order` " +
                "(order_number, order_date, number_of_visitors, confirmation_code, subscriber_id, date_of_placing_order) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, order.getOrderNumber());
        ps.setDate(2, order.getOrderDate());
        ps.setInt(3, order.getNumberOfVisitors());
        ps.setInt(4, order.getConfirmationCode());
        ps.setInt(5, order.getSubscriberId());
        ps.setDate(6, order.getDateOfPlacingOrder());

        ps.executeUpdate();
    }
    
    private boolean checkOrderExists(int orderNumber) throws SQLException {

        Connection conn = DBConnection.getConnection();

        String sql = "SELECT order_number FROM `Order` WHERE order_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, orderNumber);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private boolean updateOrder(Order order) throws SQLException {

        Connection conn = DBConnection.getConnection();

        String sql = "UPDATE `Order` SET order_date = ?, number_of_visitors = ? WHERE order_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setDate(1, order.getOrderDate());
        ps.setInt(2, order.getNumberOfVisitors());
        ps.setInt(3, order.getOrderNumber());

        int rowsAffected = ps.executeUpdate();

        return rowsAffected > 0;
    }
    
    
}