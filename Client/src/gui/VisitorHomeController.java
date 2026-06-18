package gui;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import client.ParkClient;
import client.ServerResponseListener;
import client.SessionManager;
import common.Booking;
import common.Message;
import common.Order;
import common.VisitorLoginResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VisitorHomeController implements ServerResponseListener {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
    @FXML private Spinner<Integer> spnVisitors;
    @FXML private ComboBox<ParkOption> cmbPark;
    @FXML private DatePicker dateVisit;
    @FXML private ComboBox<String> cmbTime;
    @FXML private Button btnSubmitBooking;

    private VisitorLoginResult currentUser;
    private Booking editingBooking;
    private final Map<Integer, Integer> pricesByParkId = new HashMap<>();

    @FXML
    private void initialize() {
        spnVisitors.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 1));
        spnVisitors.getEditor().textProperty().addListener((obs, oldValue, newValue) -> clampVisitorEditor());
        spnVisitors.valueProperty().addListener((obs, oldValue, newValue) -> updatePriceSummary());

        cmbPark.getItems().setAll(
            new ParkOption(1, "Banias Nature Reserve"),
            new ParkOption(2, "Tel Dan Nature Reserve"),
            new ParkOption(3, "Hula Nature Reserve"),
            new ParkOption(4, "Achziv National Park")
        );
        cmbPark.valueProperty().addListener((obs, oldValue, newValue) -> updatePriceSummary());

        dateVisit.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dateVisit.valueProperty().addListener((obs, oldValue, newValue) -> refreshTimeOptions());
        dateVisit.setValue(LocalDate.now());
        refreshTimeOptions();
        updatePriceSummary();
    }

    public void loadVisitor(VisitorLoginResult result) {
        this.currentUser = result;
        ParkClient client = ParkClient.getInstance();
        if (client != null) {
            client.setListener(this);
            requestParkPrices();
        }

        lblVisitorDetails.setText(
            "Traveler ID: " + result.getTravelerId()
            + "   -   National ID: " + result.getNationalId()
        );

        setupWindowCloseHandler();
    }

    private void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) viewMain.getScene().getWindow();
                if (stage != null) {
                    stage.setOnCloseRequest(e -> handleLogout());
                }
            } catch (Exception e) {
                System.out.println("Could not set up window close handler: " + e.getMessage());
            }
        });
    }

    private void handleLogout() {
        if (currentUser != null) {
            ParkClient client = ParkClient.getInstance();
            if (client != null && client.isConnected()) {
                try {
                    client.sendToServer(new Message("TRAVELER_LOGOUT", currentUser.getTravelerId()));
                    System.out.println("[VisitorHomeController] Sent logout message for: " + currentUser.getTravelerId());
                } catch (Exception e) {
                    System.out.println("[VisitorHomeController] Error sending logout: " + e.getMessage());
                }
            }
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
        lblDetailTitle.setText("Book a Visit");
        showBookingForm(null);
        showDetail();
    }

    @FXML
    private void handleSecondaryAction() {
        lblDetailTitle.setText("My Bookings");
        showBookingsList();
        requestTravelerBookings();
        showDetail();
    }

    @FXML
    private void handleBack() {
        showMain();
    }

    @FXML
    private void handleCancelForm() {
        resetForm();
        showMain();
    }

    @FXML
    private void handleSubmitBooking() {
        try {
            Booking booking = buildBookingFromForm();
            ParkClient client = ParkClient.getInstance();
            if (client == null || !client.isConnected()) {
                showBookingMessage("Client is not connected to the server.", true);
                return;
            }

            String command = editingBooking == null ? "CREATE_BOOKING" : "UPDATE_BOOKING";
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
        Platform.runLater(() -> renderBookings(bookings));
    }

    @Override
    public void onParkPricesResult(Map<Integer, Integer> pricesByParkId) {
        Platform.runLater(() -> {
            this.pricesByParkId.clear();
            if (pricesByParkId != null) {
                this.pricesByParkId.putAll(pricesByParkId);
            }
            updatePriceSummary();
        });
    }

    @Override
    public void onCreateBookingResult(Booking booking) {
        Platform.runLater(() -> {
            btnSubmitBooking.setDisable(false);
            resetForm();
            lblDetailTitle.setText("My Bookings");
            showBookingsList();
            if (booking != null && Booking.STATUS_WAITING_LIST.equals(booking.getStatus())) {
                showBookingsMessage("No spots available. You are in the waiting list.", false);
            } else {
                showBookingsMessage("Booking confirmed.", false);
            }
            requestTravelerBookings();
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

    private Booking buildBookingFromForm() {
        if (currentUser == null) {
            throw new IllegalArgumentException("No traveler is logged in.");
        }
        ParkOption park = cmbPark.getValue();
        if (park == null) {
            throw new IllegalArgumentException("Please choose a park.");
        }
        LocalDate date = dateVisit.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Please choose a date.");
        }
        String timeText = cmbTime.getValue();
        if (timeText == null || timeText.isBlank()) {
            throw new IllegalArgumentException("Please choose a time.");
        }

        int visitors = getVisitorCount();
        LocalDateTime visitorTime = LocalDateTime.of(date, LocalTime.parse(timeText));
        if (!visitorTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time must be in the future.");
        }

        String bookingId = editingBooking == null ? null : editingBooking.getBookingId();
        return new Booking(bookingId, currentUser.getTravelerId(), park.getId(), visitors,
            visitorTime, Booking.STATUS_PENDING, false, 0);
    }

    private int getVisitorCount() {
        clampVisitorEditor();
        return spnVisitors.getValue();
    }

    private void clampVisitorEditor() {
        if (spnVisitors.getValueFactory() == null) {
            return;
        }
        String text = spnVisitors.getEditor().getText();
        try {
            int value = Integer.parseInt(text);
            if (value < 1) value = 1;
            if (value > 15) value = 15;
            spnVisitors.getValueFactory().setValue(value);
            if (!String.valueOf(value).equals(text)) {
                spnVisitors.getEditor().setText(String.valueOf(value));
            }
        } catch (NumberFormatException e) {
            spnVisitors.getValueFactory().setValue(1);
            spnVisitors.getEditor().setText("1");
        }
        updatePriceSummary();
    }

    private void updatePriceSummary() {
        if (lblSelectedParkPrice == null || lblPricePerPerson == null || lblTotalPrice == null) {
            return;
        }

        ParkOption park = cmbPark == null ? null : cmbPark.getValue();
        if (park == null) {
            lblSelectedParkPrice.setText("Select a park to see its price per person.");
            lblPricePerPerson.setText("Price per person: --");
            lblTotalPrice.setText("Total: --");
            return;
        }

        Integer pricePerPerson = pricesByParkId.get(park.getId());
        if (pricePerPerson == null) {
            lblSelectedParkPrice.setText("Price for " + park.getName() + " is not loaded yet.");
            lblPricePerPerson.setText("Price per person: --");
            lblTotalPrice.setText("Total: --");
            return;
        }

        int visitors = spnVisitors == null || spnVisitors.getValue() == null ? 1 : spnVisitors.getValue();
        int totalPrice = pricePerPerson * visitors;

        lblSelectedParkPrice.setText(park.getName() + ": " + pricePerPerson + " ILS per person.");
        lblPricePerPerson.setText("Price per person: " + pricePerPerson + " ILS");
        lblTotalPrice.setText("Total: " + totalPrice + " ILS");
    }

    private void refreshTimeOptions() {
        String selected = cmbTime.getValue();
        cmbTime.getItems().clear();
        LocalDate selectedDate = dateVisit.getValue();
        if (selectedDate == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int hour = 8; hour <= 21; hour++) {
            LocalTime time = LocalTime.of(hour, 0);
            LocalDateTime slot = LocalDateTime.of(selectedDate, time);
            if (slot.isAfter(now)) {
                cmbTime.getItems().add(time.toString());
            }
        }

        if (selected != null && cmbTime.getItems().contains(selected)) {
            cmbTime.setValue(selected);
        } else if (!cmbTime.getItems().isEmpty()) {
            cmbTime.setValue(cmbTime.getItems().get(0));
        }
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

    private void requestParkPrices() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            client.sendToServer(new Message("GET_PARK_PRICES", null));
        } catch (IOException e) {
            showBookingMessage("Failed to load park prices: " + e.getMessage(), true);
        }
    }

    private void renderBookings(ArrayList<Booking> bookings) {
        bookingsList.getChildren().clear();
        if (bookings == null || bookings.isEmpty()) {
            showBookingsMessage("No bookings yet.", false);
            return;
        }

        hideBookingsMessage();
        VBox pendingSection = createBookingsSection("Waiting list",
            "You are in the waiting list. If another visitor cancels and there is enough room, your booking will be confirmed automatically.");
        VBox confirmedSection = createBookingsSection("Confirmed visits",
            "Approved reservations that can still be edited or cancelled before check-in.");
        VBox unavailableSection = createBookingsSection("Completed / unavailable",
            "Bookings in this section are locked because their visit state is final or already in progress.");

        int pendingCount = 0;
        int confirmedCount = 0;
        int unavailableCount = 0;

        for (Booking booking : bookings) {
            Node row = createBookingRow(booking);
            if (Booking.STATUS_PENDING.equals(booking.getStatus()) || Booking.STATUS_WAITING_LIST.equals(booking.getStatus())) {
                pendingSection.getChildren().add(row);
                pendingCount++;
            } else if (Booking.STATUS_CONFIRMED.equals(booking.getStatus())) {
                confirmedSection.getChildren().add(row);
                confirmedCount++;
            } else {
                unavailableSection.getChildren().add(row);
                unavailableCount++;
            }
        }

        if (pendingCount + confirmedCount + unavailableCount == 0) {
            showBookingsMessage("No bookings yet.", false);
            return;
        }

        if (pendingCount > 0) {
            bookingsList.getChildren().add(pendingSection);
        }
        if (confirmedCount > 0) {
            bookingsList.getChildren().add(confirmedSection);
        }
        if (unavailableCount > 0) {
            bookingsList.getChildren().add(unavailableSection);
        }
    }

    private VBox createBookingsSection(String titleText, String descriptionText) {
        VBox section = new VBox(8);
        section.getStyleClass().add("booking-section");

        Label title = new Label(titleText);
        title.getStyleClass().add("booking-section-title");

        Label description = new Label(descriptionText);
        description.getStyleClass().add("booking-section-desc");
        description.setWrapText(true);

        section.getChildren().addAll(title, description);
        return section;
    }

    private Node createBookingRow(Booking booking) {
        VBox row = new VBox(8);
        row.getStyleClass().add("booking-row");
        boolean editable = isEditableBooking(booking);
        boolean locked = !editable;
        if (locked) {
            row.getStyleClass().add("booking-row-locked");
        }

        HBox header = new HBox(10);
        header.getStyleClass().add("booking-row-header");

        Label title = new Label(getParkName(booking.getParkId()));
        title.getStyleClass().add("booking-row-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label status = new Label(getStatusLabel(booking.getStatus()));
        status.getStyleClass().addAll("booking-status-badge", getStatusStyleClass(booking.getStatus()));

        header.getChildren().addAll(title, spacer, status);

        Label details = new Label(
            booking.getVisitorTime().format(DATE_TIME_FORMAT)
            + " | Visitors: " + booking.getNumberOfVisitors()
            + " | Price: " + booking.getPrice() + " ILS"
        );
        details.getStyleClass().add("booking-row-details");

        Label description = new Label(getStatusDescription(booking.getStatus()));
        description.getStyleClass().add("booking-row-description");
        description.setWrapText(true);

        Button edit = new Button("Edit");
        edit.getStyleClass().add("small-action-btn");
        edit.setDisable(locked);
        edit.setOnAction(e -> {
            lblDetailTitle.setText("Edit Booking");
            showBookingForm(booking);
        });

        Button cancel = new Button("Cancel Booking");
        cancel.getStyleClass().add("small-danger-btn");
        cancel.setDisable(locked);
        cancel.setOnAction(e -> confirmAndCancelBooking(booking));

        HBox actions = new HBox(8, edit, cancel);
        row.getChildren().addAll(header, details, description, actions);
        return row;
    }

    private boolean isEditableBooking(Booking booking) {
        String status = booking.getStatus();
        return Booking.STATUS_PENDING.equals(status)
            || Booking.STATUS_WAITING_LIST.equals(status)
            || Booking.STATUS_CONFIRMED.equals(status);
    }

    private String getStatusLabel(String status) {
        if (Booking.STATUS_PENDING.equals(status)) return "Pending";
        if (Booking.STATUS_WAITING_LIST.equals(status)) return "Waiting list";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "Confirmed";
        if (Booking.STATUS_CANCELLED.equals(status)) return "Cancelled";
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "Checked in";
        if (Booking.STATUS_CHECKED_OUT.equals(status)) return "Checked out";
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) return "System cancelled";
        return status == null || status.isBlank() ? "Unknown" : status;
    }

    private String getStatusDescription(String status) {
        if (Booking.STATUS_PENDING.equals(status)) {
            return "Waiting for park approval. You can still edit or cancel this booking.";
        }
        if (Booking.STATUS_WAITING_LIST.equals(status)) {
            return "You are in the waiting list. You will be confirmed automatically if enough spots open.";
        }
        if (Booking.STATUS_CONFIRMED.equals(status)) {
            return "Your visit is approved. You can still edit or cancel before check-in.";
        }
        if (Booking.STATUS_CANCELLED.equals(status)) {
            return "This booking was cancelled and can no longer be changed.";
        }
        if (Booking.STATUS_CHECKED_IN.equals(status)) {
            return "This visit is already active and cannot be changed.";
        }
        if (Booking.STATUS_CHECKED_OUT.equals(status)) {
            return "This visit has ended and is kept for your records.";
        }
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) {
            return "This booking was cancelled automatically by the system.";
        }
        return "This booking status is not available for changes.";
    }

    private String getStatusStyleClass(String status) {
        if (Booking.STATUS_PENDING.equals(status)) return "status-pending";
    if (Booking.STATUS_WAITING_LIST.equals(status)) return "status-pending";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "status-confirmed";
        if (Booking.STATUS_CANCELLED.equals(status)) return "status-cancelled";
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "status-checked-in";
        if (Booking.STATUS_CHECKED_OUT.equals(status)) return "status-checked-out";
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) return "status-system-cancel";
        return "status-unknown";
    }

    private void confirmAndCancelBooking(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Booking");
        alert.setHeaderText("Cancel this booking?");
        alert.setContentText(getParkName(booking.getParkId()) + " on " + booking.getVisitorTime().format(DATE_TIME_FORMAT));
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

    private void showBookingForm(Booking booking) {
        editingBooking = booking;
        bookingForm.setVisible(true);
        bookingForm.setManaged(true);
        bookingsListView.setVisible(false);
        bookingsListView.setManaged(false);
        hideBookingMessage();

        if (booking == null) {
            resetForm();
            btnSubmitBooking.setText("Submit Booking");
            return;
        }

        spnVisitors.getValueFactory().setValue(booking.getNumberOfVisitors());
        cmbPark.setValue(findParkOption(booking.getParkId()));
        dateVisit.setValue(booking.getVisitorTime().toLocalDate());
        refreshTimeOptions();
        cmbTime.setValue(booking.getVisitorTime().toLocalTime().toString());
        btnSubmitBooking.setText("Update Booking");
        updatePriceSummary();
    }

    private void showBookingsList() {
        bookingForm.setVisible(false);
        bookingForm.setManaged(false);
        bookingsListView.setVisible(true);
        bookingsListView.setManaged(true);
    }

    private void resetForm() {
        editingBooking = null;
        btnSubmitBooking.setDisable(false);
        btnSubmitBooking.setText("Submit Booking");
        spnVisitors.getValueFactory().setValue(1);
        cmbPark.setValue(null);
        dateVisit.setValue(LocalDate.now());
        refreshTimeOptions();
        updatePriceSummary();
        hideBookingMessage();
    }

    private ParkOption findParkOption(int parkId) {
        for (ParkOption option : cmbPark.getItems()) {
            if (option.getId() == parkId) {
                return option;
            }
        }
        return null;
    }

    private String getParkName(int parkId) {
        ParkOption option = findParkOption(parkId);
        return option == null ? "Park #" + parkId : option.getName();
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

    private void hideBookingsMessage() {
        lblBookingsMessage.setText("");
        lblBookingsMessage.setVisible(false);
    }

    private static class ParkOption {
        private final int id;
        private final String name;

        ParkOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        int getId() { return id; }
        String getName() { return name; }

        @Override
        public String toString() {
            return name;
        }
    }
}
