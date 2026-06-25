package common;

import java.io.Serializable;
import java.util.ArrayList;

public class ParkSubmittedReportDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ParkSubmittedReport report;
    private final ParkVisitorsReportResult visitorsResult;
    private final ArrayList<ParkUsageReportResult> usageResults;

    public ParkSubmittedReportDetails(ParkSubmittedReport report,
                                      ParkVisitorsReportResult visitorsResult,
                                      ArrayList<ParkUsageReportResult> usageResults) {
        this.report = report;
        this.visitorsResult = visitorsResult;
        this.usageResults = usageResults;
    }

    public ParkSubmittedReport getReport() {
        return report;
    }

    public ParkVisitorsReportResult getVisitorsResult() {
        return visitorsResult;
    }

    public ArrayList<ParkUsageReportResult> getUsageResults() {
        return usageResults;
    }
}
