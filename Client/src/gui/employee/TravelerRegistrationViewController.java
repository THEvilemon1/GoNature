package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Employee;
import common.Message;
import common.Order;
import common.SubscriberRequest;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class TravelerRegistrationViewController implements EmployeeAwareController, Initializable {

    @FXML private Label lblWelcome;
    @FXML private TextField txtNationalId;
    @FXML private Label lblCurrentRole;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private RadioButton rbClubMember;
    @FXML private RadioButton rbGuide;
    @FXML private VBox clubMemberSection;
    @FXML private Spinner<Integer> spnFamilyMembers;
    @FXML private TextField txtCreditCard;
    @FXML private Label lblMessage;
    @FXML private Button btnRegister;

    private Employee employee;
    // Cached from last Look Up — null means not looked up yet or traveler not found.
    // Layout: [0]=Boolean isGuide, [1]=Boolean isClubMember,
    //         [2]=String firstName, [3]=String lastName,
    //         [4]=String email, [5]=String phoneNumber
    private Object[] lookedUpStatus;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ToggleGroup typeGroup = new ToggleGroup();
        rbClubMember.setToggleGroup(typeGroup);
        rbGuide.setToggleGroup(typeGroup);

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isClub = newVal == rbClubMember;
            clubMemberSection.setVisible(isClub);
            clubMemberSection.setManaged(isClub);
        });

        // Hide club member section initially (no radio selected)
        clubMemberSection.setVisible(false);
        clubMemberSection.setManaged(false);

        // Reset cached status when the national ID is edited
        txtNationalId.textProperty().addListener((obs, oldVal, newVal) -> {
            lookedUpStatus = null;
            hideCurrentRole();
            setRegistrationEnabled(true);
        });
    }

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName());
    }

    // ── Look Up ──────────────────────────────────────────────────────────────

    @FXML
    private void handleLookUp() {
        String nationalId = txtNationalId.getText().trim();
        if (nationalId.isEmpty()) {
            showCurrentRole("Please enter a national ID first.", false);
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showCurrentRole("Not connected to server.", false);
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String errorMessage) {
                Platform.runLater(() -> showCurrentRole("Look-up failed: " + errorMessage, false));
            }

            @Override
            public void onTravelerStatusResult(Object[] status) {
                Platform.runLater(() -> {
                    lookedUpStatus = status;
                    if (status == null) {
                        showCurrentRole("Traveler not found — will be created on registration.", true);
                        clearPersonFields();
                        clearRoleSelection();
                        setRegistrationEnabled(true);
                        return;
                    }

                    String type = (String) status[0];

                    if ("EMPLOYEE".equals(type)) {
                        // This national ID belongs to staff — show their details, no role registration.
                        String role      = (String)  status[1];
                        Integer salary   = (Integer) status[2];
                        Integer parkId   = (Integer) status[3];
                        String parkName  = (String)  status[4];
                        String firstName = (String)  status[5];
                        String lastName  = (String)  status[6];
                        String email     = (String)  status[7];
                        String phone     = (String)  status[8];

                        txtFirstName.setText(firstName != null ? firstName : "");
                        txtLastName.setText(lastName   != null ? lastName  : "");
                        txtEmail.setText(email         != null ? email     : "");
                        txtPhone.setText(phone         != null ? phone     : "");
                        clearRoleSelection();
                        setRegistrationEnabled(false);

                        String park = (parkName != null) ? parkName
                                    : (parkId != null ? "park #" + parkId : "no assigned park");
                        showCurrentRole("Employee: " + prettyRole(role) + " at " + park
                                + (salary != null ? ", salary " + salary : "")
                                + ". Employees cannot be registered as Club Members or Tour Guides.", false);
                        return;
                    }

                    // TRAVELER
                    setRegistrationEnabled(true);
                    boolean isGuide      = (Boolean) status[1];
                    boolean isClubMember = (Boolean) status[2];
                    int familyMembers    = (Integer) status[3];
                    String firstName     = (String)  status[4];
                    String lastName      = (String)  status[5];
                    String email         = (String)  status[6];
                    String phone         = (String)  status[7];

                    // Pre-fill personal details
                    txtFirstName.setText(firstName != null ? firstName : "");
                    txtLastName.setText(lastName   != null ? lastName  : "");
                    txtEmail.setText(email         != null ? email     : "");
                    spnFamilyMembers.getValueFactory().setValue(familyMembers);
                    txtPhone.setText(phone         != null ? phone     : "");

                    // Auto-select current role
                    if (isGuide) {
                        rbGuide.setSelected(true);
                        showCurrentRole("Current role: Tour Guide", true);
                    } else if (isClubMember) {
                        rbClubMember.setSelected(true);
                        showCurrentRole("Current role: Club Member", true);
                    } else {
                        clearRoleSelection();
                        showCurrentRole("Traveler found — no special role assigned yet.", true);
                    }
                });
            }
        });

        try {
            client.sendToServer(new Message("GET_TRAVELER_STATUS", nationalId));
        } catch (IOException e) {
            showCurrentRole("Failed to send request: " + e.getMessage(), false);
        }
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @FXML
    private void handleRegister() {
        String nationalId = txtNationalId.getText().trim();
        String firstName  = txtFirstName.getText().trim();
        String lastName   = txtLastName.getText().trim();
        String email      = txtEmail.getText().trim();
        String phone      = txtPhone.getText().trim();

        if (nationalId.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || phone.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!rbClubMember.isSelected() && !rbGuide.isSelected()) {
            showError("Please select a registration type (Club Member or Tour Guide).");
            return;
        }

        String type;
        int familyMembers = 1;
        String creditCard = null;

        if (rbClubMember.isSelected()) {
            type = SubscriberRequest.TYPE_CLUB_MEMBER;
            familyMembers = spnFamilyMembers.getValue();
            creditCard = txtCreditCard.getText().trim();
            if (creditCard.isEmpty()) creditCard = null;
        } else {
            type = SubscriberRequest.TYPE_GUIDE;
        }

        // Employees can never be registered as club members or guides
        if (lookedUpStatus != null && "EMPLOYEE".equals(lookedUpStatus[0])) {
            showError("This national ID belongs to an employee and cannot be registered "
                    + "as a Club Member or Tour Guide.");
            return;
        }

        // Warn if a role switch is about to happen
        if (lookedUpStatus != null) {
            boolean currentlyGuide      = (Boolean) lookedUpStatus[1];
            boolean currentlyClubMember = (Boolean) lookedUpStatus[2];

            if (type.equals(SubscriberRequest.TYPE_GUIDE) && currentlyClubMember) {
                if (!confirmSwitch("This person is currently a Club Member.\n"
                        + "Registering them as a Tour Guide will revoke their club membership.\n\n"
                        + "Do you want to continue?")) {
                    return;
                }
            } else if (type.equals(SubscriberRequest.TYPE_CLUB_MEMBER) && currentlyGuide) {
                if (!confirmSwitch("This person is currently a Tour Guide.\n"
                        + "Registering them as a Club Member will revoke their guide status.\n\n"
                        + "Do you want to continue?")) {
                    return;
                }
            }
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        btnRegister.setDisable(true);
        clearMessage();

        SubscriberRequest request = new SubscriberRequest(
                firstName, lastName, email, phone, nationalId, type, familyMembers, creditCard);

        client.setListener(new ServerResponseListener() {
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    showError("Error: " + errorMessage);
                    btnRegister.setDisable(false);
                });
            }

            @Override
            public void onRegisterSubscriberResult(boolean success, String message) {
                Platform.runLater(() -> {
                    if (success) {
                        showSuccess(message);
                        clearForm();
                    } else {
                        showError(message);
                    }
                    btnRegister.setDisable(false);
                });
            }
        });

        try {
            client.sendToServer(new Message("REGISTER_SUBSCRIBER", request));
        } catch (IOException e) {
            showError("Failed to send request: " + e.getMessage());
            btnRegister.setDisable(false);
        }
    }

    private boolean confirmSwitch(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Role Switch");
        alert.setHeaderText("Role will change");
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() throws Exception {
        ParkClient client = ParkClient.getInstance();
        if (client != null && client.isConnected() && employee != null) {
            try {
                client.sendToServer(new Message("EMPLOYEE_LOGOUT", employee.getUsername()));
            } catch (IOException e) {
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
        WindowUtil.showMaximized(stage);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void clearForm() {
        txtNationalId.clear();
        clearPersonFields();
        clearRoleSelection();
        txtCreditCard.clear();
        spnFamilyMembers.getValueFactory().setValue(1);
        lookedUpStatus = null;
        hideCurrentRole();
    }

    private void clearPersonFields() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtPhone.clear();
    }

    private void clearRoleSelection() {
        rbClubMember.getToggleGroup().selectToggle(null);
    }

    // Enable/disable the role-registration controls (used to lock them when the
    // looked-up national ID belongs to an employee).
    private void setRegistrationEnabled(boolean enabled) {
        rbClubMember.setDisable(!enabled);
        rbGuide.setDisable(!enabled);
        btnRegister.setDisable(!enabled);
    }

    // Turns a role enum value (e.g. "service_rep") into a readable label.
    private String prettyRole(String role) {
        if (role == null || role.isEmpty()) return "Employee";
        String[] words = role.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private void showCurrentRole(String msg, boolean isInfo) {
        lblCurrentRole.setText(msg);
        lblCurrentRole.getStyleClass().removeAll("msg-success", "msg-error");
        lblCurrentRole.getStyleClass().add(isInfo ? "msg-success" : "msg-error");
        lblCurrentRole.setVisible(true);
        lblCurrentRole.setManaged(true);
    }

    private void hideCurrentRole() {
        lblCurrentRole.setVisible(false);
        lblCurrentRole.setManaged(false);
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("msg-success");
        lblMessage.getStyleClass().add("msg-error");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("msg-error");
        lblMessage.getStyleClass().add("msg-success");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void clearMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
