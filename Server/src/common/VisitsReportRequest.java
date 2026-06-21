package common;

import java.io.Serializable;
import java.time.LocalDate;

public class VisitsReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int parkId;
    private LocalDate fromDate;
    private LocalDate toDate;

    public VisitsReportRequest(int parkId, LocalDate fromDate, LocalDate toDate) {
        this.parkId = parkId;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public int getParkId()       { return parkId; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate()   { return toDate; }
}
