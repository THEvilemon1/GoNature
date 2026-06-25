package gui.employee;

import common.ParkSubmittedReport;
import common.ParkSubmittedReportDetails;
import common.ParkUsageReportResult;
import common.ParkVisitorsReportResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SubmittedReportViewController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML private Label lblTitle;
    @FXML private Label lblMeta;
    @FXML private TextArea reportArea;
    @FXML private Button btnShowGraph;
    @FXML private PieChart visitorsPieChart;
    @FXML private BarChart<String, Number> usageChart;

    private ParkSubmittedReportDetails details;

    public void setReportDetails(ParkSubmittedReportDetails details) {
        this.details = details;
        ParkSubmittedReport report = details.getReport();
        lblTitle.setText(report.getReportTitle());
        lblMeta.setText(buildMeta(report));
        reportArea.setText(report.getContent() == null ? "" : report.getContent());

        hideGraphs();
        btnShowGraph.setDisable(!hasGraphData(details));
    }

    @FXML
    public void handleShowGraph(ActionEvent event) {
        if (details == null || !hasGraphData(details)) {
            reportArea.setText(reportArea.getText() +
                    "\n\nGraph data is not available for this saved report.");
            btnShowGraph.setDisable(true);
            return;
        }

        hideGraphs();
        ParkSubmittedReport report = details.getReport();
        if ("VISITORS".equals(report.getReportType())) {
            showVisitorsReport(details.getVisitorsResult());
        } else if ("USAGE".equals(report.getReportType())) {
            showUsageReport(details.getUsageResults());
        }
    }

    private boolean hasGraphData(ParkSubmittedReportDetails details) {
        if (details == null || details.getReport() == null) return false;
        String reportType = details.getReport().getReportType();
        if ("VISITORS".equals(reportType)) {
            return details.getVisitorsResult() != null;
        }
        if ("USAGE".equals(reportType)) {
            return details.getUsageResults() != null && !details.getUsageResults().isEmpty();
        }
        return false;
    }

    private void hideGraphs() {
        visitorsPieChart.setVisible(false);
        visitorsPieChart.setManaged(false);
        usageChart.setVisible(false);
        usageChart.setManaged(false);
    }

    private void showVisitorsReport(ParkVisitorsReportResult result) {
        visitorsPieChart.getData().clear();
        visitorsPieChart.getData().add(new PieChart.Data("Individual Visitors", result.getIndividualVisitors()));
        visitorsPieChart.getData().add(new PieChart.Data("Organized Groups", result.getOrganizedVisitors()));
        visitorsPieChart.setVisible(true);
        visitorsPieChart.setManaged(true);
    }

    private void showUsageReport(ArrayList<ParkUsageReportResult> results) {
        usageChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Usage %");

        for (ParkUsageReportResult r : results) {
            series.getData().add(new XYChart.Data<>(r.getDate(), r.getUsagePercent()));
        }

        usageChart.getData().add(series);
        usageChart.setVisible(true);
        usageChart.setManaged(true);
    }

    private String buildMeta(ParkSubmittedReport report) {
        String period = "";
        if (report.getFromDate() != null && report.getToDate() != null) {
            period = " | Period: " + report.getFromDate().format(DATE_FORMAT) +
                    " to " + report.getToDate().format(DATE_FORMAT);
        }
        return "Park: " + report.getParkId() +
                " (" + report.getParkName() + ")" +
                " | Employee: " + report.getEmployeeId() +
                (report.getReportType() == null ? "" : " | Type: " + report.getReportType()) +
                period;
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) reportArea.getScene().getWindow();
        stage.close();
    }
}
