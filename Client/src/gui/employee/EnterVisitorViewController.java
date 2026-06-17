package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EnterVisitorViewController {

    @FXML private TextField txtBookingId;
    @FXML private Label lblStatus;
    @FXML private Label lblBookingDetails;
    @FXML private Label lblAvailableSpots;
    @FXML private Button btnCheckIn;
    @FXML private VBox detailsBox;

    private Employee employee;
    private Booking foundBooking;

    public void setEmployee(Employee employee) {
        this.employee = employee;
        refreshAvailableSpots();
    }

    private void refreshAvailableSpots() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override
            public void onEffectiveAvailableSpotsResult(int spots) {
                Platform.runLater(() ->
                    lblAvailableSpots.setText("Available Spots: " + spots));
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

    @FXML
    public void handleSearchBooking(ActionEvent event) {
        String bookingId = txtBookingId.getText().trim();
        if (bookingId.isEmpty()) {
            showStatus("Please enter a booking ID.", true);
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showStatus("Not connected to server.", true);
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override
            public void onBookingResult(Booking booking) {
                Platform.runLater(() -> {
                    if (booking == null) {
                        showStatus("Booking not found.", true);
                        btnCheckIn.setVisible(false);
                        detailsBox.setVisible(false);
                        detailsBox.setManaged(false);
                        foundBooking = null;
                        return;
                    }
                    if (!Booking.STATUS_PENDING.equals(booking.getStatus())) {
                        showStatus("This booking cannot be checked in. Status: " + booking.getStatus(), true);
                        btnCheckIn.setVisible(false);
                        detailsBox.setVisible(false);
                        detailsBox.setManaged(false);
                        foundBooking = null;
                        return;
                    }
                    if (employee == null || booking.getParkId() != employee.getParkId()) {
                        showStatus("This booking belongs to another park and cannot be checked in here.", true);
                        btnCheckIn.setVisible(false);
                        detailsBox.setVisible(false);
                        detailsBox.setManaged(false);
                        foundBooking = null;
                        return;
                    }
                    // Check that the booking is for today
                    java.time.LocalDate bookingDate = booking.getVisitorTime().toLocalDate();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    if (!bookingDate.equals(today)) {
                        showStatus("This booking is for " + bookingDate +
                                   ", not today. Cannot check in.", true);
                        btnCheckIn.setVisible(false);
                        detailsBox.setVisible(false);
                        detailsBox.setManaged(false);
                        foundBooking = null;
                        return;
                    }
                    foundBooking = booking;
                    lblBookingDetails.setText(
                        "Booking ID: " + booking.getBookingId() + "\n" +
                        "Visitors: " + booking.getNumberOfVisitors() + "\n" +
                        "Time: " + booking.getVisitorTime() + "\n" +
                        "Price: " + booking.getPrice() + " ILS"
                    );
                    showStatus("Booking found. Click Check In to proceed.", false);
                    btnCheckIn.setVisible(true);
                    detailsBox.setVisible(true);
                    detailsBox.setManaged(true);
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
            client.sendToServer(new Message("GET_BOOKING_BY_ID", bookingId));
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleCheckIn(ActionEvent event) {
        if (foundBooking == null) return;

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showStatus("Not connected to server.", true);
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override
            public void onCheckInResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        showReceipt(foundBooking);
                        lblBookingDetails.setText("");
                        btnCheckIn.setVisible(false);
                        detailsBox.setVisible(false);
                        detailsBox.setManaged(false);
                        txtBookingId.clear();
                        foundBooking = null;
                        refreshAvailableSpots();
                    } else {
                        showStatus("Check-in failed. Please try again.", true);
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
            client.sendToServer(new Message("CHECK_IN_VISITOR", foundBooking));
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleWalkIn(ActionEvent event) throws Exception {
        Stage currentStage = (Stage) txtBookingId.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/WalkInView.fxml"));
        Parent root = loader.load();
        WalkInViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Walk-in Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        WindowUtil.showMaximized(stage);
    }

    @FXML
    public void handleBack(ActionEvent event) throws Exception {
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

    private void showReceipt(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText("Check-in Receipt");
        alert.setContentText(
            "Booking ID: " + booking.getBookingId() + "\n" +
            "Visitors: " + booking.getNumberOfVisitors() + "\n" +
            "Visit Time: " + booking.getVisitorTime() + "\n" +
            "Price: " + booking.getPrice() + " ILS\n" +
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
}
