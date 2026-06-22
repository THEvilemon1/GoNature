package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Employee;
import common.Message;
import common.ParkReportRequest;
import common.ParkUsageReportResult;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;

public class ParkUsageReportViewController implements ServerResponseListener {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TextArea reportArea;
    @FXML private BarChart<String, Number> usageChart;

    private Employee employee;

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.withDayOfMonth(1));
        toDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) {
            reportArea.setText("Please select both From and To dates.");
            return;
        }
        if (toDate.isBefore(fromDate)) {
            reportArea.setText("'To' date must not be before 'From' date.");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            reportArea.setText("Not connected to server.");
            return;
        }

        try {
            client.setListener(this);

            ParkReportRequest request = new ParkReportRequest(
                    employee.getParkId(),
                    employee.getEmployeeId(),
                    fromDate,
                    toDate
            );

            client.sendToServer(new Message("PARK_USAGE_REPORT", request));

        } catch (Exception e) {
            reportArea.setText("Error sending request: " + e.getMessage());
        }
    }

    @Override
    public void onParkUsageReportResult(ArrayList<ParkUsageReportResult> results) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder("Usage Report\n\n");
            usageChart.getData().clear();

            if (results == null || results.isEmpty()) {
                reportArea.setText("No usage data for the selected period.");
                return;
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Usage %");

            for (ParkUsageReportResult r : results) {
                sb.append(r.getDate()).append(": ")
                  .append(r.getVisitorsCount()).append("/").append(r.getMaxCapacity())
                  .append(" (").append(String.format("%.1f", r.getUsagePercent())).append("%)\n");
                series.getData().add(new XYChart.Data<>(r.getDate(), r.getUsagePercent()));
            }

            reportArea.setText(sb.toString());
            usageChart.getData().add(series);
        });
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) reportArea.getScene().getWindow();
        stage.close();
    }

    @Override public void onOrderExistsResult(boolean exists) {}
    @Override public void onOrderResult(common.Order order) {}
    @Override public void onUpdateOrderResult(boolean success) {}

    @Override
    public void onError(String errorMessage) {
        Platform.runLater(() -> reportArea.setText("Error: " + errorMessage));
    }
}
