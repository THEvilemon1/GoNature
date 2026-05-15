package client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParkClientMain extends Application {

    public static ParkClient client;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/gui/ConnectToServer.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/gui/ConnectToServer.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setTitle("Connect to Park Server");
        primaryStage.setScene(scene);

        // Handle X button — orderly disconnect
        primaryStage.setOnCloseRequest(e -> {
            if (client != null && client.isConnected()) {
                try {
                    client.closeConnection();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}