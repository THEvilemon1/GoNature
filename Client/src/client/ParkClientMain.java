package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParkClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ConnectToServer.fxml"));
        primaryStage.setTitle("GoNature - Connect to Server");
        primaryStage.setScene(new Scene(root));
        WindowUtil.showMaximized(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
