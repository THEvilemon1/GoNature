package gui;

import common.VisitorLoginResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class VisitorHomeController {

    @FXML private VBox viewMain;
    @FXML private VBox viewDetail;
    @FXML private Label lblVisitorDetails;
    @FXML private Label lblDetailTitle;

    public void loadVisitor(VisitorLoginResult result) {
        lblVisitorDetails.setText(
            "Traveler ID: " + result.getTravelerId()
            + "   ·   National ID: " + result.getNationalId()
        );
    }

    @FXML
    private void handlePrimaryAction() {
        lblDetailTitle.setText("Book a Visit");
        showDetail();
    }

    @FXML
    private void handleSecondaryAction() {
        lblDetailTitle.setText("My Bookings");
        showDetail();
    }

    @FXML
    private void handleBack() {
        showMain();
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
}

