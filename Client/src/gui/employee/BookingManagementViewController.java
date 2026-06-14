package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class BookingManagementViewController implements EmployeeAwareController, Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblCurrentVisitors;

    private Employee employee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");
        refreshCurrentVisitors();
    }

    public void refreshCurrentVisitors() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override
            public void onParkVisitorsResult(int currentVisitors) {
                Platform.runLater(() ->
                    lblCurrentVisitors.setText("Current Visitors: " + currentVisitors));
            }
            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        });

        try {
            client.sendToServer(new Message("GET_PARK_CURRENT_VISITORS", employee.getParkId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleEnterVisitor(ActionEvent event) throws Exception {
        Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/EnterVisitorView.fxml"));
        Parent root = loader.load();
        EnterVisitorViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Enter Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    @FXML
    public void handleExitVisitor(ActionEvent event) throws Exception {
        Stage currentStage = (Stage) lblWelcome.getScene().getWindow();
        currentStage.hide();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/ExitVisitorView.fxml"));
        Parent root = loader.load();
        ExitVisitorViewController controller = loader.getController();
        controller.setEmployee(employee);

        Stage stage = new Stage();
        stage.setTitle("Exit Visitor");
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
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
        stage.show();
    }
}