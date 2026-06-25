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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParkUsageReportViewController implements ServerResponseListener {

    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextArea reportArea;
    @FXML private BarChart<String, Number> usageChart;
    @FXML private Button btnSendReport;

    private Employee employee;
    private final Map<String, Integer> monthMap = new HashMap<>();
    private ParkReportRequest lastGeneratedRequest;
    private ArrayList<ParkUsageReportResult> lastGeneratedResult;

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @FXML
    public void initialize() {
        monthMap.put("January", 1);
        monthMap.put("February", 2);
        monthMap.put("March", 3);
        monthMap.put("April", 4);
        monthMap.put("May", 5);
        monthMap.put("June", 6);
        monthMap.put("July", 7);
        monthMap.put("August", 8);
        monthMap.put("September", 9);
        monthMap.put("October", 10);
        monthMap.put("November", 11);
        monthMap.put("December", 12);

        monthComboBox.getItems().addAll(monthMap.keySet());
        yearComboBox.getItems().addAll(2025, 2026, 2027);

        LocalDate today = LocalDate.now();
        monthComboBox.setValue(today.getMonth().toString().substring(0, 1) +
                today.getMonth().toString().substring(1).toLowerCase());
        yearComboBox.setValue(today.getYear());
        btnSendReport.setDisable(true);
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        if (monthComboBox.getValue() == null || yearComboBox.getValue() == null) {
            reportArea.setText("Please select month and year.");
            return;
        }
        if (employee == null) {
            reportArea.setText("Employee details are missing.");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            reportArea.setText("Not connected to server.");
            return;
        }

        try {
            client.setListener(this);
            btnSendReport.setDisable(true);
            lastGeneratedRequest = null;
            lastGeneratedResult = null;

            int month = monthMap.get(monthComboBox.getValue());
            int year = yearComboBox.getValue();
            LocalDate fromDate = LocalDate.of(year, month, 1);
            LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());

            ParkReportRequest request = new ParkReportRequest(
                    employee.getParkId(),
                    employee.getEmployeeId(),
                    fromDate,
                    toDate
            );
            lastGeneratedRequest = request;

            client.sendToServer(new Message("PARK_USAGE_REPORT", request));

        } catch (Exception e) {
            reportArea.setText("Error sending request: " + e.getMessage());
        }
    }

    @FXML
    public void handleSendReport(ActionEvent event) {
        if (lastGeneratedRequest == null || lastGeneratedResult == null) {
            reportArea.setText("Generate a report before sending it.");
            return;
        }
        if (lastGeneratedResult.isEmpty()) {
            btnSendReport.setDisable(true);
            reportArea.setText(reportArea.getText() + "\n\nCannot send an empty report.");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            reportArea.setText("Not connected to server.");
            return;
        }

        try {
            client.setListener(this);
            btnSendReport.setDisable(true);
            client.sendToServer(new Message("SUBMIT_PARK_USAGE_REPORT", lastGeneratedRequest));
        } catch (Exception e) {
            btnSendReport.setDisable(false);
            reportArea.setText("Error sending report: " + e.getMessage());
        }
    }

    @Override
    public void onParkUsageReportResult(ArrayList<ParkUsageReportResult> results) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder("Usage Report\n\n");
            usageChart.getData().clear();
            lastGeneratedResult = results;

            if (results == null || results.isEmpty()) {
                reportArea.setText("No usage data for the selected period.");
                btnSendReport.setDisable(true);
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
            btnSendReport.setDisable(false);
        });
    }

    @Override
    public void onParkUsageReportSubmitResult(boolean success) {
        Platform.runLater(() -> {
            if (success) {
                reportArea.setText(reportArea.getText() + "\n\nReport sent to Department Manager.");
            } else {
                reportArea.setText(reportArea.getText() + "\n\nReport was not sent because it has no usage rows.");
            }
            btnSendReport.setDisable(success || lastGeneratedResult == null || lastGeneratedResult.isEmpty());
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
