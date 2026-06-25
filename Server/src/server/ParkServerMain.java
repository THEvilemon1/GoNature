package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ParkServerMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerPort.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/ServerPort.css").toExternalForm());
        primaryStage.setTitle("Park Server");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void runServer(String port) {
        try {
            ParkServer server = new ParkServer(Integer.parseInt(port));
            server.listen();
            System.out.println("Park Server running on port " + port);

            BookingLifecycleService.ensureLifecycleColumns();

            // Start the lifecycle scheduler — waitlist timing, confirmations, reminders, SMS and email
            ReminderScheduler.getInstance().start();

            // Start the end-of-day scheduler — cancels unpaid bookings at 23:59
            EndOfDayScheduler.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}