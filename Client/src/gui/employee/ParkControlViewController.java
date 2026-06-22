package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Employee;
import common.Message;
import common.ParkChangeRequest;
import gui.login.EmployeeAwareController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkControlViewController implements EmployeeAwareController {

    private static final DateTimeFormatter LOG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label lblWelcome;
    @FXML private Label lblMessage;
    @FXML private Label lblCurrentMaxCapacity;
    @FXML private Label lblCurrentGap;
    @FXML private Label lblCurrentStayTime;
    @FXML private TableView<ActivityLogEntry> tblActivityLog;
    @FXML private TableColumn<ActivityLogEntry, String> colRequestNumber;
    @FXML private TableColumn<ActivityLogEntry, String> colRequestDateTime;
    @FXML private TableColumn<ActivityLogEntry, String> colRequestDetails;
    @FXML private TableColumn<ActivityLogEntry, String> colRequestStatus;
    @FXML private TableColumn<ActivityLogEntry, String> colResponseDateTime;

    @FXML private TextField txtNewMaxCapacity;
    @FXML private TextField txtNewGap;
    @FXML private TextField txtNewStayTime;

    private Employee employee;
    private final Map<String, ActivityLogEntry> requestIdToLogEntry = new HashMap<>();
    private int nextRequestNumber = 1;
    private Integer currentMaxCapacity;
    private Integer currentGap;
    private Integer currentStayTime;

    // Holds the most recently sent request awaiting a PARK_CHANGE_REQUEST_RESULT.
    private ParkChangeRequest pendingRequest;
    private TextField pendingField;
    private LocalDateTime pendingRequestSentAt;

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText(employee.getFirstName() + " " + employee.getLastName());
        setupActivityLogTable();
        updateCurrentSettings(
            employee.getParkMaxCapacity(),
            employee.getParkGap(),
            employee.getParkDefaultStayTime());
        registerListener();
    }

    private void setupActivityLogTable() {
        colRequestNumber.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getRequestNumber())));
        colRequestDateTime.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().getRequestDateTime())));
        colRequestDetails.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRequestDetails()));
        colRequestStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRequestStatus()));
        colResponseDateTime.setCellValueFactory(data ->
                new SimpleStringProperty(formatTime(data.getValue().getResponseDateTime())));
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
                Platform.runLater(() -> handleServerError(msg));
            }

            @Override
            public void onParkChangeRequestResult(boolean success) {
                Platform.runLater(() -> {
                    if (pendingRequest == null) return;
                    if (pendingField != null) pendingField.clear();

                    ActivityLogEntry entry = new ActivityLogEntry(
                            nextRequestNumber++,
                            pendingRequest.getRequestId(),
                            pendingRequestSentAt,
                            pendingRequest.getRequestTitle(),
                            "Waiting",
                            null,
                            pendingRequest.getParameterType(),
                            pendingRequest.getNewValue()
                    );
                    requestIdToLogEntry.put(pendingRequest.getRequestId(), entry);
                    tblActivityLog.getItems().add(0, entry);

                    showSuccess("Request sent");

                    pendingRequest = null;
                    pendingField = null;
                    pendingRequestSentAt = null;
                });
            }

            @Override
            public void onParkChangeApprovalResult(String requestId, boolean approved) {
                Platform.runLater(() -> {
                    updateLogByRequestId(requestId, approved);
                });
            }
        });
    }

    @FXML
    public void handleOpenPromotions(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/employee/PromotionsView.fxml"));
            Parent root = loader.load();

            PromotionsViewController controller = loader.getController();
            controller.setEmployee(employee);

            Stage stage = new Stage();
            stage.setTitle("Promotions Management");
            stage.setScene(new Scene(root));

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
                "total park capacity", "Change total park capacity to ", 1, 10000);
    }

    @FXML
    public void handleUpdateGap(ActionEvent event) {
        sendRequest(txtNewGap, ParkChangeRequest.ParameterType.GAP,
                "walk-in reserved spots", "Reserve ", 0, 1000);
    }

    @FXML
    public void handleUpdateStayTime(ActionEvent event) {
        sendRequest(txtNewStayTime, ParkChangeRequest.ParameterType.DEFAULT_STAY_TIME,
                "default visitor stay duration", "Set default visitor stay duration to ", 1, 24);
    }

    private void sendRequest(TextField field, ParkChangeRequest.ParameterType type,
                             String label, String titlePrefix, int min, int max) {
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
            showError(capitalize(label) + " must be between " + min + " and " + max + ".");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        String requestTitle = buildRequestTitle(type, titlePrefix, value);
        ParkChangeRequest request = new ParkChangeRequest(
                employee.getParkId(),
                type,
                value,
                employee.getUsername(),
                employee.getEmployeeId(),
                0,
                UUID.randomUUID().toString(),
                requestTitle
        );

        pendingRequest = request;
        pendingField = field;
        pendingRequestSentAt = LocalDateTime.now();

        try {
            client.sendToServer(new Message("PARK_CHANGE_REQUEST", request));
        } catch (Exception e) {
            showError("Failed to send request.");
            pendingRequest = null;
            pendingField = null;
            pendingRequestSentAt = null;
        }
    }

    private String buildRequestTitle(ParkChangeRequest.ParameterType type, String titlePrefix, int value) {
        if (type == ParkChangeRequest.ParameterType.GAP) {
            return titlePrefix + value + " spots for walk-in visitors";
        }
        if (type == ParkChangeRequest.ParameterType.DEFAULT_STAY_TIME) {
            return titlePrefix + value + " hours";
        }
        return titlePrefix + value;
    }

    private void updateLogByRequestId(String requestId, boolean approved) {
        ActivityLogEntry entry = requestIdToLogEntry.get(requestId);
        if (entry == null || entry.isFinalStatus()) return;

        entry.setRequestStatus(approved ? "Confirmed" : "Rejected");
        entry.setResponseDateTime(LocalDateTime.now());
        entry.setFinalStatus(true);
        tblActivityLog.refresh();

        if (approved) {
            applyApprovedValue(entry);
            showSuccess("Request #" + entry.getRequestNumber() + " confirmed");
        } else {
            showError("Request #" + entry.getRequestNumber() + " was rejected");
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

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(LOG_TIME_FORMAT);
    }

    private void updateCurrentSettings(Integer maxCapacity, Integer gap, Integer defaultStayTime) {
        currentMaxCapacity = maxCapacity;
        currentGap = gap;
        currentStayTime = defaultStayTime;
        refreshCurrentValueLabels();
    }

    private void applyApprovedValue(ActivityLogEntry entry) {
        if (entry.getParameterType() == ParkChangeRequest.ParameterType.MAX_CAPACITY) {
            currentMaxCapacity = entry.getRequestedValue();
        } else if (entry.getParameterType() == ParkChangeRequest.ParameterType.GAP) {
            currentGap = entry.getRequestedValue();
        } else if (entry.getParameterType() == ParkChangeRequest.ParameterType.DEFAULT_STAY_TIME) {
            currentStayTime = entry.getRequestedValue();
        }
        refreshCurrentValueLabels();
    }

    private void refreshCurrentValueLabels() {
        lblCurrentMaxCapacity.setText("Current park capacity: " + formatCurrentValue(currentMaxCapacity, ""));
        lblCurrentGap.setText("Current walk-in reserve: " + formatCurrentValue(currentGap, ""));
        lblCurrentStayTime.setText("Current stay duration: " + formatCurrentValue(currentStayTime, " hours"));
    }

    private String formatCurrentValue(Integer value, String suffix) {
        return value == null ? "--" : value + suffix;
    }

    private void handleServerError(String msg) {
        showError("Error: " + msg);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
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

    public static class ActivityLogEntry {
        private final int requestNumber;
        private final String requestId;
        private final LocalDateTime requestDateTime;
        private final String requestDetails;
        private final ParkChangeRequest.ParameterType parameterType;
        private final int requestedValue;
        private String requestStatus;
        private LocalDateTime responseDateTime;
        private boolean finalStatus;

        public ActivityLogEntry(int requestNumber, String requestId, LocalDateTime requestDateTime,
                                String requestDetails, String requestStatus,
                                LocalDateTime responseDateTime,
                                ParkChangeRequest.ParameterType parameterType,
                                int requestedValue) {
            this.requestNumber = requestNumber;
            this.requestId = requestId;
            this.requestDateTime = requestDateTime;
            this.requestDetails = requestDetails;
            this.requestStatus = requestStatus;
            this.responseDateTime = responseDateTime;
            this.parameterType = parameterType;
            this.requestedValue = requestedValue;
        }

        public int getRequestNumber() {
            return requestNumber;
        }

        public String getRequestId() {
            return requestId;
        }

        public LocalDateTime getRequestDateTime() {
            return requestDateTime;
        }

        public String getRequestDetails() {
            return requestDetails;
        }

        public ParkChangeRequest.ParameterType getParameterType() {
            return parameterType;
        }

        public int getRequestedValue() {
            return requestedValue;
        }

        public String getRequestStatus() {
            return requestStatus;
        }

        public void setRequestStatus(String requestStatus) {
            this.requestStatus = requestStatus;
        }

        public LocalDateTime getResponseDateTime() {
            return responseDateTime;
        }

        public void setResponseDateTime(LocalDateTime responseDateTime) {
            this.responseDateTime = responseDateTime;
        }

        public boolean isFinalStatus() {
            return finalStatus;
        }

        public void setFinalStatus(boolean finalStatus) {
            this.finalStatus = finalStatus;
        }
    }
}
