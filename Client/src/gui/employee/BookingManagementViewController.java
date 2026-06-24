package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Booking;
import common.Employee;
import common.ExitRequest;
import common.Message;
import common.Order;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class BookingManagementViewController implements EmployeeAwareController, Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblCurrentVisitors;
    @FXML private ListView<Booking> lstPending;
    @FXML private ListView<Booking> lstCheckedIn;
    @FXML private VBox detailBox;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailBookingId;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailVisitors;
    @FXML private Label lblDetailTime;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblActionStatus;
    @FXML private Button btnBookingAction;

    private Employee employee;
    private Timer refreshTimer;
    private Booking selectedBooking;
    private boolean syncingSelection;
    private ServerResponseListener liveUpdatesListener;

    private final ObservableList<Booking> pendingItems = FXCollections.observableArrayList();
    private final ObservableList<Booking> checkedInItems = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lstPending.setItems(pendingItems);
        lstCheckedIn.setItems(checkedInItems);
        lstPending.setPlaceholder(new Label("No pending bookings"));
        lstCheckedIn.setPlaceholder(new Label("No checked-in bookings"));
        lstPending.setCellFactory(list -> bookingCell());
        lstCheckedIn.setCellFactory(list -> bookingCell());

        lstPending.getSelectionModel().selectedItemProperty().addListener((obs, oldB, newB) -> {
            if (syncingSelection) return;
            if (newB != null) {
                syncingSelection = true;
                lstCheckedIn.getSelectionModel().clearSelection();
                syncingSelection = false;
                showBookingDetails(newB);
            }
        });
        lstCheckedIn.getSelectionModel().selectedItemProperty().addListener((obs, oldB, newB) -> {
            if (syncingSelection) return;
            if (newB != null) {
                syncingSelection = true;
                lstPending.getSelectionModel().clearSelection();
                syncingSelection = false;
                showBookingDetails(newB);
            }
        });
    }

    private ListCell<Booking> bookingCell() {
        return new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setText(null);
                } else {
                    setText(b.getVisitorTime().toLocalTime().format(TIME_FMT)
                        + "   |   " + b.getNumberOfVisitors() + " visitor(s)");
                }
            }
        };
    }

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");
        registerLiveUpdatesListener();
        refreshData();
        startAutoRefresh();
    }

    private void registerLiveUpdatesListener() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        liveUpdatesListener = new ServerResponseListener() {
            @Override
            public void onParkVisitorsResult(int currentVisitors) {
                Platform.runLater(() -> updateCurrentVisitors(currentVisitors));
            }

            @Override
            public void onTodayBookingsResult(ArrayList<Booking> bookings) {
                Platform.runLater(() -> populateLists(bookings));
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        };

        client.setListener(liveUpdatesListener);
        client.setNotificationListener(liveUpdatesListener);
    }

    // Auto-refresh both the visitor count and the booking lists every 30 seconds
    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        }, 30000, 30000);
    }

    private void refreshData() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(liveUpdatesListener);

        try {
            client.sendToServer(new Message("GET_PARK_CURRENT_VISITORS", employee.getParkId()));
            client.sendToServer(new Message("GET_TODAY_BOOKINGS", employee.getParkId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentVisitors(int currentVisitors) {
        lblCurrentVisitors.setText("Current Visitors: " + currentVisitors);
    }

    private void populateLists(ArrayList<Booking> bookings) {
        int selectedId = selectedBooking != null ? selectedBooking.getBookingId() : -1;

        pendingItems.clear();
        checkedInItems.clear();

        for (Booking b : bookings) {
            if (Booking.STATUS_CHECKED_IN.equals(b.getStatus())) {
                checkedInItems.add(b);
            } else {
                // PENDING and CONFIRMED bookings are waiting to enter
                pendingItems.add(b);
            }
        }

        // Re-select the previously selected booking if it is still listed,
        // so an in-progress action keeps its details visible across refreshes.
        if (selectedId != -1) {
            Booking match = findBooking(selectedId);
            if (match != null) {
                showBookingDetails(match);
            } else {
                clearDetails();
            }
        }
    }

    private Booking findBooking(int bookingId) {
        for (Booking b : pendingItems) {
            if (b.getBookingId() == bookingId) return b;
        }
        for (Booking b : checkedInItems) {
            if (b.getBookingId() == bookingId) return b;
        }
        return null;
    }

    private void showBookingDetails(Booking booking) {
        selectedBooking = booking;

        lblDetailStatus.setText(formatStatus(booking.getStatus()));
        styleStatusBadge(booking.getStatus());
        lblDetailBookingId.setText(String.valueOf(booking.getBookingId()));
        lblDetailName.setText(hasText(booking.getTravelerName()) ? booking.getTravelerName() : "Not provided");
        lblDetailVisitors.setText(booking.getNumberOfVisitors() + " visitor(s)");
        lblDetailTime.setText(booking.getVisitorTime().format(DATE_TIME_FMT));
        lblDetailPrice.setText(String.format("%.2f ILS", booking.getPrice()));

        hideActionStatus();

        if (Booking.STATUS_CHECKED_IN.equals(booking.getStatus())) {
            configureActionButton("Walk Out", "btn-danger", true);
        } else if (Booking.STATUS_CONFIRMED.equals(booking.getStatus())) {
            configureActionButton("Enter", "btn-primary", true);
        } else {
            // PENDING — the visitor has not confirmed yet, so it cannot be entered.
            configureActionButton(null, null, false);
            showActionStatus("This booking is not confirmed yet and cannot be checked in.", true);
        }

        detailBox.setVisible(true);
        detailBox.setManaged(true);
    }

    private void configureActionButton(String text, String styleClass, boolean visible) {
        if (visible) {
            btnBookingAction.setText(text);
            btnBookingAction.getStyleClass().setAll("booking-action-button", styleClass);
        }
        btnBookingAction.setVisible(visible);
        btnBookingAction.setManaged(visible);
        btnBookingAction.setDisable(false);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatStatus(String status) {
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "Checked in";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "Confirmed";
        if (Booking.STATUS_PENDING.equals(status)) return "Pending";
        if (Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)) return "Pending confirmation";
        if (Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status)) return "Pending confirmation";
        return status == null ? "Unknown" : status.replace('_', ' ');
    }

    private void styleStatusBadge(String status) {
        lblDetailStatus.getStyleClass().removeAll(
            "booking-status-pending", "booking-status-confirmed", "booking-status-checked-in");
        if (Booking.STATUS_CHECKED_IN.equals(status)) {
            lblDetailStatus.getStyleClass().add("booking-status-checked-in");
        } else if (Booking.STATUS_CONFIRMED.equals(status)) {
            lblDetailStatus.getStyleClass().add("booking-status-confirmed");
        } else {
            lblDetailStatus.getStyleClass().add("booking-status-pending");
        }
    }

    private void clearDetails() {
        selectedBooking = null;
        detailBox.setVisible(false);
        detailBox.setManaged(false);
        lstPending.getSelectionModel().clearSelection();
        lstCheckedIn.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleBookingAction(ActionEvent event) {
        if (selectedBooking == null) return;
        if (Booking.STATUS_CHECKED_IN.equals(selectedBooking.getStatus())) {
            walkOut(selectedBooking);
        } else if (Booking.STATUS_CONFIRMED.equals(selectedBooking.getStatus())) {
            enter(selectedBooking);
        }
    }

    private void enter(Booking booking) {
        // Same check-in window rule as the Enter Visitor screen:
        // 10 minutes before to 30 minutes after the booked time.
        LocalDate bookingDate = booking.getVisitorTime().toLocalDate();
        if (!bookingDate.equals(LocalDate.now())) {
            showActionStatus("This booking is for " + bookingDate + ", not today. Cannot check in.", true);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = booking.getVisitorTime().minusMinutes(10);
        LocalDateTime windowEnd = booking.getVisitorTime().plusMinutes(30);
        if (now.isBefore(windowStart)) {
            showActionStatus("Too early to check in. Check-in opens at "
                + windowStart.toLocalTime().withSecond(0).withNano(0)
                + " (10 minutes before the booked time).", true);
            return;
        }
        if (now.isAfter(windowEnd)) {
            showActionStatus("Too late to check in. Check-in closed at "
                + windowEnd.toLocalTime().withSecond(0).withNano(0)
                + " (30 minutes after the booked time).", true);
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showActionStatus("Not connected to server.", true);
            return;
        }

        btnBookingAction.setDisable(true);
        client.setListener(new ServerResponseListener() {
            @Override
            public void onCheckInResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        showActionAlert("Visitor checked in.");
                        refreshData();
                    } else {
                        showActionStatus("Check-in failed. Please try again.", true);
                        btnBookingAction.setDisable(false);
                    }
                });
            }
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {
                Platform.runLater(() -> {
                    showActionStatus(msg, true);
                    btnBookingAction.setDisable(false);
                });
            }
        });

        try {
            client.sendToServer(new Message("CHECK_IN_VISITOR", booking));
        } catch (Exception e) {
            showActionStatus("Error: " + e.getMessage(), true);
            btnBookingAction.setDisable(false);
        }
    }

    private void walkOut(Booking booking) {
        int visitorsInside = booking.getVisitorsInside() > 0
            ? booking.getVisitorsInside() : booking.getNumberOfVisitors();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Walk Out");
        confirm.setHeaderText("Walk out this booking?");
        confirm.setContentText("Booking ID: " + booking.getBookingId()
            + "\nVisitors leaving: " + visitorsInside);
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                sendCheckOut(booking, visitorsInside);
            }
        });
    }

    private void sendCheckOut(Booking booking, int visitorsLeaving) {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showActionStatus("Not connected to server.", true);
            return;
        }

        btnBookingAction.setDisable(true);
        client.setListener(new ServerResponseListener() {
            @Override
            public void onCheckOutResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        showActionAlert("Visitor walked out.");
                        refreshData();
                    } else {
                        showActionStatus("Walk out failed. Please try again.", true);
                        btnBookingAction.setDisable(false);
                    }
                });
            }
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {
                Platform.runLater(() -> {
                    showActionStatus(msg, true);
                    btnBookingAction.setDisable(false);
                });
            }
        });

        try {
            ExitRequest request = new ExitRequest(
                String.valueOf(booking.getBookingId()), visitorsLeaving, employee.getParkId());
            client.sendToServer(new Message("CHECK_OUT_VISITOR", request));
        } catch (Exception e) {
            showActionStatus("Error: " + e.getMessage(), true);
            btnBookingAction.setDisable(false);
        }
    }

    private void showActionStatus(String message, boolean isError) {
        lblActionStatus.setText(message);
        lblActionStatus.getStyleClass().removeAll("msg-error", "msg-success");
        lblActionStatus.getStyleClass().add(isError ? "msg-error" : "msg-success");
        lblActionStatus.setVisible(true);
        lblActionStatus.setManaged(true);
    }

    private void hideActionStatus() {
        lblActionStatus.setText("");
        lblActionStatus.setVisible(false);
        lblActionStatus.setManaged(false);
    }

    private void showActionAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    public void handleEnterVisitor(ActionEvent event) throws Exception {
        stopRefresh();
        Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/EnterVisitorView.fxml"));
        Parent root = loader.load();
        EnterVisitorViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Enter Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    @FXML
    public void handleExitVisitor(ActionEvent event) throws Exception {
        stopRefresh();
        Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/ExitVisitorView.fxml"));
        Parent root = loader.load();
        ExitVisitorViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Exit Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    @FXML
    public void handleLogout(ActionEvent event) throws Exception {
        stopRefresh();
        ParkClient client = ParkClient.getInstance();
        if (client != null && client.isConnected()) {
            try {
                client.sendToServer(new Message("EMPLOYEE_LOGOUT", employee.getUsername()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login/LoginPage.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/gui/login/LoginPage.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("GoNature - Login");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    private void stopRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        ParkClient client = ParkClient.getInstance();
        if (client != null) {
            client.clearNotificationListener(liveUpdatesListener);
        }
    }
}
