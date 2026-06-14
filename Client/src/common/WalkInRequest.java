package common;

import java.io.Serializable;

public class WalkInRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int parkId;
    private final int numberOfVisitors;
    private final String nationalId;

    public WalkInRequest(int parkId, int numberOfVisitors, String nationalId) {
        this.parkId = parkId;
        this.numberOfVisitors = numberOfVisitors;
        this.nationalId = nationalId;
    }

    public int getParkId() { return parkId; }
    public int getNumberOfVisitors() { return numberOfVisitors; }
    public String getNationalId() { return nationalId; }
}