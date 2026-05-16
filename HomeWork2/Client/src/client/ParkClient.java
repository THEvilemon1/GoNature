package client;

import java.io.IOException;

import common.Message;
import common.Order;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import ocsf.client.AbstractClient;

public class ParkClient extends AbstractClient {

    private ServerResponseListener listener;
    private boolean intentionalDisconnect = false;

    public ParkClient(String host, int port) throws IOException {
        super(host, port);
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
                case "ERROR":
                    listener.onError((String) message.getData());
                    break;
            }
        }
    }

    // Case 1: Orderly disconnect — don't show alert if we closed it ourselves
    @Override
    protected void connectionClosed() {
        if (!intentionalDisconnect) {
            System.out.println("Server closed the connection.");
            showServerDisconnectedAlert("The server has shut down.");
        }
    }

    // Case 2: Server crashed or lost connection abruptly
    @Override
    protected void connectionException(Exception exception) {
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
}