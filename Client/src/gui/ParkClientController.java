package gui;

import client.ParkClient;
import client.ServerResponseListener;
import common.Message;
import common.Order;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ParkClientController {

    private ParkClient client;

    @FXML
    private Button btnExit = null;

    @FXML
    private Button btnSend = null;

    @FXML
    private TextField idtxt;

    private String getID() {
        return idtxt.getText();
    }

    // Called once when this screen starts, to pass in the connected client
    public void setClient(ParkClient parkClient) {
        this.client = parkClient;
    }

    public void Send(ActionEvent event) throws Exception {
        String orderNumberStr = getID().trim();

        if (orderNumberStr.isEmpty()) {
            System.out.println("You must enter an order number");
            return;
        }

        int orderNumber;
        try {
            orderNumber = Integer.parseInt(orderNumberStr);
        } catch (NumberFormatException e) {
            System.out.println("Order number must be a number");
            return;
        }

        // Set up listener to handle server response
        client.setListener(new ServerResponseListener() {
            @Override
            public void onOrderExistsResult(boolean exists) {
                Platform.runLater(() -> {
                    if (!exists) {
                        System.out.println("ERROR: Order number " + orderNumber + " does not exist.");
                    } else {
                        System.out.println("Order found — waiting for order data...");
                    }
                });
            }

            @Override
            public void onOrderResult(Order order) {
                Platform.runLater(() -> {
                    try {
                        ((Node) event.getSource()).getScene().getWindow().hide();
                        Stage primaryStage = new Stage();
                        FXMLLoader loader = new FXMLLoader();
                        Pane root = loader.load(
                            getClass().getResource("/gui/OrderForm.fxml").openStream()
                        );
                        OrderFormController orderFormController = loader.getController();
                        orderFormController.loadOrder(order, client);

                        Scene scene = new Scene(root);
                        scene.getStylesheets().add(
                            getClass().getResource("/gui/OrderForm.css").toExternalForm()
                        );
                        primaryStage.setTitle("Order Management Tool");
                        primaryStage.setScene(scene);
                        primaryStage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onUpdateOrderResult(boolean success) {}

            @Override
            public void onError(String errorMsg) {
                Platform.runLater(() -> System.out.println("Server error: " + errorMsg));
            }
        });

        // Send request to server
        client.sendToServer(new Message("GET_ORDER", orderNumber));
    }

    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ParkClientView.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/ParkClientView.css").toExternalForm());
        primaryStage.setTitle("Park Management Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void getExitBtn(ActionEvent event) throws Exception {
        if (client != null && client.isConnected()) {
            client.closeConnection();
        }
        System.exit(0);
    }
}