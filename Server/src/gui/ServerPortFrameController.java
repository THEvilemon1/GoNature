package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import server.ParkServerMain;

import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class ServerPortFrameController implements Initializable {

    private static final int PORT = 5555;

    @FXML private Button btnExit;
    @FXML private Button btnDone;
    @FXML private Label lblServerStatus;
    @FXML private Label lblDBStatus;
    @FXML private Label lblUptime;
    @FXML private ListView<String> lstClients;
    @FXML private TextArea txtLog;

    private ObservableList<String> clientList = FXCollections.observableArrayList();
    private Timer uptimeTimer;
    private LocalTime serverStartTime;

    public static ServerPortFrameController instance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        lstClients.setItems(clientList);
        log("Server ready. Click Start Server to begin.");
    }

    public void Done(ActionEvent event) throws Exception {
        ParkServerMain.runServer(String.valueOf(PORT));
        serverStartTime = LocalTime.now();

        Platform.runLater(() -> {
            lblServerStatus.setText("Running on port " + PORT);
            lblServerStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            btnDone.setDisable(true);
        });

        log("Server started on port " + PORT);
        startUptimeTimer();
    }

    public void clientConnected(String clientInfo) {
        Platform.runLater(() -> {
            clientList.add(clientInfo);
            log("Client connected: " + clientInfo);
        });
    }

    public void clientDisconnected(String clientInfo) {
        Platform.runLater(() -> {
            clientList.remove(clientInfo);
            log("Client disconnected: " + clientInfo);
        });
    }

    public void updateDBStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                lblDBStatus.setText("Connected");
                lblDBStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblDBStatus.setText("Disconnected (timed out)");
                lblDBStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        });
    }

    public void log(String message) {
        Platform.runLater(() -> {
            String time = LocalTime.now().toString().substring(0, 8);
            txtLog.appendText("[" + time + "] " + message + "\n");
        });
    }

    private void startUptimeTimer() {
        uptimeTimer = new Timer();
        uptimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Duration uptime = Duration.between(serverStartTime, LocalTime.now());
                long hours = uptime.toHours();
                long minutes = uptime.toMinutesPart();
                long seconds = uptime.toSecondsPart();
                String uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                Platform.runLater(() -> lblUptime.setText(uptimeStr));
            }
        }, 0, 1000);
    }

    public void getExitBtn(ActionEvent event) throws Exception {
        if (uptimeTimer != null) uptimeTimer.cancel();
        System.out.println("Exiting Park Server");
        System.exit(0);
    }
}