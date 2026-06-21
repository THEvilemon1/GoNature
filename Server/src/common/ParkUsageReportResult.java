package common;

import java.io.Serializable;

public class ParkUsageReportResult implements Serializable {
    private String date;
    private int visitorsCount;
    private int maxCapacity;
    private double usagePercent;

    public ParkUsageReportResult(String date, int visitorsCount, int maxCapacity, double usagePercent) {
        this.date = date;
        this.visitorsCount = visitorsCount;
        this.maxCapacity = maxCapacity;
        this.usagePercent = usagePercent;
    }

    public String getDate() {
        return date;
    }

    public int getVisitorsCount() {
        return visitorsCount;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public double getUsagePercent() {
        return usagePercent;
    }
}