package common;

import java.io.Serializable;

public class ParkVisitorsReportResult implements Serializable {
    private int individualVisitors;
    private int organizedVisitors;

    public ParkVisitorsReportResult(int individualVisitors, int organizedVisitors) {
        this.individualVisitors = individualVisitors;
        this.organizedVisitors = organizedVisitors;
    }

    public int getIndividualVisitors() {
        return individualVisitors;
    }

    public int getOrganizedVisitors() {
        return organizedVisitors;
    }

    public int getTotalVisitors() {
        return individualVisitors + organizedVisitors;
    }
}