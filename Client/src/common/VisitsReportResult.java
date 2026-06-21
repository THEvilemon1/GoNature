package common;

import java.io.Serializable;

public class VisitsReportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String visitorType;
    private int visitsCount;
    private double avgStayMinutes;
    private String firstEntryTime;
    private String lastEntryTime;

    public VisitsReportResult(String visitorType, int visitsCount, double avgStayMinutes,
                               String firstEntryTime, String lastEntryTime) {
        this.visitorType = visitorType;
        this.visitsCount = visitsCount;
        this.avgStayMinutes = avgStayMinutes;
        this.firstEntryTime = firstEntryTime;
        this.lastEntryTime = lastEntryTime;
    }

    public String getVisitorType()    { return visitorType; }
    public int getVisitsCount()       { return visitsCount; }
    public double getAvgStayMinutes() { return avgStayMinutes; }
    public String getFirstEntryTime() { return firstEntryTime; }
    public String getLastEntryTime()  { return lastEntryTime; }
}
