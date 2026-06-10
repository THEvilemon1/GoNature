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

    public void loadVisitor(VisitorLoginResult result) {
        lblVisitorDetails.setText("Traveler ID: " + result.getTravelerId()
            + "   National ID: " + result.getNationalId());

        if (result.isNewVisitor()) {
            lblTitle.setText("Book a Visit");
            lblMode.setText("A visitor account was created for this national ID. You can continue with a new booking.");
            btnPrimaryAction.setText("Create Booking");
            btnSecondaryAction.setVisible(false);
            btnSecondaryAction.setManaged(false);
        } else {
            lblTitle.setText("Manage My Bookings");
            lblMode.setText("Welcome back. You can manage existing bookings or create a new one.");
            btnPrimaryAction.setText("New Booking");
            btnSecondaryAction.setText("My Bookings");
            btnSecondaryAction.setVisible(true);
            btnSecondaryAction.setManaged(true);
        }
    }
}
