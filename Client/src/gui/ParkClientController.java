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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ParkClientController {

    private ParkClient client;

    @FXML private Button btnExit = null;
    @FXML private Button btnSend = null;
    @FXML private TextField idtxt;
    @FXML private Label lblStatus;

    private String getID() {
        return idtxt.getText();
    }

    public void setClient(ParkClient parkClient) {
        this.client = parkClient;
    }

    public void Send(ActionEvent event) throws Exception {
        // Clear previous status
        lblStatus.setText("");
        lblStatus.setStyle("");

        String orderNumberStr = getID().trim();

        if (orderNumberStr.isEmpty()) {
            lblStatus.setText("You must enter an order number.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        int orderNumber;
        try {
            orderNumber = Integer.parseInt(orderNumberStr);
        } catch (NumberFormatException e) {
            lblStatus.setText("Order number must be a number.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        lblStatus.setText("Searching...");
        lblStatus.setStyle("-fx-text-fill: orange;");

        client.setListener(new ServerResponseListener() {
            @Override
            public void onOrderExistsResult(boolean exists) {
                Platform.runLater(() -> {
                    if (!exists) {
                        lblStatus.setText("Order not found.");
                        lblStatus.setStyle("-fx-text-fill: red;");
                    }
                });
            }

            @Override
            public void onOrderResult(Order order) {
                Platform.runLater(() -> {
                    try {
                        ((Node) event.getSource()).getScene().getWindow().hide();
                        FXMLLoader loader = new FXMLLoader();
                        Pane root = loader.load(
                            getClass().getResource("/gui/OrderForm.fxml").openStream()
                        );
                        OrderFormController orderFormController = loader.getController();
                        orderFormController.loadOrder(order, client);

                        Stage primaryStage = new Stage();
                        Scene scene = new Scene(root);
                        java.net.URL css = getClass().getResource("/gui/OrderForm.css");
                        if (css != null) scene.getStylesheets().add(css.toExternalForm());
                        primaryStage.setTitle("Order Management Tool");
                        primaryStage.setScene(scene);

                        primaryStage.setOnCloseRequest(e -> {
                            System.exit(0);
                        });

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
                Platform.runLater(() -> {
                    lblStatus.setText("Order not found.");
                    lblStatus.setStyle("-fx-text-fill: red;");
                });
            }
        });

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
        System.exit(0);
    }
}