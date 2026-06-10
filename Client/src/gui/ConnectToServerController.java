package gui;

import client.ParkClient;
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
                ParkClient.connect(ip, PORT);

                Platform.runLater(() -> {
                    try {
                        ((Node) event.getSource()).getScene().getWindow().hide();
                        FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/gui/login/LoginPage.fxml")
                        );
                        Parent root = loader.load();

                        Stage stage = new Stage();
                        Scene scene = new Scene(root);
                        java.net.URL css = getClass().getResource("/gui/login/LoginPage.css");
                        if (css != null) scene.getStylesheets().add(css.toExternalForm());
                        stage.setTitle("GoNature - Login");
                        stage.setScene(scene);

                        // X button — Case 2: abrupt disconnect
                        stage.setOnCloseRequest(e -> {
                            try {
                                ParkClient parkClient = ParkClient.getInstance();
                                if (parkClient != null && parkClient.isConnected()) {
                                    parkClient.setIntentionalDisconnect();
                                    parkClient.closeConnection();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            System.exit(0);
                        });

                        stage.show();
                    } catch (Exception e) {
                        lblStatus.setText("Failed to open login window.");
                        lblStatus.setStyle("-fx-text-fill: red;");
                        btnConnect.setDisable(false);
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Could not connect to " + ip + ": " + e.getMessage());
                    lblStatus.setStyle("-fx-text-fill: red;");
                    btnConnect.setDisable(false);
                });
            }
        }).start();
    }

    // Exit button — Case 1: orderly disconnect via closeConnection()
    public void exit(ActionEvent event) {
        ParkClient client = ParkClient.getInstance();
        if (client != null && client.isConnected()) {
            try {
                client.setIntentionalDisconnect();
                client.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}