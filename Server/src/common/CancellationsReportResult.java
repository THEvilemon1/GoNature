package common;

import java.io.Serializable;

/**
 * One row of the cancellations report, for a single park.
 *
 * "cancelledCount" = orders the traveler cancelled themselves.
 * "expiredCount"   = orders the system cancelled because the traveler never
 *                    confirmed in time (expired without cancellation).
 * "totalBookings"  = every booking made in the period, used to work out
 *                    what share of bookings ended up cancelled or expired.
 */
public class CancellationsReportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String parkName;
    private int cancelledCount;
    private int expiredCount;
    private int totalBookings;

    public CancellationsReportResult(String parkName, int cancelledCount,
                                     int expiredCount, int totalBookings) {
        this.parkName = parkName;
        this.cancelledCount = cancelledCount;
        this.expiredCount = expiredCount;
        this.totalBookings = totalBookings;
    }

    public String getParkName()   { return parkName; }
    public int getCancelledCount() { return cancelledCount; }
    public int getExpiredCount()   { return expiredCount; }
    public int getTotalBookings()  { return totalBookings; }

    /** Percentage of all bookings that were cancelled or expired (0 if no bookings). */
    public double getCancellationRate() {
        if (totalBookings == 0) {
            return 0.0;
        }
        return (cancelledCount + expiredCount) * 100.0 / totalBookings;
    }
}
