package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Employee;
import common.Message;
import common.VisitsReportRequest;
import common.VisitsReportResult;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VisitorsReportViewController implements ServerResponseListener {

    private Employee employee;

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextArea reportArea;
    @FXML private BarChart<String, Number> visitsBarChart;

    private final Map<String, Integer> monthMap = new HashMap<>();

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

        monthComboBox.getItems().addAll(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        yearComboBox.getItems().addAll(2025, 2026, 2027);

        monthComboBox.setValue("June");
        yearComboBox.setValue(2026);
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        if (monthComboBox.getValue() == null || yearComboBox.getValue() == null) {
            reportArea.setText("Please select month and year.");
            return;
        }
        if (employee == null) {
            reportArea.setText("Employee session not found.");
            return;
        }

        try {
            ParkClient.getInstance().setListener(this);

            int month = monthMap.get(monthComboBox.getValue());
            int year = yearComboBox.getValue();

            LocalDate fromDate = LocalDate.of(year, month, 1);
            LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());

            VisitsReportRequest request = new VisitsReportRequest(
                employee.getParkId(), fromDate, toDate
            );

            ParkClient.getInstance().sendToServer(new Message("GET_VISITS_REPORT", request));

        } catch (Exception e) {
            reportArea.setText("Error sending request: " + e.getMessage());
        }
    }

    @Override
    public void onVisitsReportResult(ArrayList<VisitsReportResult> results) {
        Platform.runLater(() -> {
            visitsBarChart.getData().clear();

            XYChart.Series<String, Number> countSeries = new XYChart.Series<>();
            countSeries.setName("Visit Count");

            XYChart.Series<String, Number> avgStaySeries = new XYChart.Series<>();
            avgStaySeries.setName("Avg Stay (min)");

            StringBuilder sb = new StringBuilder("Visits Report\n\n");

            if (results == null || results.isEmpty()) {
                sb.append("No data found for the selected period.");
            } else {
                for (VisitsReportResult r : results) {
                    countSeries.getData().add(
                        new XYChart.Data<>(r.getVisitorType(), r.getVisitsCount()));
                    avgStaySeries.getData().add(
                        new XYChart.Data<>(r.getVisitorType(), r.getAvgStayMinutes()));

                    sb.append("Type: ").append(r.getVisitorType()).append("\n")
                      .append("  Visits: ").append(r.getVisitsCount()).append("\n")
                      .append("  Avg Stay: ").append(String.format("%.1f", r.getAvgStayMinutes())).append(" min\n")
                      .append("  First Entry: ").append(r.getFirstEntryTime()).append("\n")
                      .append("  Last Entry: ").append(r.getLastEntryTime()).append("\n\n");
                }
            }

            visitsBarChart.getData().add(countSeries);
            visitsBarChart.getData().add(avgStaySeries);
            reportArea.setText(sb.toString());
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
