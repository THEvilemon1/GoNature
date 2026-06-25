package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ParkVisitorsReportViewController implements ServerResponseListener {
	
	private Employee employee;

	public void setEmployee(Employee employee) {
	    this.employee = employee;
	} 
	
    @FXML private ComboBox<String> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private TextArea reportArea;
    @FXML private PieChart visitorsPieChart;
    @FXML private Button btnSendReport;

    private final Map<String, Integer> monthMap = new HashMap<>();
    private ParkReportRequest lastGeneratedRequest;
    private ParkVisitorsReportResult lastGeneratedResult;

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

        monthComboBox.setValue("June");
        yearComboBox.setValue(2026);
        btnSendReport.setDisable(true);
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        if (monthComboBox.getValue() == null || yearComboBox.getValue() == null) {
            reportArea.setText("Please select month and year.");
            return;
        }

        try {
            if (employee == null) {
                reportArea.setText("Employee details are missing.");
                return;
            }
            if (ParkClient.getInstance() == null || !ParkClient.getInstance().isConnected()) {
                reportArea.setText("Not connected to server.");
                return;
            }

            ParkClient.getInstance().setListener(this);
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

            ParkClient.getInstance().sendToServer(
                    new Message("PARK_VISITORS_REPORT", request)
            );

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
        if (lastGeneratedResult.getTotalVisitors() <= 0) {
            btnSendReport.setDisable(true);
            reportArea.setText(reportArea.getText() + "\n\nCannot send an empty report.");
            return;
        }

        try {
            if (ParkClient.getInstance() == null || !ParkClient.getInstance().isConnected()) {
                reportArea.setText("Not connected to server.");
                return;
            }

            ParkClient.getInstance().setListener(this);
            btnSendReport.setDisable(true);
            ParkClient.getInstance().sendToServer(
                    new Message("SUBMIT_PARK_VISITORS_REPORT", lastGeneratedRequest)
            );
        } catch (Exception e) {
            btnSendReport.setDisable(false);
            reportArea.setText("Error sending report: " + e.getMessage());
        }
    }

    @Override
    public void onParkVisitorsReportResult(ParkVisitorsReportResult result) {
        Platform.runLater(() -> {
            int individual = result.getIndividualVisitors();
            int organized = result.getOrganizedVisitors();
            int total = result.getTotalVisitors();
            lastGeneratedResult = result;

            reportArea.setText(
                    "Visitors Report\n\n" +
                    "Individual Visitors: " + individual + "\n" +
                    "Organized Groups: " + organized + "\n" +
                    "Total Visitors: " + total
            );

            visitorsPieChart.getData().clear();
            visitorsPieChart.getData().add(new PieChart.Data("Individual Visitors", individual));
            visitorsPieChart.getData().add(new PieChart.Data("Organized Groups", organized));
            btnSendReport.setDisable(total <= 0);
            if (total <= 0) {
                reportArea.setText(reportArea.getText() +
                        "\n\nNo bookings or walk-in visitors found for the selected period.");
            }
        });
    }

    @Override
    public void onParkVisitorsReportSubmitResult(boolean success) {
        Platform.runLater(() -> {
            if (success) {
                reportArea.setText(reportArea.getText() + "\n\nReport sent to Department Manager.");
            } else {
                reportArea.setText(reportArea.getText() + "\n\nReport was not sent because it has no visitors.");
            }
            btnSendReport.setDisable(success || lastGeneratedResult == null ||
                    lastGeneratedResult.getTotalVisitors() <= 0);
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
