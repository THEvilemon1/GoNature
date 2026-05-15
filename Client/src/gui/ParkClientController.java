package gui;

import java.io.IOException;
import java.sql.Date;

import client.ParkClient;
import client.ServerResponseListener;
import common.Message;
import common.Order;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class ParkClientController {

    // --- Connection fields ---
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Button connectButton;

    // --- Step 1: Check order ---
    @FXML private TextField orderNumberField;
    @FXML private Button checkOrderButton;

    // --- Step 2: Update order ---
    @FXML private TextField newDateField;
    @FXML private TextField visitorsField;
    @FXML private Button updateOrderButton;

    // --- Status ---
    @FXML private Label statusLabel;

    private ParkClient client;
    private int currentOrderNumber;

    @FXML
    public void initialize() {
        setUpdateSectionDisabled(true);
        checkOrderButton.setDisable(true);
    }

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty() || portText.isEmpty()) {
            setStatus("Please enter host and port.", true);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            setStatus("Port must be a number.", true);
            return;
        }

        try {
            client = new ParkClient(host, port);
            
            // Set up the asynchronous network callback listeners
            client.setListener(new ServerResponseListener() {
                @Override
                public void onOrderExistsResult(boolean exists) {
                    // Update JavaFX components inside Platform.runLater
                    Platform.runLater(() -> {
                        checkOrderButton.setDisable(false);
                        if (exists) {
                            setStatus("Order #" + currentOrderNumber + " found. Fill in new details below.", false);
                            setUpdateSectionDisabled(false);
                        } else {
                            setStatus("Order #" + currentOrderNumber + " does not exist. Try again.", true);
                        }
                    });
                }

                @Override
                public void onUpdateOrderResult(boolean success) {
                    Platform.runLater(() -> {
                        updateOrderButton.setDisable(false);
                        if (success) {
                            setStatus("Order #" + currentOrderNumber + " updated successfully!", false);
                            setUpdateSectionDisabled(true);
                            orderNumberField.clear();
                            newDateField.clear();
                            visitorsField.clear();
                        } else {
                            setStatus("Update failed. Please try again.", true);
                        }
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    Platform.runLater(() -> {
                        checkOrderButton.setDisable(false);
                        updateOrderButton.setDisable(false);
                        setStatus("Server Error: " + errorMsg, true);
                    });
                }
            });

            client.openConnection();
            setStatus("Connected to " + host + ":" + port, false);
            connectButton.setDisable(true);
            hostField.setDisable(true);
            portField.setDisable(true);
            checkOrderButton.setDisable(false);
        } catch (IOException e) {
            setStatus("Connection failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCheckOrder() {
        String orderText = orderNumberField.getText().trim();
        if (orderText.isEmpty()) {
            setStatus("Please enter an order number.", true);
            return;
        }

        try {
            currentOrderNumber = Integer.parseInt(orderText);
        } catch (NumberFormatException e) {
            setStatus("Order number must be a valid integer.", true);
            return;
        }

        checkOrderButton.setDisable(true);
        setUpdateSectionDisabled(true);
        setStatus("Checking order #" + currentOrderNumber + "...", false);

        // Fire-and-forget network transmission. No threads or loops needed here!
        try {
            client.sendToServer(new Message("CHECK_ORDER_EXISTS", currentOrderNumber));
        } catch (IOException e) {
            checkOrderButton.setDisable(false);
            setStatus("Network Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleUpdateOrder() {
        String dateText = newDateField.getText().trim();
        String visitorsText = visitorsField.getText().trim();

        if (dateText.isEmpty() || visitorsText.isEmpty()) {
            setStatus("Please fill in all update fields.", true);
            return;
        }

        Date newDate;
        try {
            newDate = Date.valueOf(dateText);
        } catch (IllegalArgumentException e) {
            setStatus("Date must be in YYYY-MM-DD format.", true);
            return;
        }

        int visitors;
        try {
            visitors = Integer.parseInt(visitorsText);
        } catch (NumberFormatException e) {
            setStatus("Number of visitors must be a valid integer.", true);
            return;
        }

        Order updatedOrder = new Order(currentOrderNumber, newDate, visitors, 0, 0, null);

        updateOrderButton.setDisable(true);
        setStatus("Sending update...", false);

        // Fire-and-forget network transmission. No threads or loops needed here!
        try {
            client.sendToServer(new Message("UPDATE_ORDER", updatedOrder));
        } catch (IOException e) {
            updateOrderButton.setDisable(false);
            setStatus("Network Error: " + e.getMessage(), true);
        }
    }

    private void setUpdateSectionDisabled(boolean disabled) {
        newDateField.setDisable(disabled);
        visitorsField.setDisable(disabled);
        updateOrderButton.setDisable(disabled);
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);
    }
}
