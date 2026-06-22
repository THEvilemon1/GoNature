package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.CancellationsReportRequest;
import common.CancellationsReportResult;
import common.Employee;
import common.Message;
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

/**
 * Department manager screen that shows, for the chosen month, how many
 * bookings in each park were cancelled by the traveler versus expired
 * because the traveler never confirmed in time.
 *
 * This is built the same way as VisitorsReportViewController: pick a month
 * and year, ask the server, then draw a bar chart plus a text summary.
 */
public class CancellationsReportViewController implements ServerResponseListener {

    private Employee employee;

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextArea reportArea;
    @FXML private BarChart<String, Number> cancellationsBarChart;

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

        try {
            ParkClient.getInstance().setListener(this);

            int month = monthMap.get(monthComboBox.getValue());
            int year = yearComboBox.getValue();

            // Build the first and last day of the chosen month.
            LocalDate fromDate = LocalDate.of(year, month, 1);
            LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());

            CancellationsReportRequest request = new CancellationsReportRequest(fromDate, toDate);
            ParkClient.getInstance().sendToServer(new Message("GET_CANCELLATIONS_REPORT", request));

        } catch (Exception e) {
            reportArea.setText("Error sending request: " + e.getMessage());
        }
    }

    @Override
    public void onCancellationsReportResult(ArrayList<CancellationsReportResult> results) {
        Platform.runLater(() -> {
            cancellationsBarChart.getData().clear();

            // One bar series for traveler cancellations, one for expired bookings.
            XYChart.Series<String, Number> cancelledSeries = new XYChart.Series<>();
            cancelledSeries.setName("Cancelled by Traveler");

            XYChart.Series<String, Number> expiredSeries = new XYChart.Series<>();
            expiredSeries.setName("Expired (no confirmation)");

            StringBuilder sb = new StringBuilder("Cancellations Report\n\n");

            if (results == null || results.isEmpty()) {
                sb.append("No data found for the selected period.");
            } else {
                // Totals for the whole region (all parks together).
                int totalCancelled = 0;
                int totalExpired = 0;

                for (CancellationsReportResult r : results) {
                    cancelledSeries.getData().add(
                        new XYChart.Data<>(r.getParkName(), r.getCancelledCount()));
                    expiredSeries.getData().add(
                        new XYChart.Data<>(r.getParkName(), r.getExpiredCount()));

                    sb.append("Park: ").append(r.getParkName()).append("\n")
                      .append("  Cancelled by traveler: ").append(r.getCancelledCount()).append("\n")
                      .append("  Expired without cancellation: ").append(r.getExpiredCount()).append("\n")
                      .append("  Total bookings: ").append(r.getTotalBookings()).append("\n")
                      .append("  Cancellation rate: ")
                      .append(String.format("%.1f", r.getCancellationRate())).append("%\n\n");

                    totalCancelled += r.getCancelledCount();
                    totalExpired += r.getExpiredCount();
                }

                sb.append("--- Whole Region ---\n")
                  .append("Total cancelled: ").append(totalCancelled).append("\n")
                  .append("Total expired: ").append(totalExpired).append("\n");
            }

            cancellationsBarChart.getData().add(cancelledSeries);
            cancellationsBarChart.getData().add(expiredSeries);
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
