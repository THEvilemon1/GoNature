package common;

import java.io.Serializable;

/**
 * Holds the live visitor count for a single park.
 * The department manager screen shows one of these per park
 * so the manager can see how full every park is right now.
 */
public class ParkVisitorsCount implements Serializable {
    private static final long serialVersionUID = 1L;

    private int parkId;
    private String parkName;
    private int currentVisitors;
    private int maxCapacity;

    public ParkVisitorsCount(int parkId, String parkName, int currentVisitors, int maxCapacity) {
        this.parkId = parkId;
        this.parkName = parkName;
        this.currentVisitors = currentVisitors;
        this.maxCapacity = maxCapacity;
    }

    public int getParkId()          { return parkId; }
    public String getParkName()     { return parkName; }
    public int getCurrentVisitors() { return currentVisitors; }
    public int getMaxCapacity()     { return maxCapacity; }
}
