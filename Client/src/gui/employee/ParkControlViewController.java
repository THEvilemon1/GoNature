package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Employee;
import common.Message;
import common.ParkChangeRequest;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class ParkControlViewController implements EmployeeAwareController {

    @FXML private Label lblWelcome;
    @FXML private Label lblMessage;
    @FXML private ListView<String> lstActivityLog;

    @FXML private TextField txtNewMaxCapacity;
    @FXML private TextField txtNewGap;
    @FXML private TextField txtNewStayTime;

    private Employee employee;
    private Map<String, String> requestIdToLogText = new HashMap<>();

    // Holds the most recently sent request awaiting an PARK_CHANGE_REQUEST_RESULT
    private ParkChangeRequest pendingRequest;
    private TextField pendingField;

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");
        registerListener();
    }

    /**
     * Registers ONE persistent listener for the lifetime of this screen.
     * This avoids race conditions caused by swapping listeners back and forth,
     * which previously caused approval responses to be silently dropped.
     */
    private void registerListener() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override public void onOrderExistsResult(boolean e) {}
            @Override public void onOrderResult(common.Order o) {}
            @Override public void onUpdateOrderResult(boolean s) {}
            @Override public void onError(String msg) {
                Platform.runLater(() -> showError("Error: " + msg));
            }

            @Override
            public void onParkChangeRequestResult(boolean success) {
                Platform.runLater(() -> {
                    if (pendingRequest == null) return;
                    if (pendingField != null) pendingField.clear();

                    String logText = "⏳ WAITING  |  " + pendingRequest.getRequestTitle();
                    requestIdToLogText.put(pendingRequest.getRequestId(), logText);
                    addToLog(logText);

                    if (success) {
                        showSuccess("Request sent! Waiting for department manager approval.");
                    } else {
                        showSuccess("Request saved! Department manager is offline — it will be reviewed when they log in.");
                    }

                    pendingRequest = null;
                    pendingField = null;
                });
            }

            @Override
            public void onParkChangeApprovalResult(String requestId, boolean approved) {
                System.out.println("[DEBUG] onParkChangeApprovalResult received! requestId=" + requestId + " approved=" + approved);
                Platform.runLater(() -> {
                    System.out.println("[DEBUG] requestIdToLogText contains key? " + requestIdToLogText.containsKey(requestId));
                    updateLogByRequestId(requestId, approved ? "✔ APPROVED" : "✘ REJECTED");
                    if (approved) {
                        showSuccess("A request was APPROVED and applied!");
                    } else {
                        showError("A request was REJECTED by the department manager.");
                    }
                });
            }
        });
    }

    @FXML
    public void handleOpenPromotions(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/employee/PromotionsView.fxml"));
            javafx.scene.Parent root = loader.load();

            PromotionsViewController controller = loader.getController();
            controller.setEmployee(employee);

            Stage stage = new Stage();
            stage.setTitle("Promotions Management");
            stage.setScene(new javafx.scene.Scene(root));

            // When the Promotions window closes, restore THIS screen's listener,
            // since ParkClient only supports one active listener at a time.
            stage.setOnHidden(e -> registerListener());

            WindowUtil.showMaximized(stage);
        } catch (Exception e) {
            showError("Failed to open promotions screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    public void handleVisitorsReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/ParkVisitorsReportView.fxml"));
            Parent root = loader.load();
            ParkVisitorsReportViewController controller = loader.getController();
            controller.setEmployee(employee);

            Stage stage = new Stage();
            stage.setTitle("Park Visitors Report");
            stage.setScene(new Scene(root));
            // Report screens hijack the shared single listener; restore ours on close.
            stage.setOnHidden(e -> registerListener());
            WindowUtil.showMaximized(stage);

        } catch (Exception e) {
            showError("Failed to open visitors report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUsageReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/ParkUsageReportView.fxml"));
            Parent root = loader.load();
            ParkUsageReportViewController controller = loader.getController();
            controller.setEmployee(employee);

            Stage stage = new Stage();
            stage.setTitle("Park Usage Report");
            stage.setScene(new Scene(root));
            // Report screens hijack the shared single listener; restore ours on close.
            stage.setOnHidden(e -> registerListener());
            WindowUtil.showMaximized(stage);

        } catch (Exception e) {
            showError("Failed to open usage report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    @FXML
    public void handleUpdateMaxCapacity(ActionEvent event) {
        sendRequest(txtNewMaxCapacity, ParkChangeRequest.ParameterType.MAX_CAPACITY,
                "Max Capacity", 1, 10000);
    }

    @FXML
    public void handleUpdateGap(ActionEvent event) {
        sendRequest(txtNewGap, ParkChangeRequest.ParameterType.GAP,
                "Gap for Walk-Ins", 0, 1000);
    }

    @FXML
    public void handleUpdateStayTime(ActionEvent event) {
        sendRequest(txtNewStayTime, ParkChangeRequest.ParameterType.DEFAULT_STAY_TIME,
                "Default Stay Time", 1, 24);
    }

    private void sendRequest(TextField field, ParkChangeRequest.ParameterType type,
                              String label, int min, int max) {
        clearMessage();
        String text = field.getText().trim();
        if (text.isEmpty()) {
            showError("Please enter a value for " + label + ".");
            return;
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for " + label + ".");
            return;
        }

        if (value < min || value > max) {
            showError(label + " must be between " + min + " and " + max + ".");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        ParkChangeRequest request = new ParkChangeRequest(
                employee.getParkId(),
                type,
                value,
                employee.getUsername(),
                employee.getEmployeeId(),
                0,
                UUID.randomUUID().toString(),
                "Change " + label + " to " + value
        );

        pendingRequest = request;
        pendingField = field;

        try {
            client.sendToServer(new Message("PARK_CHANGE_REQUEST", request));
        } catch (Exception e) {
            showError("Failed to send request.");
            pendingRequest = null;
            pendingField = null;
        }
    }

    private void addToLog(String entry) {
        lstActivityLog.getItems().add(0, entry);
    }

    private void updateLogByRequestId(String requestId, String newStatus) {
        String originalText = requestIdToLogText.get(requestId);
        if (originalText == null) return;
        for (int i = 0; i < lstActivityLog.getItems().size(); i++) {
            if (lstActivityLog.getItems().get(i).equals(originalText)) {
                String updated = originalText.replace("⏳ WAITING", newStatus);
                lstActivityLog.getItems().set(i, updated);
                requestIdToLogText.put(requestId, updated);
                break;
            }
        }
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

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("msg-success");
        lblMessage.getStyleClass().add("msg-error");
        lblMessage.setVisible(true);
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("msg-error");
        lblMessage.getStyleClass().add("msg-success");
        lblMessage.setVisible(true);
    }

    private void clearMessage() {
        lblMessage.setText("");
        lblMessage.setVisible(false);
    }
}