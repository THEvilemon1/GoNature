package gui;

import client.ParkClient;
import client.ParkClientMain;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;

public class ConnectToServerController {

    private static final int PORT = 5555;

    @FXML private TextField txtIP;
    @FXML private Label lblStatus;
    @FXML private Button btnConnect;
    @FXML private Button btnExit;

    public void connect(ActionEvent event) {
        String ip = txtIP.getText().trim();

        if (ip.isEmpty()) {
            lblStatus.setText("Please enter the server IP.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        lblStatus.setText("Connecting...");
        lblStatus.setStyle("-fx-text-fill: orange;");
        btnConnect.setDisable(true);

        new Thread(() -> {
            try {
                ParkClient client = new ParkClient(ip, PORT);
                client.openConnection();
                ParkClientMain.client = client;

                Platform.runLater(() -> {
                    try {
                        ((Node) event.getSource()).getScene().getWindow().hide();
                        FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/gui/ParkClientView.fxml")
                        );
                        Parent root = loader.load();

                        ParkClientController controller = loader.getController();
                        controller.setClient(client);

                        Stage stage = new Stage();
                        Scene scene = new Scene(root);
                        java.net.URL css = getClass().getResource("/gui/ParkClientView.css");
                        if (css != null) scene.getStylesheets().add(css.toExternalForm());
                        stage.setTitle("Park Management Tool");
                        stage.setScene(scene);

                        // X button — Case 2: abrupt disconnect
                        stage.setOnCloseRequest(e -> System.exit(0));

                        stage.show();
                    } catch (Exception e) {
                        lblStatus.setText("Failed to open client window.");
                        lblStatus.setStyle("-fx-text-fill: red;");
                        btnConnect.setDisable(false);
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Could not connect to " + ip);
                    lblStatus.setStyle("-fx-text-fill: red;");
                    btnConnect.setDisable(false);
                });
            }
        }).start();
    }

    // Exit button — Case 1: orderly disconnect via closeConnection()
    public void exit(ActionEvent event) {
        if (ParkClientMain.client != null && ParkClientMain.client.isConnected()) {
            try {
                ParkClientMain.client.setIntentionalDisconnect();
                ParkClientMain.client.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}