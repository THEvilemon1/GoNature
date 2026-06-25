package gui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import common.Booking;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

class VisitorBookingsViewHelper {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    interface BookingActionHandler {
        void editBooking(Booking booking);
        void cancelBooking(Booking booking);
        void confirmBooking(Booking booking);
        void exitBooking(Booking booking);
    }

    private final VBox bookingsList;
    private final Label lblBookingsMessage;
    private final VisitorBookingFormHelper formHelper;
    private final BookingActionHandler actionHandler;

    VisitorBookingsViewHelper(VBox bookingsList, Label lblBookingsMessage,
                              VisitorBookingFormHelper formHelper, BookingActionHandler actionHandler) {
        this.bookingsList = bookingsList;
        this.lblBookingsMessage = lblBookingsMessage;
        this.formHelper = formHelper;
        this.actionHandler = actionHandler;
    }

    void render(ArrayList<Booking> bookings) {
        bookingsList.getChildren().clear();
        if (bookings == null || bookings.isEmpty()) {
            showMessage("No bookings yet.", false);
            return;
        }

        hideMessage();
        VBox waiting = createSection("Waiting and action needed", "Bookings here are waiting for availability or your confirmation.");
        VBox confirmed = createSection("Confirmed visits", "Approved reservations that can still be edited or cancelled before check-in.");
        VBox active = createSection("Active visits", "You are currently inside the park. Use Exit when you leave.");
        VBox unavailable = createSection("Completed / unavailable", "Final or cancelled bookings.");

        int waitingCount = 0;
        int confirmedCount = 0;
        int activeCount = 0;
        int unavailableCount = 0;

        for (Booking booking : bookings) {
            if (isWaitingStatus(booking.getStatus())) {
                waiting.getChildren().add(createRow(booking));
                waitingCount++;
            } else if (Booking.STATUS_CONFIRMED.equals(booking.getStatus())) {
                confirmed.getChildren().add(createRow(booking));
                confirmedCount++;
            } else if (Booking.STATUS_CHECKED_IN.equals(booking.getStatus())) {
                active.getChildren().add(createRow(booking));
                activeCount++;
            } else {
                unavailable.getChildren().add(createRow(booking));
                unavailableCount++;
            }
        }

        if (waitingCount > 0) bookingsList.getChildren().add(waiting);
        if (confirmedCount > 0) bookingsList.getChildren().add(confirmed);
        if (activeCount > 0) bookingsList.getChildren().add(active);
        if (unavailableCount > 0) bookingsList.getChildren().add(unavailable);
    }

    void showMessage(String message, boolean error) {
        lblBookingsMessage.setText(message);
        lblBookingsMessage.getStyleClass().removeAll("msg-error", "msg-success");
        lblBookingsMessage.getStyleClass().add(error ? "msg-error" : "msg-success");
        lblBookingsMessage.setVisible(true);
    }

    void hideMessage() {
        lblBookingsMessage.setText("");
        lblBookingsMessage.setVisible(false);
    }

    private VBox createSection(String titleText, String descriptionText) {
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

    private Node createRow(Booking booking) {
        VBox row = new VBox(8);
        row.getStyleClass().add("booking-row");
        boolean checkedIn = Booking.STATUS_CHECKED_IN.equals(booking.getStatus());
        boolean locked = !isEditable(booking);
        if (locked && !checkedIn) {
            row.getStyleClass().add("booking-row-locked");
        }

        Label title = new Label(formHelper.getParkName(booking.getParkId()));
        title.getStyleClass().add("booking-row-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label status = new Label(getStatusLabel(booking.getStatus()));
        status.getStyleClass().addAll("booking-status-badge", getStatusStyleClass(booking.getStatus()));

        HBox header = new HBox(10, title, spacer, status);
        header.getStyleClass().add("booking-row-header");

        Label bookingId = new Label("Booking ID: " + booking.getBookingId());
        bookingId.getStyleClass().add("booking-row-id");

        Label details = new Label(booking.getVisitorTime().format(DATE_TIME_FORMAT)
            + " | Visitors: " + booking.getNumberOfVisitors()
            + " | Price: " + String.format("%.2f", booking.getPrice()) + " ILS");
        details.getStyleClass().add("booking-row-details");

        Label description = new Label(getStatusDescription(booking.getStatus()));
        description.getStyleClass().add("booking-row-description");
        description.setWrapText(true);

        HBox actions = new HBox(8);
        if (checkedIn) {
            Button exit = new Button("Exit");
            exit.getStyleClass().add("small-action-btn");
            exit.setOnAction(e -> actionHandler.exitBooking(booking));
            actions.getChildren().add(exit);
        } else {
            if (requiresConfirmation(booking)) {
                Button confirm = new Button("Confirm Arrival");
                confirm.getStyleClass().add("small-action-btn");
                confirm.setOnAction(e -> actionHandler.confirmBooking(booking));
                actions.getChildren().add(confirm);
            }

            Button edit = new Button("Edit");
            edit.getStyleClass().add("small-action-btn");
            edit.setDisable(locked || requiresConfirmation(booking));
            edit.setOnAction(e -> actionHandler.editBooking(booking));

            Button cancel = new Button("Cancel Booking");
            cancel.getStyleClass().add("small-danger-btn");
            cancel.setDisable(locked);
            cancel.setOnAction(e -> actionHandler.cancelBooking(booking));

            actions.getChildren().addAll(edit, cancel);
        }
        row.getChildren().addAll(header, bookingId, details, description, actions);
        return row;
    }

    private boolean isWaitingStatus(String status) {
        return Booking.STATUS_PENDING.equals(status)
            || Booking.STATUS_WAITING_LIST.equals(status)
            || Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)
            || Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status);
    }

    private boolean requiresConfirmation(Booking booking) {
        return Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(booking.getStatus())
            || Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(booking.getStatus());
    }

    private boolean isEditable(Booking booking) {
        String status = booking.getStatus();
        return Booking.STATUS_PENDING.equals(status)
            || Booking.STATUS_WAITING_LIST.equals(status)
            || Booking.STATUS_CONFIRMED.equals(status)
            || Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)
            || Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status);
    }

    private String getStatusLabel(String status) {
        if (Booking.STATUS_PENDING.equals(status)) return "Pending";
        if (Booking.STATUS_WAITING_LIST.equals(status)) return "Waiting list";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "Confirmed";
        if (Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)) return "Pending confirmation";
        if (Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status)) return "Pending confirmation";
        if (Booking.STATUS_CANCELLED.equals(status)) return "Cancelled";
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "Checked in";
        if (Booking.STATUS_CHECKED_OUT.equals(status)) return "Checked out";
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) return "System cancelled";
        return status == null || status.isBlank() ? "Unknown" : status;
    }

    private String getStatusDescription(String status) {
        if (Booking.STATUS_PENDING.equals(status)) return "Waiting for park approval. You can still edit or cancel this booking.";
        if (Booking.STATUS_WAITING_LIST.equals(status)) return "You are in the waiting list until a matching spot becomes available.";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "Your visit is approved. You can still edit or cancel before check-in.";
        if (Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)) return "A spot opened for you. Confirm within one hour or it will be cancelled.";
        if (Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status)) return "Please confirm that you are coming, or the booking may be cancelled.";
        if (Booking.STATUS_CANCELLED.equals(status)) return "This booking was cancelled and can no longer be changed.";
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "Your visit is active. Use Exit when you leave the park.";
        if (Booking.STATUS_CHECKED_OUT.equals(status)) return "This visit has ended and is kept for your records.";
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) return "This booking was cancelled automatically by the system.";
        return "This booking status is not available for changes.";
    }

    private String getStatusStyleClass(String status) {
        if (Booking.STATUS_PENDING.equals(status)) return "status-pending";
        if (Booking.STATUS_WAITING_LIST.equals(status)) return "status-pending";
        if (Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(status)) return "status-pending";
        if (Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(status)) return "status-pending";
        if (Booking.STATUS_CONFIRMED.equals(status)) return "status-confirmed";
        if (Booking.STATUS_CANCELLED.equals(status)) return "status-cancelled";
        if (Booking.STATUS_CHECKED_IN.equals(status)) return "status-checked-in";
        if (Booking.STATUS_CHECKED_OUT.equals(status)) return "status-checked-out";
        if (Booking.STATUS_SYSTEM_CANCEL.equals(status)) return "status-system-cancel";
        return "status-unknown";
    }
}
