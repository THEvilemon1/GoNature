package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import common.WalkInRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class WalkInViewController implements Initializable {

    @FXML private Label lblAvailableSpots;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtVisitors;
    @FXML private Label lblStatus;

    private Employee employee;
    private Timer refreshTimer;
    private ServerResponseListener liveSpotsListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setEmployee(Employee employee) {
        this.employee = employee;
        registerLiveSpotsListener();
        refreshAvailableSpots();
        startAutoRefresh();
    }

    private void registerLiveSpotsListener() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        liveSpotsListener = new ServerResponseListener() {
            @Override
            public void onEffectiveAvailableSpotsResult(int spots) {
                Platform.runLater(() -> updateAvailableSpots(spots));
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        };
        client.setNotificationListener(liveSpotsListener);
    }

    // Refresh available spots every 30 seconds automatically
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshAvailableSpots();
            }
        }, 30000, 30000);
    }

    private void refreshAvailableSpots() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override
            public void onEffectiveAvailableSpotsResult(int spots) {
                Platform.runLater(() -> updateAvailableSpots(spots));
            }
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        });

        try {
            client.sendToServer(new Message("GET_EFFECTIVE_AVAILABLE_SPOTS", employee.getParkId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAvailableSpots(int spots) {
        lblAvailableSpots.setText("Available Spots: " + spots);
        lblAvailableSpots.setStyle(
            spots > 0 ? "-fx-text-fill: white;" : "-fx-text-fill: #ffcccc;"
        );
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        String nationalId = txtNationalId.getText().trim();
        String visitorsStr = txtVisitors.getText().trim();

        if (nationalId.isEmpty() || visitorsStr.isEmpty()) {
            showStatus("Please fill in all fields.", true);
            return;
        }

        int visitors;
        try {
            visitors = Integer.parseInt(visitorsStr);
            if (visitors < 1 || visitors > 15) {
                showStatus("Number of visitors must be between 1 and 15.", true);
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
            public void onWalkInResult(Booking booking) {
                Platform.runLater(() -> {
                    if (refreshTimer != null) refreshTimer.cancel();
                    showReceipt(booking);
                    txtNationalId.clear();
                    txtVisitors.clear();
                    refreshAvailableSpots();
                    startAutoRefresh();
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
            WalkInRequest request = new WalkInRequest(employee.getParkId(), visitors, nationalId);
            client.sendToServer(new Message("WALK_IN_VISITOR", request));
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleBack(ActionEvent event) throws Exception {
        if (refreshTimer != null) refreshTimer.cancel();
        clearLiveSpotsListener();

        Stage currentStage = (Stage) txtNationalId.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/BookingManagementView.fxml"));
        Parent root = loader.load();
        BookingManagementViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Enter Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        WindowUtil.showMaximized(stage);
    }

    private void showReceipt(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Walk-in Receipt");
        alert.setHeaderText("Walk-in Successful");
        alert.setContentText(
            "Booking ID: " + booking.getBookingId() + "\n" +
            "Visitors: " + booking.getNumberOfVisitors() + "\n" +
            "Visit Time: " + booking.getVisitorTime() + "\n" +
            "Price: " + String.format("%.2f", booking.getPrice()) + " ILS\n" +
            "Status: CHECKED IN"
        );
        alert.showAndWait();
    }

 
    private void showStatus(String message, boolean isError) {
        lblStatus.setText(message);
        lblStatus.getStyleClass().removeAll("msg-error", "msg-success");
        lblStatus.getStyleClass().add(isError ? "msg-error" : "msg-success");
        lblStatus.setVisible(!message.isEmpty());
        lblStatus.setManaged(!message.isEmpty());
    }

    private void clearLiveSpotsListener() {
        ParkClient client = ParkClient.getInstance();
        if (client != null) {
            client.clearNotificationListener(liveSpotsListener);
        }
    }
}
