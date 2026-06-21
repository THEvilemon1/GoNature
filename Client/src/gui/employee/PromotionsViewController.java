package gui.employee;

import client.ParkClient;
import client.ServerResponseListener;
import common.Employee;
import common.Message;
import common.Promotion;
import common.PromotionRequest;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PromotionsViewController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;

    @FXML private TableView<Promotion> tblPromotions;
    @FXML private TableColumn<Promotion, String> colPromoCode;
    @FXML private TableColumn<Promotion, String> colPercentage;
    @FXML private TableColumn<Promotion, String> colEndDate;
    @FXML private TableColumn<Promotion, String> colDescription;
    @FXML private TableColumn<Promotion, Void> colEdit;
    @FXML private TableColumn<Promotion, Void> colDelete;

    @FXML private TextField txtPromoCode;
    @FXML private TextField txtPercentage;
    @FXML private TextField txtEndDate;
    @FXML private TextField txtDescription;

    @FXML private Button btnSubmit;
    @FXML private Button btnCancelEdit;

    private Employee employee;
    private Promotion editingPromotion = null; // null = adding new

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void setEmployee(Employee employee) {
        this.employee = employee;
        setupTable();
        registerListener();
        loadPromotions();
    }

    private void setupTable() {
        colPromoCode.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getPromoCode())));

        colPercentage.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPercentage() + "%"));

        colEndDate.setCellValueFactory(data -> {
            LocalDateTime end = data.getValue().getEndDate();
            return new SimpleStringProperty(end != null ? end.toLocalDate().format(DATE_FORMAT) : "");
        });

        colDescription.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDescription()));

        colEdit.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✎ Edit");
            {
                btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Promotion item = getTableRow().getItem();
                    if (item != null) {
                        beginEditPromotion(item);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : btn);
            }
        });

        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✘ Delete");
            {
                btn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Promotion item = getTableRow().getItem();
                    if (item != null) {
                        requestDelete(item);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : btn);
            }
        });
    }

    /**
     * Registers ONE persistent listener for the lifetime of this screen.
     * This avoids race conditions caused by swapping listeners back and forth,
     * which previously caused server responses to be silently dropped.
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
            public void onPromotionsListResult(java.util.ArrayList<Promotion> promotions) {
                Platform.runLater(() -> tblPromotions.getItems().setAll(promotions));
            }

            @Override
            public void onPromotionRequestResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        showSuccess("Request sent! Waiting for department manager approval.");
                        resetForm();
                    } else {
                        showError("Department manager is not online. Please try again later.");
                    }
                });
            }

            @Override
            public void onPromotionApprovalResult(String requestId, boolean approved) {
                Platform.runLater(() -> {
                    if (approved) {
                        showSuccess("Promotion request was APPROVED and applied!");
                        loadPromotions();
                    } else {
                        showError("Promotion request was REJECTED by the department manager.");
                    }
                });
            }
        });
    }

    private void loadPromotions() {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) return;
        try {
            client.sendToServer(new Message("GET_PROMOTIONS", employee.getParkId()));
        } catch (Exception e) {
            showError("Failed to load promotions.");
        }
    }

    private void beginEditPromotion(Promotion promotion) {
        editingPromotion = promotion;
        lblFormTitle.setText("EDIT PROMOTION #" + promotion.getPromoCode());
        txtPromoCode.setText(String.valueOf(promotion.getPromoCode()));
        txtPercentage.setText(String.valueOf(promotion.getPercentage()));
        txtEndDate.setText(promotion.getEndDate() != null ? promotion.getEndDate().toLocalDate().format(DATE_FORMAT) : "");
        txtDescription.setText(promotion.getDescription());
        btnSubmit.setText("Submit Update Request");
        btnCancelEdit.setVisible(true);
        btnCancelEdit.setManaged(true);
    }

    @FXML
    public void handleCancelEdit(ActionEvent event) {
        resetForm();
    }

    private void resetForm() {
        editingPromotion = null;
        lblFormTitle.setText("ADD NEW PROMOTION");
        txtPromoCode.clear();
        txtPercentage.clear();
        txtEndDate.clear();
        txtDescription.clear();
        btnSubmit.setText("Submit Add Request");
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
    }

    @FXML
    public void handleSubmit(ActionEvent event) {
        clearMessage();

        String codeText = txtPromoCode.getText().trim();
        String percentText = txtPercentage.getText().trim();
        String dateText = txtEndDate.getText().trim();
        String description = txtDescription.getText().trim();

        if (codeText.isEmpty() || percentText.isEmpty() || dateText.isEmpty() || description.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        int promoCode, percentage;
        LocalDate endDate;
        try {
            promoCode = Integer.parseInt(codeText);
            percentage = Integer.parseInt(percentText);
        } catch (NumberFormatException e) {
            showError("Promo code and percentage must be numbers.");
            return;
        }
        if (percentage < 0 || percentage > 100) {
            showError("Percentage must be between 0 and 100.");
            return;
        }
        try {
            endDate = LocalDate.parse(dateText, DATE_FORMAT);
        } catch (Exception e) {
            showError("End date must be in format yyyy-MM-dd.");
            return;
        }
        if (!endDate.isAfter(LocalDate.now())) {
            showError("End date must be in the future.");
            return;
        }

        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        PromotionRequest.ActionType actionType = (editingPromotion == null)
            ? PromotionRequest.ActionType.ADD
            : PromotionRequest.ActionType.UPDATE;

        int promotionId = (editingPromotion != null) ? editingPromotion.getPromotionId() : 0;

        PromotionRequest request = new PromotionRequest(
            UUID.randomUUID().toString(),
            actionType,
            promotionId,
            employee.getParkId(),
            promoCode,
            percentage,
            endDate.atStartOfDay(),
            description,
            employee.getUsername(),
            employee.getEmployeeId(),
            (actionType == PromotionRequest.ActionType.ADD ? "Add promotion #" : "Update promotion #") + promoCode
        );

        try {
            client.sendToServer(new Message("PROMOTION_REQUEST", request));
        } catch (Exception e) {
            showError("Failed to send request.");
        }
    }

    private void requestDelete(Promotion promotion) {
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            showError("Not connected to server.");
            return;
        }

        PromotionRequest request = new PromotionRequest(
            UUID.randomUUID().toString(),
            PromotionRequest.ActionType.DELETE,
            promotion.getPromotionId(),
            employee.getParkId(),
            promotion.getPromoCode(),
            promotion.getPercentage(),
            promotion.getEndDate(),
            promotion.getDescription(),
            employee.getUsername(),
            employee.getEmployeeId(),
            "Delete promotion #" + promotion.getPromoCode()
        );

        try {
            client.sendToServer(new Message("PROMOTION_REQUEST", request));
        } catch (Exception e) {
            showError("Failed to send request.");
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) tblPromotions.getScene().getWindow();
        stage.close();
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