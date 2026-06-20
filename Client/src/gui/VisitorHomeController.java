package gui;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import client.ParkClient;
import client.ServerResponseListener;
import client.SessionManager;
import common.Booking;
import common.Message;
import common.Order;
import common.ParkOption;
import common.VisitorLoginResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VisitorHomeController implements ServerResponseListener {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private VBox viewMain;
    @FXML private VBox viewDetail;
    @FXML private VBox bookingForm;
    @FXML private VBox bookingsListView;
    @FXML private VBox bookingsList;
    @FXML private Label lblVisitorDetails;
    @FXML private Label lblDetailTitle;
    @FXML private Label lblBookingMessage;
    @FXML private Label lblBookingsMessage;
    @FXML private Label lblSelectedParkPrice;
    @FXML private Label lblPricePerPerson;
    @FXML private Label lblTotalPrice;
    @FXML private Label lblVisitorHint;
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhoneNumber;
    @FXML private Spinner<Integer> spnVisitors;
    @FXML private ComboBox<ParkOption> cmbPark;
    @FXML private DatePicker dateVisit;
    @FXML private ComboBox<String> cmbTime;
    @FXML private Button btnSubmitBooking;

    private VisitorLoginResult currentUser;
    private Booking pendingWaitlistBooking;
    private Booking pendingConfirmBooking;
    private boolean confirmingFromWaitlist;
    private VisitorBookingFormHelper formHelper;
    private VisitorBookingsViewHelper bookingsHelper;
    private Timer bookingsRefreshTimer;

    @FXML
    private void initialize() {
        formHelper = new VisitorBookingFormHelper(txtName, txtEmail, txtPhoneNumber, spnVisitors, cmbPark,
            dateVisit, cmbTime, lblSelectedParkPrice, lblPricePerPerson, lblTotalPrice, lblVisitorHint);
        formHelper.initialize();

        bookingsHelper = new VisitorBookingsViewHelper(bookingsList, lblBookingsMessage, formHelper,
            new VisitorBookingsViewHelper.BookingActionHandler() {
                @Override
                public void editBooking(Booking booking) {
                    lblDetailTitle.setText("Edit Booking");
                    showBookingForm(booking);
                }

                @Override
                public void cancelBooking(Booking booking) {
                    confirmAndCancelBooking(booking);
                }

                @Override
                public void confirmBooking(Booking booking) {
                    sendConfirmBooking(booking);
                }
            });
    }

    public void loadVisitor(VisitorLoginResult result) {
        currentUser = result;
        formHelper.setCurrentUser(result);
        ParkClient client = ParkClient.getInstance();
        if (client != null) {
            client.setListener(this);
        }
        lblVisitorDetails.setText("Traveler ID: " + result.getTravelerId() + "   -   National ID: " + result.getNationalId());
        setupWindowCloseHandler();
    }

    private void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            Stage stage = (Stage) viewMain.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(e -> handleLogout());
            }
        });
    }

    private void handleLogout() {
        try {
            ParkClient client = ParkClient.getInstance();
            if (currentUser != null && client != null && client.isConnected()) {
                client.sendToServer(new Message("TRAVELER_LOGOUT", currentUser.getTravelerId()));
            }
        } catch (Exception e) {
            System.out.println("[VisitorHomeController] Error sending logout: " + e.getMessage());
        }
        SessionManager.getInstance().logout();
    }

    @FXML
    private void handleLogoutButton(javafx.event.ActionEvent event) {
        handleLogout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/login/LoginPage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/gui/login/LoginPage.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("GoNature - Login");
            stage.setScene(scene);
        } catch (Exception e) {
            System.out.println("Error returning to login window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrimaryAction() {
        stopBookingsAutoRefresh();
        lblDetailTitle.setText("Book a Visit");
        showBookingForm(null);
        showDetail();
    }

    @FXML
    private void handleSecondaryAction() {
        lblDetailTitle.setText("My Bookings");
        showBookingsList();
        requestTravelerBookings();
        startBookingsAutoRefresh();
        showDetail();
    }

    @FXML
    private void handleBack() {
        stopBookingsAutoRefresh();
        showMain();
    }

    @FXML
    private void handleCancelForm() {
        resetForm();
        stopBookingsAutoRefresh();
        showMain();
    }

    @FXML
    private void handleSubmitBooking() {
        try {
            Booking booking = formHelper.buildBooking();
            ParkClient client = ParkClient.getInstance();
            if (client == null || !client.isConnected()) {
                showBookingMessage("Client is not connected to the server.", true);
                return;
            }
            if (currentUser.isGuide() && booking.getNumberOfVisitors() == 1) {
                showBookingMessage("You cannot book a visit with only yourself as a guide.", true);
                return;
            }
            String command = pendingWaitlistBooking != null ? "CREATE_WAITLIST_BOOKING" : (formHelper.isNewBooking() ? "CREATE_BOOKING" : "UPDATE_BOOKING");
            client.sendToServer(new Message(command, booking));
            btnSubmitBooking.setDisable(true);
        } catch (IllegalArgumentException e) {
            showBookingMessage(e.getMessage(), true);
        } catch (IOException e) {
            showBookingMessage("Failed to send booking request: " + e.getMessage(), true);
        }
    }

    @Override
    public void onOrderExistsResult(boolean exists) {}

    @Override
    public void onOrderResult(Order order) {}

    @Override
    public void onUpdateOrderResult(boolean success) {}

    @Override
    public void onTravelerBookingsResult(ArrayList<Booking> bookings) {
        Platform.runLater(() -> bookingsHelper.render(bookings));
    }

    @Override
    public void onParksResult(ArrayList<ParkOption> parks) {
        Platform.runLater(() -> formHelper.setParks(parks));
    }

    @Override
    public void onCreateBookingResult(Booking booking) {
        Platform.runLater(() -> {
            btnSubmitBooking.setDisable(false);
            pendingWaitlistBooking = null;
            resetForm();
            lblDetailTitle.setText("My Bookings");
            showBookingsList();
            if (booking != null && Booking.STATUS_WAITING_LIST.equals(booking.getStatus())) {
                showBookingsMessage("No spots available. You are in the waiting list.", false);
            } else {
                showBookingsMessage("Booking confirmed.", false);
                if (currentUser != null && currentUser.isGuide() && booking != null) {
                    offerAdvancePay(booking);
                }
            }
            requestTravelerBookings();
        });
    }

    @Override
    public void onCreateBookingRequiresWaitlistConfirmation(Booking booking, String message) {
        Platform.runLater(() -> {
            btnSubmitBooking.setDisable(false);
            pendingWaitlistBooking = booking;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Parking is Full");
            alert.setHeaderText("No place is available for this time.");
            alert.setContentText(message + "\n\nChoose 'Yes' to join the waiting list.");
            alert.showAndWait().ifPresent(result -> {
                if (result == javafx.scene.control.ButtonType.OK) {
                    try {
                        ParkClient client = ParkClient.getInstance();
                        if (client != null && client.isConnected()) {
                            client.sendToServer(new Message("CREATE_WAITLIST_BOOKING", booking));
                            btnSubmitBooking.setDisable(true);
                        }
                    } catch (IOException e) {
                        showBookingMessage("Failed to join waiting list: " + e.getMessage(), true);
                    }
                } else {
                    pendingWaitlistBooking = null;
                    showBookingMessage("Booking was not created.", false);
                }
            });
        });
    }

    @Override
    public void onUpdateBookingResult(boolean success) {
        Platform.runLater(() -> {
            btnSubmitBooking.setDisable(false);
            if (success) {
                resetForm();
                lblDetailTitle.setText("My Bookings");
                showBookingsList();
                showBookingsMessage("Booking updated and waiting for approval.", false);
                requestTravelerBookings();
            } else {
                showBookingMessage("Booking could not be updated.", true);
            }
        });
    }

    @Override
    public void onCancelBookingResult(boolean success) {
        Platform.runLater(() -> {
            showBookingsMessage(success ? "Booking cancelled." : "Booking could not be cancelled.", !success);
            requestTravelerBookings();
        });
    }

    @Override
    public void onConfirmBookingResult(boolean success) {
        Platform.runLater(() -> {
            showBookingsMessage(success ? "Booking confirmed successfully." : "Booking confirmation failed.", !success);
            if (success && currentUser != null && currentUser.isGuide() && confirmingFromWaitlist && pendingConfirmBooking != null) {
                offerAdvancePay(pendingConfirmBooking);
            }
            confirmingFromWaitlist = false;
            pendingConfirmBooking = null;
            requestTravelerBookings();
        });
    }

    @Override
    public void onError(String errorMessage) {
        Platform.runLater(() -> {
            btnSubmitBooking.setDisable(false);
            if (bookingForm.isVisible()) {
                showBookingMessage(errorMessage, true);
            } else {
                showBookingsMessage(errorMessage, true);
            }
        });
    }

    private void requestTravelerBookings() {
        if (currentUser == null) {
            return;
        }
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showBookingsMessage("Client is not connected to the server.", true);
            return;
        }
        try {
            client.sendToServer(new Message("GET_TRAVELER_BOOKINGS", currentUser.getTravelerId()));
        } catch (IOException e) {
            showBookingsMessage("Failed to load bookings: " + e.getMessage(), true);
        }
    }

    private void requestParks() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            client.sendToServer(new Message("GET_PARKS", null));
        } catch (IOException e) {
            showBookingMessage("Failed to load parks: " + e.getMessage(), true);
        }
    }

    private void confirmAndCancelBooking(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        alert.setHeaderText("Cancel this booking?");
        alert.setContentText(formHelper.getParkName(booking.getParkId()) + " on " + booking.getVisitorTime().format(DATE_TIME_FORMAT));
        alert.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                sendCancelBooking(booking);
            }
        });
    }

    private void sendCancelBooking(Booking booking) {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showBookingsMessage("Client is not connected to the server.", true);
            return;
        }
        try {
            client.sendToServer(new Message("CANCEL_BOOKING", booking));
        } catch (IOException e) {
            showBookingsMessage("Failed to cancel booking: " + e.getMessage(), true);
        }
    }

    private void offerAdvancePay(Booking booking) {
        int discounted = (int) Math.round(booking.getPrice() * 0.88);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pay in Advance");
        alert.setHeaderText("Save 12% by paying in advance!");
        alert.setContentText("Pay now: " + discounted + " ILS instead of " + booking.getPrice() + " ILS.\n\nWould you like to pay in advance?");
        alert.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                booking.setPrice(discounted);
                booking.setPaid(true);
                try {
                    ParkClient.getInstance().sendToServer(new Message("PAY_IN_ADVANCE", booking));
                } catch (IOException e) {
                    showBookingsMessage("Failed to process payment: " + e.getMessage(), true);
                }
            }
        });
    }

    private void sendConfirmBooking(Booking booking) {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showBookingsMessage("Client is not connected to the server.", true);
            return;
        }
        confirmingFromWaitlist = Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(booking.getStatus());
        pendingConfirmBooking = booking;
        try {
            client.sendToServer(new Message("CONFIRM_BOOKING", new String[] {
                booking.getBookingId(),
                booking.getTravelerId()
            }));
        } catch (IOException e) {
            showBookingsMessage("Failed to confirm booking: " + e.getMessage(), true);
        }
    }

    private void showBookingForm(Booking booking) {
        bookingForm.setVisible(true);
        bookingForm.setManaged(true);
        bookingsListView.setVisible(false);
        bookingsListView.setManaged(false);
        hideBookingMessage();
        formHelper.showBooking(booking);
        btnSubmitBooking.setDisable(false);
        btnSubmitBooking.setText(booking == null ? "Submit Booking" : "Update Booking");
        requestParks();
    }

    private void showBookingsList() {
        bookingForm.setVisible(false);
        bookingForm.setManaged(false);
        bookingsListView.setVisible(true);
        bookingsListView.setManaged(true);
        startBookingsAutoRefresh();
    }

    private void resetForm() {
        pendingWaitlistBooking = null;
        btnSubmitBooking.setDisable(false);
        btnSubmitBooking.setText("Submit Booking");
        formHelper.reset();
        hideBookingMessage();
    }

    private void showDetail() {
        viewMain.setVisible(false);
        viewMain.setManaged(false);
        viewDetail.setVisible(true);
        viewDetail.setManaged(true);
    }

    private void showMain() {
        viewDetail.setVisible(false);
        viewDetail.setManaged(false);
        viewMain.setVisible(true);
        viewMain.setManaged(true);
    }

    private void showBookingMessage(String message, boolean error) {
        lblBookingMessage.setText(message);
        lblBookingMessage.getStyleClass().removeAll("msg-error", "msg-success");
        lblBookingMessage.getStyleClass().add(error ? "msg-error" : "msg-success");
        lblBookingMessage.setVisible(true);
    }

    private void hideBookingMessage() {
        lblBookingMessage.setText("");
        lblBookingMessage.setVisible(false);
    }

    private void showBookingsMessage(String message, boolean error) {
        lblBookingsMessage.setText(message);
        lblBookingsMessage.getStyleClass().removeAll("msg-error", "msg-success");
        lblBookingsMessage.getStyleClass().add(error ? "msg-error" : "msg-success");
        lblBookingsMessage.setVisible(true);
    }

    private void startBookingsAutoRefresh() {
        stopBookingsAutoRefresh();
        bookingsRefreshTimer = new Timer(true);
        bookingsRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (bookingsListView != null && bookingsListView.isVisible()) {
                    requestTravelerBookings();
                }
            }
        }, 15000, 15000);
    }

    private void stopBookingsAutoRefresh() {
        if (bookingsRefreshTimer != null) {
            bookingsRefreshTimer.cancel();
            bookingsRefreshTimer = null;
        }
    }
}
