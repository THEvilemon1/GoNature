package common;

import java.io.Serializable;

public class ParkSubmittedReport implements Serializable {
    private int parkId;
    private String reportTitle;
    private String content;
    private int employeeId;

    public ParkSubmittedReport(int parkId, String reportTitle, String content, int employeeId) {
        this.parkId = parkId;
        this.reportTitle = reportTitle;
        this.content = content;
        this.employeeId = employeeId;
    }

    public int getParkId() {
        return parkId;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public String getContent() {
        return content;
    }

    public int getEmployeeId() {
        return employeeId;
    }
}