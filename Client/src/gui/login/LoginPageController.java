package gui.login;

import boundaries.login.EmployeeLogin;
import boundaries.login.TravelerLoginAndRegister;
import client.loginController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginPageController (JavaFX / GUI Layer)
 *
 * The bridge between LoginPage.fxml and the ECB control layer.
 *
 * How it works:
 *  1. The user picks a role (Employee or Traveler) via the ToggleGroup.
 *  2. The relevant input section slides into view (employee fields vs traveler field).
 *  3. On "Login" click, this controller builds the right Login strategy,
 *     hands it to loginController, and lets the controller drive the rest.
 */
public class LoginPageController implements Initializable {

    // -------------------------------------------------------------------------
    // FXML injected nodes
    // -------------------------------------------------------------------------

    /** Role selector toggle buttons */
    @FXML private ToggleButton btnEmployee;
    @FXML private ToggleButton btnTraveler;

    /** Employee-specific fields */
    @FXML private VBox         employeeSection;
    @FXML private TextField    txtEmployeeId;
    @FXML private PasswordField txtPassword;

    /** Traveler-specific fields */
    @FXML private VBox         travelerSection;
    @FXML private TextField    txtTravelerId;

    /** Action buttons */
    @FXML private Button       btnLogin;

    /** Feedback label shown below the form */
    @FXML private Label        lblMessage;

    // -------------------------------------------------------------------------
    // Initializable
    // -------------------------------------------------------------------------

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Default: Employee tab is selected, traveler section is hidden
        showEmployeeSection();

        // Wire toggle buttons to section visibility
        btnEmployee.setOnAction(e -> showEmployeeSection());
        btnTraveler.setOnAction(e -> showTravelerSection());
    }

    // -------------------------------------------------------------------------
    // FXML action handlers
    // -------------------------------------------------------------------------

    /**
     * Called when the user presses the "Login" button.
     * Builds the correct Login strategy and delegates to loginController.
     */
    @FXML
    private void handleLogin() {
        clearMessage();

        loginController controller;

        if (btnEmployee.isSelected()) {
            // --- Employee login path ---
            String id  = txtEmployeeId.getText().trim();
            String pwd = txtPassword.getText();

            controller = new loginController(new EmployeeLogin(id, pwd));

        } else {
            // --- Traveler login / register path ---
            String id = txtTravelerId.getText().trim();

            controller = new loginController(new TravelerLoginAndRegister(id));
        }

        boolean proceeded = controller.login();

        if (!proceeded) {
            // Validation failed — show a user-friendly hint
            showError("Please fill in all fields correctly before logging in.");
        } else {
            showSuccess("Connecting to server…");
            // TODO: disable the button while awaiting server response
        }
    }

    // -------------------------------------------------------------------------
    // Section visibility helpers
    // -------------------------------------------------------------------------

    private void showEmployeeSection() {
        employeeSection.setVisible(true);
        employeeSection.setManaged(true);
        travelerSection.setVisible(false);
        travelerSection.setManaged(false);

        // Keep toggle state consistent
        btnEmployee.setSelected(true);
        btnTraveler.setSelected(false);

        clearMessage();
        clearAllFields();
    }

    private void showTravelerSection() {
        travelerSection.setVisible(true);
        travelerSection.setManaged(true);
        employeeSection.setVisible(false);
        employeeSection.setManaged(false);

        btnTraveler.setSelected(true);
        btnEmployee.setSelected(false);

        clearMessage();
        clearAllFields();
    }

    // -------------------------------------------------------------------------
    // Feedback helpers
    // -------------------------------------------------------------------------

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().removeAll("msg-success");
        lblMessage.getStyleClass().add("msg-error");
        lblMessage.setVisible(true);
    }

    private void showSuccess(String message) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().removeAll("msg-error");
        lblMessage.getStyleClass().add("msg-success");
        lblMessage.setVisible(true);
    }

    private void clearMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
    }

    private void clearAllFields() {
        txtEmployeeId.clear();
        txtPassword.clear();
        txtTravelerId.clear();
    }
}
