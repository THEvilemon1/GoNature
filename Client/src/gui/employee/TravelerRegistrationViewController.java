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
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class TravelerRegistrationViewController implements EmployeeAwareController, Initializable {

    @FXML private Label lblWelcome;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private VBox boxFamilyMembers;
    @FXML private TextField txtFamilyMembers;
    @FXML private VBox boxCreditCard;
    @FXML private TextField txtCreditCard;
    @FXML private Label lblStatus;
    @FXML private Button btnRegister;

    private Employee employee;

    private static final String LABEL_CLUB = "Club Member";
    private static final String LABEL_GUIDE = "Tour Guide";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbType.setItems(FXCollections.observableArrayList(LABEL_CLUB, LABEL_GUIDE));

        // Show family/credit-card fields only for Club Member
        cmbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isClub = LABEL_CLUB.equals(newVal);
            boxFamilyMembers.setVisible(isClub);
            boxFamilyMembers.setManaged(isClub);
            boxCreditCard.setVisible(isClub);
            boxCreditCard.setManaged(isClub);
            if (!isClub) {
                txtFamilyMembers.clear();
                txtCreditCard.clear();
            }
        });
    }

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        clearStatus();
 
        String typeLabel = cmbType.getValue();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String nationalId = txtNationalId.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
 
        // ---- Type ----
        if (typeLabel == null) {
            showStatus("Please select a registration type.", true);
            return;
        }
 
        // ---- Required fields present ----
        if (firstName.isEmpty() || lastName.isEmpty() || nationalId.isEmpty()
                || email.isEmpty() || phone.isEmpty()) {
            showStatus("Please fill in all fields.", true);
            return;
        }
 
        // ---- First name ----
        if (!isValidName(firstName)) {
            showStatus("First name must contain letters only.", true);
            return;
        }
 
        // ---- Last name ----
        if (!isValidName(lastName)) {
            showStatus("Last name must contain letters only.", true);
            return;
        }
 
        // ---- National ID ----
        if (!isValidNationalId(nationalId)) {
            showStatus("National ID must be exactly 9 digits.", true);
            return;
        }
 
        // ---- Email ----
        if (!isValidEmail(email)) {
            showStatus("Please enter a valid email address (e.g. name@example.com).", true);
            return;
        }
 
        // ---- Phone ----
        if (!isValidPhone(phone)) {
            showStatus("Phone number must be 10 digits starting with 05 (e.g. 0501234567).", true);
            return;
        }
 
        boolean isClub = LABEL_CLUB.equals(typeLabel);
        String type = isClub ? SubscriberRequest.TYPE_CLUB_MEMBER : SubscriberRequest.TYPE_GUIDE;
 
        int familyMembers = 0;
        String creditCard = null;
 
        // ---- Club-member-only fields ----
        if (isClub) {
            String familyStr = txtFamilyMembers.getText().trim();
            if (familyStr.isEmpty()) {
                showStatus("Please enter the total number of family members.", true);
                return;
            }
            if (!familyStr.matches("\\d+")) {
                showStatus("Family members must be a number.", true);
                return;
            }
            familyMembers = Integer.parseInt(familyStr);
            if (familyMembers < 2) {
                showStatus("A club membership must include at least 2 family members " +
                           "(the subscriber and at least one other).", true);
                return;
            }
 
            // Credit card is optional, but if provided must be exactly 16 digits
            String cc = txtCreditCard.getText().trim();
            if (!cc.isEmpty()) {
                if (!isValidCreditCard(cc)) {
                    showStatus("Credit card must be exactly 16 digits, or left empty to pay cash.", true);
                    return;
                }
                creditCard = cc;
            }
        }
 
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showStatus("Not connected to server.", true);
            return;
        }
 
        btnRegister.setDisable(true);
        showStatus("Registering...", false);
 
        client.setListener(new ServerResponseListener() {
            @Override
            public void onRegisterTravelerResult(String result) {
                Platform.runLater(() -> {
                    btnRegister.setDisable(false);
                    if ("SUCCESS".equals(result)) {
                        showStatus("Registration successful!", false);
                        clearForm();
                    } else {
                        showStatus(result, true);
                    }
                });
            }
 
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {
                Platform.runLater(() -> {
                    btnRegister.setDisable(false);
                    showStatus("Error: " + msg, true);
                });
            }
        });
 
        try {
            SubscriberRequest req = new SubscriberRequest(
                firstName, lastName, email, phone, nationalId, type,
                familyMembers, creditCard);
            client.sendToServer(new Message("REGISTER_TRAVELER", req));
        } catch (Exception e) {
            btnRegister.setDisable(false);
            showStatus("Error: " + e.getMessage(), true);
        }
    }
    
    
    
 // National ID: exactly 9 digits
    private boolean isValidNationalId(String id) {
        return id.matches("\\d{9}");
    }
     
    // Israeli mobile: 10 digits starting with 05
    private boolean isValidPhone(String phone) {
        return phone.matches("05\\d{8}");
    }
     
    // Standard email format: something@something.something
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
     
    // Name: letters only (Hebrew + English) and spaces, at least 1 char
    private boolean isValidName(String name) {
        return name.matches("^[A-Za-z\\u0590-\\u05FF ]+$");
    }
     
    // Credit card: exactly 16 digits
    private boolean isValidCreditCard(String cc) {
        return cc.matches("\\d{16}");
    }
    
    
    @FXML
    public void handleLogout(ActionEvent event) throws Exception {
        ParkClient client = ParkClient.getInstance();
        if (client != null && client.isConnected()) {
            try {
                client.sendToServer(new Message("EMPLOYEE_LOGOUT", employee.getUsername()));
            } catch (Exception e) {
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

    private void clearForm() {
        cmbType.setValue(null);
        txtFirstName.clear();
        txtLastName.clear();
        txtNationalId.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtFamilyMembers.clear();
        txtCreditCard.clear();
    }

    private void showStatus(String message, boolean isError) {
        lblStatus.setText(message);
        lblStatus.getStyleClass().removeAll("msg-error", "msg-success");
        lblStatus.getStyleClass().add(isError ? "msg-error" : "msg-success");
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void clearStatus() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}