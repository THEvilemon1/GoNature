package common;

import java.io.Serializable;

public class ParkChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ParameterType { MAX_CAPACITY, GAP, DEFAULT_STAY_TIME, PRICE_PER_PERSON }

    private int parkId;
    private ParameterType parameterType;
    private int newValue;
    private String requestedByUsername;
    private int employeeId;
    private int depManagerId;
    private String requestId;
    private String requestTitle;

    public ParkChangeRequest(int parkId, ParameterType parameterType, int newValue,
                              String requestedByUsername, int employeeId,
                              int depManagerId, String requestId, String requestTitle) {
        this.parkId = parkId;
        this.parameterType = parameterType;
        this.newValue = newValue;
        this.requestedByUsername = requestedByUsername;
        this.employeeId = employeeId;
        this.depManagerId = depManagerId;
        this.requestId = requestId;
        this.requestTitle = requestTitle;
    }

    public int getParkId()                  { return parkId; }
    public ParameterType getParameterType() { return parameterType; }
    public int getNewValue()                { return newValue; }
    public String getRequestedByUsername()  { return requestedByUsername; }
    public int getEmployeeId()              { return employeeId; }
    public int getDepManagerId()            { return depManagerId; }
    public String getRequestId()            { return requestId; }
    public String getRequestTitle()         { return requestTitle; }
}