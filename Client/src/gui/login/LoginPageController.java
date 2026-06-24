package gui.login;

import boundaries.login.EmployeeLogin;
import boundaries.login.TravelerLoginAndRegister;
import client.ParkClient;
import client.ServerResponseListener;
import client.SessionManager;
import client.WindowUtil;
import client.loginController;
import common.Booking;
import common.Employee;
import common.Order;
import common.VisitorLoginResult;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginPageController implements Initializable {

    @FXML private ToggleButton btnEmployee;
    @FXML private ToggleButton btnTraveler;

    @FXML private VBox         employeeSection;
    @FXML private TextField    txtEmployeeUsername;
    @FXML private PasswordField txtPassword;

    @FXML private VBox         travelerSection;
    @FXML private TextField    txtTravelerId;

    @FXML private Button       btnLogin;
    @FXML private Label        lblMessage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showEmployeeSection();
        btnEmployee.setOnAction(e -> showEmployeeSection());
        btnTraveler.setOnAction(e -> showTravelerSection());
    }

    @FXML
    private void handleLogin(javafx.event.ActionEvent event) {
        clearMessage();

        if (btnEmployee.isSelected()) {
            handleEmployeeLogin(event);
        } else {
            handleTravelerLogin(event);
        }
    }

    // -------------------------------------------------------------------------
    // Employee Login
    // -------------------------------------------------------------------------

    private void handleEmployeeLogin(javafx.event.ActionEvent event) {
        String username = txtEmployeeUsername.getText().trim();
        String password = txtPassword.getText();

        loginController controller = new loginController(new EmployeeLogin(username, password));

        boolean proceeded = controller.login();
        if (!proceeded) {
            showError("Please fill in all fields correctly before logging in.");
            return;
        }

        showSuccess("Connecting to server…");
        btnLogin.setDisable(true);

        // Register listener for server response
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Client is not connected to the server.");
            btnLogin.setDisable(false);
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override
            public void onEmployeeLoginSuccess(Employee employee) {
                Platform.runLater(() -> openEmployeeScreen(event, employee));
            }

            @Override
            public void onEmployeeLoginFailed(String reason) {
                Platform.runLater(() -> {
                    showError(reason);
                    btnLogin.setDisable(false);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    showError(errorMessage);
                    btnLogin.setDisable(false);
                });
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
        });
    }

    private void openEmployeeScreen(javafx.event.ActionEvent event, Employee employee) {
        try {
            String fxmlFile;
            String title;

            switch (employee.getRole()) {
                case Employee.ROLE_PARK_WORKER:
                    fxmlFile = "/gui/employee/BookingManagementView.fxml";
                    title = "Booking Management";
                    break;
                case Employee.ROLE_PARK_MANAGER:
                    fxmlFile = "/gui/employee/ParkControlView.fxml";
                    title = "Park Control";
                    break;
                case Employee.ROLE_DEPARTMENT_MANAGER:
                    fxmlFile = "/gui/employee/DepartmentOverviewView.fxml";
                    title = "Department Overview";
                    break;
                case Employee.ROLE_SERVICE_REP:
                    fxmlFile = "/gui/employee/TravelerRegistrationView.fxml";
                    title = "Traveler Registration";
                    break;
                default:
                    showError("Unknown role: " + employee.getRole());
                    btnLogin.setDisable(false);
                    return;
            }

            ((Node) event.getSource()).getScene().getWindow().hide();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Pass employee to next controller
            Object controller = loader.getController();
            if (controller instanceof EmployeeAwareController) {
                ((EmployeeAwareController) controller).setEmployee(employee);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));

            // X button — logout employee and exit
            stage.setOnCloseRequest(e -> {
                sendEmployeeLogout(employee.getUsername());
                System.exit(0);
            });

            WindowUtil.showMaximized(stage);

        } catch (Exception e) {
            showError("Failed to open screen: " + e.getMessage());
            btnLogin.setDisable(false);
            e.printStackTrace();
        }
    }

    private void sendEmployeeLogout(String username) {
        ParkClient client = ParkClient.getInstance();
        if (client != null && client.isConnected()) {
            try {
                client.sendToServer(new common.Message("EMPLOYEE_LOGOUT", username));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Traveler Login
    // -------------------------------------------------------------------------

    private void handleTravelerLogin(javafx.event.ActionEvent event) {
        String id = txtTravelerId.getText().trim();
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Client is not connected to the server.");
            return;
        }

        loginController controller = new loginController(new TravelerLoginAndRegister(id));

        boolean proceeded = controller.login();
        if (!proceeded) {
            showError("Please fill in all fields correctly before logging in.");
            return;
        }

        showSuccess("Connecting to server…");
        btnLogin.setDisable(true);
        registerTravelerLoginListener(event);
    }

    private void registerTravelerLoginListener(javafx.event.ActionEvent event) {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Client is not connected to the server.");
            return;
        }

        client.setListener(new ServerResponseListener() {
            @Override
            public void onVisitorLoginResult(VisitorLoginResult result) {
                Platform.runLater(() -> openVisitorHome(event, result));
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    showError(errorMessage);
                    btnLogin.setDisable(false);
                });
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
        });
    }

    private void openVisitorHome(javafx.event.ActionEvent event, VisitorLoginResult result) {
        try {
            SessionManager.getInstance().setCurrentUser(result);

            ((Node) event.getSource()).getScene().getWindow().hide();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/VisitorHome.fxml"));
            Parent root = loader.load();
            gui.VisitorHomeController controller = loader.getController();
            controller.loadVisitor(result);

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/gui/VisitorHome.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("GoNature - Visitor");
            stage.setScene(scene);
            WindowUtil.showMaximized(stage);
        } catch (Exception e) {
            showError("Failed to open visitor screen.");
            btnLogin.setDisable(false);
            e.printStackTrace();
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
        txtEmployeeUsername.clear();
        txtPassword.clear();
        txtTravelerId.clear();
    }
}
