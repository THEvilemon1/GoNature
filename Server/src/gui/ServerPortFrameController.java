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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.Alert;
import server.ParkServerMain;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Enumeration;
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
    @FXML private Label lblServerIP;
    @FXML private Button btnCopyIP;
    @FXML private ListView<String> lstClients;
    @FXML private TextArea txtLog;

    private ObservableList<String> clientList = FXCollections.observableArrayList();
    private Timer uptimeTimer;
    private LocalTime serverStartTime;
    private boolean serverStarted = false;

    public static ServerPortFrameController instance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        lstClients.setItems(clientList);
        log("Server ready. Click Start Server to begin.");
        
        // Load and display WiFi IPv4 address
        loadServerIP();
    }

    public void Done(ActionEvent event) throws Exception {
        if (serverStarted) {
            log("Server is already running on port " + PORT);
            return;
        }

        ParkServerMain.runServer(String.valueOf(PORT));
        serverStarted = true;
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

    /**
     * Get the server's WiFi IPv4 address
     */
    private void loadServerIP() {
        try {
            String ipAddress = getWiFiIPAddress();
            if (ipAddress != null && !ipAddress.isEmpty()) {
                lblServerIP.setText(ipAddress);
            } else {
                lblServerIP.setText("Unable to determine IP");
                lblServerIP.setStyle("-fx-text-fill: #ff6600; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lblServerIP.setText("Error: " + e.getMessage());
            lblServerIP.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            System.err.println("Error loading server IP: " + e.getMessage());
        }
    }

    /**
     * Get WiFi IPv4 address by checking all network interfaces
     */
    private String getWiFiIPAddress() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            
            // Skip loopback and inactive interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            
            // Prefer WiFi/WLAN interfaces
            String interfaceName = networkInterface.getName().toLowerCase();
            if (interfaceName.contains("en") || interfaceName.contains("wlan") || 
                interfaceName.contains("wifi")) {
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // Check for IPv4 addresses (not IPv6)
                    if (!address.isLoopbackAddress() && address.getHostAddress().contains(".")) {
                        return address.getHostAddress();
                    }
                }
            }
        }
        
        // Fallback: get first non-loopback IPv4 address
        Enumeration<NetworkInterface> fallbackInterfaces = NetworkInterface.getNetworkInterfaces();
        while (fallbackInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = fallbackInterfaces.nextElement();
            
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress() && address.getHostAddress().contains(".")) {
                    return address.getHostAddress();
                }
            }
        }
        
        return null;
    }

    /**
     * Copy the server IP address to clipboard
     */
    public void copyIPToClipboard(ActionEvent event) {
        String ip = lblServerIP.getText();
        
        if (ip.equals("Loading...") || ip.equals("Unable to determine IP") || ip.startsWith("Error:")) {
            showAlert("Cannot Copy", "IP address is not available");
            return;
        }
        
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(ip);
            clipboard.setContent(content);
            
            log("Server IP copied to clipboard: " + ip);
            showAlert("Success", "Server IP copied to clipboard:\n" + ip);
        } catch (Exception e) {
            log("Error copying IP to clipboard: " + e.getMessage());
            showAlert("Error", "Failed to copy IP address");
        }
    }

    /**
     * Show a simple alert dialog
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
