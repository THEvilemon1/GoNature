package common;

import java.io.Serializable;
import java.sql.Date;

public class Order implements Serializable {

    private int orderNumber;
    private Date orderDate;
    private int numberOfVisitors;
    private int confirmationCode;
    private int subscriberId;
    private Date dateOfPlacingOrder;

    public Order(int orderNumber, Date orderDate, int numberOfVisitors,
                 int confirmationCode, int subscriberId, Date dateOfPlacingOrder) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.numberOfVisitors = numberOfVisitors;
        this.confirmationCode = confirmationCode;
        this.subscriberId = subscriberId;
        this.dateOfPlacingOrder = dateOfPlacingOrder;
    }

    public int getOrderNumber() { return orderNumber; }
    public Date getOrderDate() { return orderDate; }
    public int getNumberOfVisitors() { return numberOfVisitors; }
    public int getConfirmationCode() { return confirmationCode; }
    public int getSubscriberId() { return subscriberId; }
    public Date getDateOfPlacingOrder() { return dateOfPlacingOrder; }

    @Override
    public String toString() {
        return "Order #" + orderNumber + " Visitors=" + numberOfVisitors;
    }
}