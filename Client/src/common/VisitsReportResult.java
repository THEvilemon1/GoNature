package common;

import java.io.Serializable;

public class VisitsReportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String visitorType;
    private String entryTime;
    private Integer stayMinutes;

    public VisitsReportResult(String visitorType, String entryTime, Integer stayMinutes) {
        this.visitorType = visitorType;
        this.entryTime = entryTime;
        this.stayMinutes = stayMinutes;
    }

    public String getVisitorType()  { return visitorType; }
    public String getEntryTime()    { return entryTime; }
    public Integer getStayMinutes() { return stayMinutes; }
}
