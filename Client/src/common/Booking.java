package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    public static final String STATUS_SYSTEM_CANCEL = "SYSTEM_CANCEL";

    private String bookingId;
    private String travelerId;
    private int parkId;
    private int numberOfVisitors;
    private LocalDateTime visitorTime;
    private String status;
    private boolean organizedBooking;
    private int price;

    public Booking(String bookingId, String travelerId, int parkId, int numberOfVisitors,
                   LocalDateTime visitorTime, String status, boolean organizedBooking, int price) {
        this.bookingId = bookingId;
        this.travelerId = travelerId;
        this.parkId = parkId;
        this.numberOfVisitors = numberOfVisitors;
        this.visitorTime = visitorTime;
        this.status = status;
        this.organizedBooking = organizedBooking;
        this.price = price;
    }

    public String getBookingId() { return bookingId; }
    public String getTravelerId() { return travelerId; }
    public int getParkId() { return parkId; }
    public int getNumberOfVisitors() { return numberOfVisitors; }
    public LocalDateTime getVisitorTime() { return visitorTime; }
    public String getStatus() { return status; }
    public boolean isOrganizedBooking() { return organizedBooking; }
    public int getPrice() { return price; }
}
