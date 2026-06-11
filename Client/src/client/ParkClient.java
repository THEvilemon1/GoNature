package client;

import java.io.IOException;

import common.Message;
import common.Order;
import common.VisitorLoginResult;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import ocsf.client.AbstractClient;

public class ParkClient extends AbstractClient {

    private ServerResponseListener listener;
    private boolean intentionalDisconnect = false;

    private ParkClient(String host, int port) throws IOException {
        super(host, port);
    }
    
    private static ParkClient instance;
    
    public static synchronized void connect(String host, int port) throws IOException {
        if (instance != null && instance.isConnected()) {
            return;
        }

        ParkClient newInstance = new ParkClient(host, port);
        newInstance.openConnection();
        instance = newInstance;
    }

    public static ParkClient getInstance() { 
        return instance; 
    }

    private static synchronized void clearInstance(ParkClient client) {
        if (instance == client) {
            instance = null;
        }
    }

    public void setListener(ServerResponseListener listener) {
        this.listener = listener;
    }

    // Call this before closing intentionally so we don't show the alert
    public void setIntentionalDisconnect() {
        this.intentionalDisconnect = true;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message && listener != null) {
            Message message = (Message) msg;
            switch (message.getCommand()) {
                case "ORDER_EXISTS_RESULT":
                    listener.onOrderExistsResult((boolean) message.getData());
                    break;
                case "ORDER_RESULT":
                    listener.onOrderResult((Order) message.getData());
                    break;
                case "UPDATE_ORDER_RESULT":
                    listener.onUpdateOrderResult((boolean) message.getData());
                    break;
                case "VISITOR_LOGIN_RESULT":
                    listener.onVisitorLoginResult((VisitorLoginResult) message.getData());
                    break;
                case "FORCE_LOGOUT":
                    // User was logged in from another computer
                    handleForceLogout((String) message.getData());
                    break;
                case "ERROR":
                    listener.onError((String) message.getData());
                    break;
            }
        }
    }

    // Case 1: Orderly disconnect — don't show alert if we closed it ourselves
    @Override
    protected void connectionClosed() {
        clearInstance(this);
        if (!intentionalDisconnect) {
            System.out.println("Server closed the connection.");
            showServerDisconnectedAlert("The server has shut down.");
        }
    }

    // Case 2: Server crashed or lost connection abruptly
    @Override
    protected void connectionException(Exception exception) {
        clearInstance(this);
        if (!intentionalDisconnect) {
            System.out.println("Lost connection to server: " + exception.getMessage());
            showServerDisconnectedAlert("Connection to server was lost.");
        }
    }

    private void showServerDisconnectedAlert(String reason) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Server Disconnected");
            alert.setHeaderText("Connection Lost");
            alert.setContentText(reason + "\nPlease restart the client.");
            alert.showAndWait();
            System.exit(0);
        });
    }

    /**
     * Handle forced logout when the same user logs in from another computer.
     */
    private void handleForceLogout(String reason) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Logged Out");
            alert.setHeaderText("You have been logged out");
            alert.setContentText(reason + "\nPlease log in again.");
            alert.showAndWait();
            
            // Clear session and close connection
            SessionManager.getInstance().logout();
            this.setIntentionalDisconnect();
            try {
                this.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
    }
}
