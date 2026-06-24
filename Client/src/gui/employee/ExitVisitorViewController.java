package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Employee;
import common.Message;
import common.Order;
import common.ExitRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ExitVisitorViewController {

    @FXML private TextField txtBookingId;
    @FXML private TextField txtVisitorsLeaving;
    @FXML private Label lblCurrentVisitors;
    @FXML private Label lblStatus;

    private Employee employee;
    private ServerResponseListener liveVisitorsListener;

    public void setEmployee(Employee employee) {
        this.employee = employee;
        registerLiveVisitorsListener();
        requestCurrentVisitors();
    }

    private void registerLiveVisitorsListener() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        liveVisitorsListener = new ServerResponseListener() {
            @Override
            public void onParkVisitorsResult(int currentVisitors) {
                Platform.runLater(() -> updateCurrentVisitors(currentVisitors));
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        };
        client.setNotificationListener(liveVisitorsListener);
    }

    private void requestCurrentVisitors() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(liveVisitorsListener);
        try {
            client.sendToServer(new Message("GET_PARK_CURRENT_VISITORS", employee.getParkId()));
        } catch (Exception e) {
            showStatus("Failed to load live visitor count.", true);
        }
    }

    private void updateCurrentVisitors(int currentVisitors) {
        lblCurrentVisitors.setText("Current Visitors: " + currentVisitors);
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        String bookingId = txtBookingId.getText().trim();
        String visitorsStr = txtVisitorsLeaving.getText().trim();

        if (bookingId.isEmpty() || visitorsStr.isEmpty()) {
            showStatus("Please fill in all fields.", true);
            return;
        }

        int visitorsLeaving;
        try {
            visitorsLeaving = Integer.parseInt(visitorsStr);
            if (visitorsLeaving < 1) {
                showStatus("Number of visitors must be at least 1.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Number of visitors must be a number.", true);
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showStatus("Not connected to server.", true);
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override
            public void onCheckOutResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        showStatus("Exit recorded successfully.", false);
                        txtBookingId.clear();
                        txtVisitorsLeaving.clear();
                        requestCurrentVisitors();
                    } else {
                        showStatus("Exit failed. Please check the booking ID.", true);
                    }
                });
            }
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {
                Platform.runLater(() -> showStatus(msg, true));
            }
        });

        try {
            ExitRequest request = new ExitRequest(bookingId, visitorsLeaving, employee.getParkId());
            client.sendToServer(new Message("CHECK_OUT_VISITOR", request));
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleBack(ActionEvent event) throws Exception {
        clearLiveVisitorsListener();
        Stage currentStage = (Stage) txtBookingId.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/BookingManagementView.fxml"));
        Parent root = loader.load();
        BookingManagementViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Booking Management");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        WindowUtil.showMaximized(stage);
    }

 
    private void showStatus(String message, boolean isError) {
        lblStatus.setText(message);
        lblStatus.getStyleClass().removeAll("msg-error", "msg-success");
        lblStatus.getStyleClass().add(isError ? "msg-error" : "msg-success");
        lblStatus.setVisible(!message.isEmpty());
        lblStatus.setManaged(!message.isEmpty());
    }

    private void clearLiveVisitorsListener() {
        ParkClient client = ParkClient.getInstance();
        if (client != null) {
            client.clearNotificationListener(liveVisitorsListener);
        }
    }
}
