package gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import common.Booking;
import common.ParkOption;
import common.VisitorLoginResult;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

// Helper class for managing the visitor booking form, including input validation, price calculation, and form state management.
class VisitorBookingFormHelper {
    private static final LocalTime BOOKING_OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime BOOKING_CLOSE_TIME = LocalTime.of(16, 0);

    private final TextField txtName;
    private final TextField txtEmail;
    private final TextField txtPhoneNumber;
    private final Spinner<Integer> spnVisitors;
    private final ComboBox<ParkOption> cmbPark;
    private final DatePicker dateVisit;
    private final ComboBox<String> cmbTime;
    private final Label lblSelectedParkPrice;
    private final Label lblPricePerPerson;
    private final Label lblTotalPrice;
    private final Label lblVisitorHint;

    private VisitorLoginResult currentUser;
    private Booking editingBooking;

    VisitorBookingFormHelper(TextField txtName, TextField txtEmail, TextField txtPhoneNumber,
                             Spinner<Integer> spnVisitors, ComboBox<ParkOption> cmbPark,
                             DatePicker dateVisit, ComboBox<String> cmbTime,
                             Label lblSelectedParkPrice, Label lblPricePerPerson,
                             Label lblTotalPrice, Label lblVisitorHint) {
        this.txtName = txtName;
        this.txtEmail = txtEmail;
        this.txtPhoneNumber = txtPhoneNumber;
        this.spnVisitors = spnVisitors;
        this.cmbPark = cmbPark;
        this.dateVisit = dateVisit;
        this.cmbTime = cmbTime;
        this.lblSelectedParkPrice = lblSelectedParkPrice;
        this.lblPricePerPerson = lblPricePerPerson;
        this.lblTotalPrice = lblTotalPrice;
        this.lblVisitorHint = lblVisitorHint;
    }

    void initialize() {
        spnVisitors.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1));
        spnVisitors.getEditor().textProperty().addListener((obs, oldValue, newValue) -> clampVisitorEditor());
        spnVisitors.valueProperty().addListener((obs, oldValue, newValue) -> updatePriceSummary());

        cmbPark.valueProperty().addListener((obs, oldValue, newValue) -> updatePriceSummary());

        dateVisit.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || isDateUnavailable(date));
            }
        });

        dateVisit.valueProperty().addListener((obs, oldValue, newValue) -> refreshTimeOptions());
        dateVisit.setValue(firstAvailableBookingDate());
        refreshTimeOptions();
        updatePriceSummary();
    }

    void setCurrentUser(VisitorLoginResult currentUser) {
        this.currentUser = currentUser;
        setupGuideMode();
        updatePriceSummary();
    }

    void setParks(ArrayList<ParkOption> parks) {
        ParkOption selected = cmbPark.getValue();
        cmbPark.getItems().clear();
        if (parks != null) {
            cmbPark.getItems().addAll(parks);
        }
        if (selected != null) {
            cmbPark.setValue(findParkOption(selected.getId()));
        }
        updatePriceSummary();
    }

    Booking buildBooking() {
        if (currentUser == null) {
            throw new IllegalArgumentException("No traveler is logged in.");
        }
        ParkOption park = cmbPark.getValue();
        if (park == null) {
            throw new IllegalArgumentException("Please choose a park.");
        }
        LocalDate date = dateVisit.getValue();
        String timeText = cmbTime.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Please choose a date.");
        }
        if (isDateUnavailable(date)) {
            throw new IllegalArgumentException("Booking is no longer available for today because the park is already closed.");
        }
        if (timeText == null || timeText.isBlank()) {
            throw new IllegalArgumentException("Please choose a time.");
        }

        int visitors = getVisitorCount();
        if (visitors < 1 || visitors > maxVisitors()) {
            throw new IllegalArgumentException("Number of visitors must be between 1 and " + maxVisitors() + ".");
        }

        LocalDateTime visitorTime = LocalDateTime.of(date, LocalTime.parse(timeText));
        if (!visitorTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time must be in the future.");
        }

        double price = calculateTotalPrice(park, visitors);
        int bookingId = editingBooking == null ? 0 : editingBooking.getBookingId();
        return new Booking(bookingId, currentUser.getTravelerId(), requireText(txtName, "Please enter your name."),
            requireText(txtEmail, "Please enter email address."),
            requireText(txtPhoneNumber, "Please enter phone number."), park.getId(), visitors,
            visitorTime, Booking.STATUS_PENDING, false, price);
    }

    void showBooking(Booking booking) {
        editingBooking = booking;
        if (booking == null) {
            reset();
            return;
        }
        txtName.setText(booking.getTravelerName());
        txtEmail.setText(booking.getTravelerEmail());
        txtPhoneNumber.setText(booking.getTravelerPhoneNumber());
        spnVisitors.getValueFactory().setValue(booking.getNumberOfVisitors());
        cmbPark.setValue(findParkOption(booking.getParkId()));
        dateVisit.setValue(booking.getVisitorTime().toLocalDate());
        refreshTimeOptions();
        cmbTime.setValue(booking.getVisitorTime().toLocalTime().toString());
        updatePriceSummary();
    }

    void reset() {
        editingBooking = null;
        txtName.clear();
        txtEmail.clear();
        txtPhoneNumber.clear();
        spnVisitors.getValueFactory().setValue(1);
        cmbPark.setValue(null);
        dateVisit.setValue(firstAvailableBookingDate());
        refreshTimeOptions();
        updatePriceSummary();
    }

    boolean isNewBooking() {
        return editingBooking == null;
    }

    Booking getEditingBooking() {
        return editingBooking;
    }

    String getParkName(int parkId) {
        ParkOption option = findParkOption(parkId);
        return option == null ? "Unknown Park" : option.getName();
    }

    private void setupGuideMode() {
        SpinnerValueFactory<Integer> factory = spnVisitors.getValueFactory();
        if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) factory).setMax(maxVisitors());
        }
        if (currentUser != null && currentUser.isGuide()) {
            lblVisitorHint.setText("Choose between 1 and 16 visitors. As a guide, your own entry is free.");
        } else {
            lblVisitorHint.setText("Choose between 1 and 6 visitors.");
        }
    }

    private int maxVisitors() {
        return currentUser != null && currentUser.isGuide() ? 16 : 6;
    }

    private int getVisitorCount() {
        clampVisitorEditor();
        return spnVisitors.getValue();
    }

    private void clampVisitorEditor() {
        if (spnVisitors.getValueFactory() == null) {
            return;
        }
        int value;
        try {
            value = Integer.parseInt(spnVisitors.getEditor().getText());
        } catch (NumberFormatException e) {
            value = 1;
        }
        value = Math.max(1, Math.min(maxVisitors(), value));
        spnVisitors.getValueFactory().setValue(value);
        spnVisitors.getEditor().setText(String.valueOf(value));
        updatePriceSummary();
    }

    private void updatePriceSummary() {
        ParkOption park = cmbPark.getValue();
        if (park == null) {
            lblSelectedParkPrice.setText("Select a park to see its price per person.");
            lblPricePerPerson.setText("Price per person: --");
            lblTotalPrice.setText("Total: --");
            return;
        }

        double pricePerPerson = park.getPrice();
        double totalPrice = calculateTotalPrice(park, spnVisitors.getValue());
        lblSelectedParkPrice.setText(park.getName() + ": " + String.format("%.2f", pricePerPerson) + " ILS per person.");
        lblPricePerPerson.setText("Price per person: " + String.format("%.2f", pricePerPerson) + " ILS");
        lblTotalPrice.setText("Total: " + String.format("%.2f", totalPrice) + " ILS" + discountText());
    }

    private double calculateTotalPrice(ParkOption park, int visitors) {
        boolean guide = currentUser != null && currentUser.isGuide();
        int billableVisitors = guide ? Math.max(0, visitors - 1) : visitors;
        return park.getPrice() * billableVisitors * discountFactor();
    }

    private double discountFactor() {
        if (currentUser == null || currentUser.isGuide()) {
            return 1.0;
        }
        double factor = 0.85; // 15% digital booking discount always applies
        return currentUser.isClubMember() ? factor * 0.90 : factor;
    }

    private String discountText() {
        if (currentUser != null && currentUser.isGuide()) {
            return " (guide entry excluded)";
        }
        if (currentUser == null) return "";
        String text = "15% digital booking";
        if (currentUser.isClubMember()) text += ", 10% club member";
        return " (" + text + ")";
    }

    private void refreshTimeOptions() {
        String selected = cmbTime.getValue();
        cmbTime.getItems().clear();
        LocalDate selectedDate = dateVisit.getValue();
        if (selectedDate == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (isDateUnavailable(selectedDate)) {
            cmbTime.setValue(null);
            return;
        }

        for (int hour = BOOKING_OPEN_TIME.getHour(); hour <= BOOKING_CLOSE_TIME.getHour(); hour++) {
            LocalTime time = LocalTime.of(hour, 0);
            if (LocalDateTime.of(selectedDate, time).isAfter(now)) {
                cmbTime.getItems().add(time.toString());
            }
        }

        if (selected != null && cmbTime.getItems().contains(selected)) {
            cmbTime.setValue(selected);
        } else if (!cmbTime.getItems().isEmpty()) {
            cmbTime.setValue(cmbTime.getItems().get(0));
        } else {
            cmbTime.setValue(null);
        }
    }

    private boolean isDateUnavailable(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isBefore(today) || (date.isEqual(today) && isTodayClosedForBookings());
    }

    private boolean isTodayClosedForBookings() {
        return !LocalTime.now().isBefore(BOOKING_CLOSE_TIME);
    }

    private LocalDate firstAvailableBookingDate() {
        LocalDate today = LocalDate.now();
        return isTodayClosedForBookings() ? today.plusDays(1) : today;
    }

    private ParkOption findParkOption(int parkId) {
        for (ParkOption option : cmbPark.getItems()) {
            if (option.getId() == parkId) {
                return option;
            }
        }
        return null;
    }

    private String requireText(TextField field, String message) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
