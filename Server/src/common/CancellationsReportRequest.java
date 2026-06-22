package common;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Request sent by the department manager to build the cancellations report.
 * It only carries a date range because the report always covers every park
 * in the region (so the manager can compare parks against each other).
 */
public class CancellationsReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDate fromDate;
    private LocalDate toDate;

    public CancellationsReportRequest(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate()   { return toDate; }
}
