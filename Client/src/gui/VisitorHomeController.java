package gui;

import client.ParkClient;
import client.SessionManager;
import common.Message;
import common.VisitorLoginResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VisitorHomeController {

    @FXML private VBox viewMain;
    @FXML private VBox viewDetail;
    @FXML private Label lblVisitorDetails;
    @FXML private Label lblDetailTitle;

    private VisitorLoginResult currentUser;

    public void loadVisitor(VisitorLoginResult result) {
        this.currentUser = result;
        lblVisitorDetails.setText(
            "Traveler ID: " + result.getTravelerId()
            + "   ·   National ID: " + result.getNationalId()
        );
        
        // Set up window close handler to logout when user exits
        setupWindowCloseHandler();
    }

    /**
     * Set up the window close event handler to send logout message to server.
     */
    private void setupWindowCloseHandler() {
        // Find the stage and attach close handler
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) viewMain.getScene().getWindow();
                if (stage != null) {
                    stage.setOnCloseRequest(e -> {
                        handleLogout();
                    });
                }
            } catch (Exception e) {
                System.out.println("Could not set up window close handler: " + e.getMessage());
            }
        });
    }

    /**
     * Handle logout: send message to server and clear session.
     */
    private void handleLogout() {
        if (currentUser != null) {
            // Send logout message to server
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
        
        // Clear local session
        SessionManager.getInstance().logout();
    }

    @FXML
    private void handleLogoutButton(javafx.event.ActionEvent event) {
        handleLogout();
        // Close the window after logout
        try {
            ((Node) event.getSource()).getScene().getWindow().hide();
        } catch (Exception e) {
            System.out.println("Error closing window: " + e.getMessage());
        }
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

