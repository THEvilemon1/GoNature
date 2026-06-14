package common;

import java.io.Serializable;

public class ExitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String bookingId;
    private final int visitorsLeaving;
    private final int parkId;

    public ExitRequest(String bookingId, int visitorsLeaving, int parkId) {
        this.bookingId = bookingId;
        this.visitorsLeaving = visitorsLeaving;
        this.parkId = parkId;
    }

    public String getBookingId() { return bookingId; }
    public int getVisitorsLeaving() { return visitorsLeaving; }
    public int getParkId() { return parkId; }
}