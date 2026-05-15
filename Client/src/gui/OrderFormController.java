package gui;

import java.sql.Date;
import java.net.URL;
import java.util.ResourceBundle;

import client.ParkClient;
import client.ServerResponseListener;
import common.Message;
import common.Order;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class OrderFormController implements Initializable {

    private Order order;
    private ParkClient client;

    @FXML
    private TextField txtOrderNumber;
    @FXML
    private TextField txtOrderDate;
    @FXML
    private TextField txtNumberOfVisitors;
    @FXML
    private TextField txtConfirmationCode;
    @FXML
    private TextField txtSubscriberId;
    @FXML
    private TextField txtDateOfPlacingOrder;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnClose;

    // Called after navigating from the search screen
    public void loadOrder(Order o, ParkClient parkClient) {
        this.order = o;
        this.client = parkClient;

        txtOrderNumber.setText(String.valueOf(o.getOrderNumber()));
        txtOrderDate.setText(o.getOrderDate().toString());
        txtNumberOfVisitors.setText(String.valueOf(o.getNumberOfVisitors()));
        txtConfirmationCode.setText(String.valueOf(o.getConfirmationCode()));
        txtSubscriberId.setText(String.valueOf(o.getSubscriberId()));
        txtDateOfPlacingOrder.setText(
            o.getDateOfPlacingOrder() != null ? o.getDateOfPlacingOrder().toString() : ""
        );

        // Order number is not editable
        txtOrderNumber.setEditable(false);
        txtConfirmationCode.setEditable(false);
        txtSubscriberId.setEditable(false);
        txtDateOfPlacingOrder.setEditable(false);
    }

    // Save button — send updated order to server
    public void Save(ActionEvent event) throws Exception {
        String newDateStr = txtOrderDate.getText().trim();
        String newVisitorsStr = txtNumberOfVisitors.getText().trim();

        if (newDateStr.isEmpty() || newVisitorsStr.isEmpty()) {
            lblStatus.setText("Please fill in all fields.");
            return;
        }

        try {
            Date newDate = Date.valueOf(newDateStr); // expects YYYY-MM-DD
            int newVisitors = Integer.parseInt(newVisitorsStr);

            Order updatedOrder = new Order(
                order.getOrderNumber(),
                newDate,
                newVisitors,
                order.getConfirmationCode(),
                order.getSubscriberId(),
                order.getDateOfPlacingOrder()
            );

            // Set up listener for server response
            client.setListener(new ServerResponseListener() {
                @Override
                public void onOrderExistsResult(boolean exists) {}

                @Override
                public void onOrderResult(Order order) {}

                @Override
                public void onUpdateOrderResult(boolean success) {
                    Platform.runLater(() -> {
                        if (success) {
                            lblStatus.setText("Order updated successfully.");
                        } else {
                            lblStatus.setText("ERROR: Update failed.");
                        }
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    Platform.runLater(() -> lblStatus.setText("Server error: " + errorMsg));
                }
            });

            client.sendToServer(new Message("UPDATE_ORDER", updatedOrder));

        } catch (IllegalArgumentException e) {
            lblStatus.setText("Invalid date format. Use YYYY-MM-DD.");
        }
    }

    // Close button — go back to search screen
    public void closeWindow(ActionEvent event) throws Exception {
        ((Node) event.getSource()).getScene().getWindow().hide();
        Stage primaryStage = new Stage();
        ParkClientController searchController = new ParkClientController();
        searchController.start(primaryStage);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblStatus.setText("");
    }
}