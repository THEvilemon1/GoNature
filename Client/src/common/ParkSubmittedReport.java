package common;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ParkSubmittedReport implements Serializable {
    private int reportId;
    private int parkId;
    private String parkName;
    private String reportTitle;
    private String content;
    private int employeeId;
    private String reportType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDateTime submittedAt;

    public ParkSubmittedReport(int parkId, String reportTitle, String content, int employeeId) {
        this(0, parkId, null, reportTitle, content, employeeId, null, null, null, null);
    }

    public ParkSubmittedReport(int reportId, int parkId, String reportTitle, String content,
                               int employeeId, String reportType, LocalDate fromDate,
                               LocalDate toDate, LocalDateTime submittedAt) {
        this(reportId, parkId, null, reportTitle, content, employeeId, reportType,
                fromDate, toDate, submittedAt);
    }

    public ParkSubmittedReport(int reportId, int parkId, String parkName, String reportTitle, String content,
                               int employeeId, String reportType, LocalDate fromDate,
                               LocalDate toDate, LocalDateTime submittedAt) {
        this.reportId = reportId;
        this.parkId = parkId;
        this.parkName = parkName;
        this.reportTitle = reportTitle;
        this.content = content;
        this.employeeId = employeeId;
        this.reportType = reportType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.submittedAt = submittedAt;
    }

    public int getReportId() {
        return reportId;
    }

    public int getParkId() {
        return parkId;
    }

    public String getParkName() {
        return parkName == null || parkName.trim().isEmpty() ? "Unknown Park" : parkName;
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

    public String getReportType() {
        return reportType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
