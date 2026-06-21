package common;

import java.io.Serializable;
import java.time.LocalDate;

public class ParkReportRequest implements Serializable {
    private int parkId;
    private int employeeId;
    private LocalDate fromDate;
    private LocalDate toDate;

    public ParkReportRequest(int parkId, int employeeId, LocalDate fromDate, LocalDate toDate) {
        this.parkId = parkId;
        this.employeeId = employeeId;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public int getParkId() {
        return parkId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}