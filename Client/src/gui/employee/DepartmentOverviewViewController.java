package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import client.WindowUtil;
import common.Employee;
import common.Message;
import common.ParkChangeRequest;
import common.ParkSubmittedReport;
import common.PromotionRequest;
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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public class DepartmentOverviewViewController implements EmployeeAwareController {

    @FXML private Label lblWelcome;
    @FXML private Label lblMessage;

    @FXML private TableView<ParkSubmittedReport> tblReports;
    @FXML private TableColumn<ParkSubmittedReport, String> colReportParkId;
    @FXML private TableColumn<ParkSubmittedReport, String> colReportTitle;
    @FXML private TableColumn<ParkSubmittedReport, String> colReportEmployeeId;
    @FXML private TableColumn<ParkSubmittedReport, String> colReportContent;

    @FXML private TableView<Object> tblRequests;
    @FXML private TableColumn<Object, String> colRequest;
    @FXML private TableColumn<Object, String> colStatus;
    @FXML private TableColumn<Object, Void> colApprove;
    @FXML private TableColumn<Object, Void> colReject;

    private Employee employee;
    private Map<Object, String> statusMap = new IdentityHashMap<>();

    @Override
    public void setEmployee(Employee employee) {
        this.employee = employee;
        lblWelcome.setText("Welcome, " + employee.getFirstName() + " " + employee.getLastName() + "!");

        setupReportsTable();
        setupTable();
        listenForRequests();
        requestPendingRequests();
        requestSubmittedReports();
    }

    private void requestSubmittedReports() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        try {
            client.sendToServer(new Message("GET_SUBMITTED_REPORTS", employee.getParkId()));
        } catch (Exception e) {
            showError("Failed to load submitted reports.");
        }
    }

    private void requestPendingRequests() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        try {
            client.sendToServer(new Message("GET_PENDING_REQUESTS", employee.getParkId()));
        } catch (Exception e) {
            showError("Failed to load pending requests.");
        }
    }

    private void setupReportsTable() {
        colReportParkId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getParkId())));

        colReportTitle.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getReportTitle()));

        colReportEmployeeId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getEmployeeId())));

        colReportContent.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContent()));
    }

    private void setupTable() {
        colRequest.setCellValueFactory(data -> {
            Object item = data.getValue();
            String text;

            if (item instanceof ParkChangeRequest) {
                ParkChangeRequest r = (ParkChangeRequest) item;
                text = r.getRequestTitle() + "  |  Park: " + r.getParkId() + "  |  From: " + r.getRequestedByUsername();

            } else if (item instanceof PromotionRequest) {
                PromotionRequest r = (PromotionRequest) item;
                String details = "Code: " + r.getPromoCode() + ", " + r.getPercentage() + "% off";

                if (r.getEndDate() != null) {
                    details += ", until " + r.getEndDate().toLocalDate();
                }

                if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                    details += ", \"" + r.getDescription() + "\"";
                }

                text = "PROMOTION: " + r.getRequestTitle() + "  (" + details + ")  |  Park: " + r.getParkId() + "  |  From: " + r.getRequestedByUsername();

            } else {
                text = "Unknown request";
            }

            return new SimpleStringProperty(text);
        });

        colStatus.setCellValueFactory(data -> {
            String status = statusMap.getOrDefault(data.getValue(), "⏳ Waiting");
            return new SimpleStringProperty(status);
        });

        colApprove.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✔ Approve");

            {
                btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Object item = getTableRow().getItem();
                    if (item == null) return;

                    if (!isItemAlreadyDecided(item)) {
                        sendDecision(item, true);
                    } else {
                        showError("This request has already been processed.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Object rowItem = getTableRow().getItem();
                    btn.setDisable(isItemAlreadyDecided(rowItem));
                    setGraphic(btn);
                }
            }
        });

        colReject.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✘ Reject");

            {
                btn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Object item = getTableRow().getItem();
                    if (item == null) return;

                    if (!isItemAlreadyDecided(item)) {
                        sendDecision(item, false);
                    } else {
                        showError("This request has already been processed.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Object rowItem = getTableRow().getItem();
                    btn.setDisable(isItemAlreadyDecided(rowItem));
                    setGraphic(btn);
                }
            }
        });
    }

    private boolean isItemAlreadyDecided(Object item) {
        String status = statusMap.get(item);
        return status != null && !status.equals("⏳ Waiting");
    }

    private void listenForRequests() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;

        client.setListener(new ServerResponseListener() {
            @Override public void onOrderExistsResult(boolean e) {}
            @Override public void onOrderResult(common.Order o) {}
            @Override public void onUpdateOrderResult(boolean s) {}

            @Override
            public void onError(String msg) {
                Platform.runLater(() -> showError("Error: " + msg));
            }

            @Override
            public void onSubmittedReportsResult(ArrayList<ParkSubmittedReport> reports) {
                Platform.runLater(() -> {
                    tblReports.getItems().clear();
                    tblReports.getItems().addAll(reports);
                });
            }

            @Override
            public void onParkChangeRequestNotification(ParkChangeRequest request) {
                Platform.runLater(() -> {
                    statusMap.put(request, "⏳ Waiting");
                    tblRequests.getItems().add(request);
                    tblRequests.refresh();
                });
            }

            @Override
            public void onPromotionRequestNotification(PromotionRequest request) {
                Platform.runLater(() -> {
                    statusMap.put(request, "⏳ Waiting");
                    tblRequests.getItems().add(request);
                    tblRequests.refresh();
                });
            }
        });
    }

    private void sendDecision(Object requestObj, boolean approved) {
        ParkClient client = ParkClient.getInstance();

        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        try {
            String requestId = null;

            if (requestObj instanceof ParkChangeRequest) {
                ParkChangeRequest request = (ParkChangeRequest) requestObj;
                requestId = request.getRequestId();

                client.sendToServer(new Message("PARK_CHANGE_APPROVAL",
                        new Object[]{request.getRequestId(), approved}));

            } else if (requestObj instanceof PromotionRequest) {
                PromotionRequest request = (PromotionRequest) requestObj;
                requestId = request.getRequestId();

                client.sendToServer(new Message("PROMOTION_APPROVAL",
                        new Object[]{request.getRequestId(), approved}));
            }

            statusMap.put(requestObj, approved ? "✔ Approved" : "✘ Rejected");
            tblRequests.refresh();

            showSuccess((approved ? "Request approved!" : "Request rejected.") +
                    (requestId != null ? " (ID: " + requestId.substring(0, 8) + "...)" : ""));

        } catch (Exception e) {
            showError("Failed to send decision.");
        }
    }

    @FXML
    public void handleVisitsReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gui/employee/VisitorsReportView.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Visits Report");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancellationsReport(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/gui/employee/CancellationsReportView.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Cancellations Report");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
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
}