package gui;

import common.VisitorLoginResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class VisitorHomeController {

    @FXML private Label lblTitle;
    @FXML private Label lblVisitorDetails;
    @FXML private Label lblMode;
    @FXML private Button btnPrimaryAction;
    @FXML private Button btnSecondaryAction;

    private VisitorLoginResult visitor;

    public void loadVisitor(VisitorLoginResult result) {
        this.visitor = result;
        lblVisitorDetails.setText("Traveler ID: " + result.getTravelerId()
            + "   National ID: " + result.getNationalId());

        if (result.isNewVisitor()) showBookingMode();
        else showManageMode();
    }

    @FXML
    private void handlePrimaryAction() {
        showBookingMode();
    }

    @FXML
    private void handleSecondaryAction() {
        showManageMode();
    }

    private void showBookingMode() {
        lblTitle.setText("Book a Visit");
        lblMode.setText(visitor != null && visitor.isNewVisitor()
            ? "A visitor account was created for this national ID. You can continue with a new booking."
            : "Create a new booking for this visitor.");
        btnPrimaryAction.setText("Booking a Visit");
        btnPrimaryAction.setDisable(true);
        btnSecondaryAction.setText("Managing Bookings");
        btnSecondaryAction.setDisable(false);
        btnSecondaryAction.setVisible(true);
        btnSecondaryAction.setManaged(true);
    }

    private void showManageMode() {
        lblTitle.setText("Manage My Bookings");
        lblMode.setText("Welcome back. You can manage existing bookings or create a new one.");
        btnPrimaryAction.setText("Booking a Visit");
        btnPrimaryAction.setDisable(false);
        btnSecondaryAction.setText("Managing Bookings");
        btnSecondaryAction.setDisable(true);
        btnSecondaryAction.setVisible(true);
        btnSecondaryAction.setManaged(true);
    }
}
