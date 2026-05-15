package client;

import java.io.IOException;

import common.Message;
import ocsf.client.AbstractClient;

public class ParkClient extends AbstractClient {

    private ServerResponseListener listener;

    public ParkClient(String host, int port) throws IOException {
        super(host, port);
    }

    public void setListener(ServerResponseListener listener) {
        this.listener = listener;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message && listener != null) {
            Message message = (Message) msg;
            switch (message.getCommand()) {
                case "ORDER_EXISTS_RESULT":
                    listener.onOrderExistsResult((boolean) message.getData());
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
}