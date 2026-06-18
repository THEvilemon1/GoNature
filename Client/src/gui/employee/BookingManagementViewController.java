package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class BookingManagementViewController implements EmployeeAwareController, Initializable {

    @FXML private Label lblWelcome;
    @FXML private Label lblCurrentVisitors;
    @FXML private ListView<String> lstPending;
    @FXML private ListView<String> lstCheckedIn;

    private Employee employee;
    private Timer refreshTimer;

    private final ObservableList<String> pendingItems = FXCollections.observableArrayList();
    private final ObservableList<String> checkedInItems = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lstPending.setItems(pendingItems);
        lstCheckedIn.setItems(checkedInItems);
    }

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");
        refreshData();
        startAutoRefresh();
    }

    // Auto-refresh both the visitor count and the booking lists every 30 seconds
    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        }, 30000, 30000);
    }

    private void refreshData() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override
            public void onParkVisitorsResult(int currentVisitors) {
                Platform.runLater(() ->
                    lblCurrentVisitors.setText("Current Visitors: " + currentVisitors));
            }

            @Override
            public void onTodayBookingsResult(ArrayList<Booking> bookings) {
                Platform.runLater(() -> populateLists(bookings));
            }

            @Override public void onOrderExistsResult(boolean exists) {}
            @Override public void onOrderResult(Order order) {}
            @Override public void onUpdateOrderResult(boolean success) {}
            @Override public void onError(String msg) {}
        });

        try {
            client.sendToServer(new Message("GET_PARK_CURRENT_VISITORS", employee.getParkId()));
            client.sendToServer(new Message("GET_TODAY_BOOKINGS", employee.getParkId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateLists(ArrayList<Booking> bookings) {
        pendingItems.clear();
        checkedInItems.clear();

        for (Booking b : bookings) {
            String time = b.getVisitorTime().toLocalTime().format(TIME_FMT);
            String row = time + "   |   " + b.getNumberOfVisitors() + " visitor(s)";

            if (Booking.STATUS_PENDING.equals(b.getStatus())) {
                pendingItems.add(row);
            } else if ("CHECKED_IN".equals(b.getStatus())) {
                checkedInItems.add(row);
            }
        }

        if (pendingItems.isEmpty()) pendingItems.add("No pending bookings");
        if (checkedInItems.isEmpty()) checkedInItems.add("No checked-in bookings");
    }

    @FXML
    public void handleEnterVisitor(ActionEvent event) throws Exception {
        stopRefresh();
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
        stopRefresh();
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
        stopRefresh();
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

    private void stopRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}