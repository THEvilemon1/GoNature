package client;

import java.io.IOException;

import common.Message;
import ocsf.client.AbstractClient;

public class ParkClient extends AbstractClient {
	
	public static boolean lastExistsResult;
	public static boolean lastUpdateResult;
	public static boolean responseArrived = false;
	public static String lastCommand = "";
	
	

    public ParkClient(String host, int port) throws IOException {
        super(host, port);
    }

    @Override
    protected void handleMessageFromServer(Object msg) {

        if (msg instanceof Message) {
            Message message = (Message) msg;

            lastCommand = message.getCommand();

            if (message.getCommand().equals("ORDER_EXISTS_RESULT")) {
                lastExistsResult = (boolean) message.getData();
                responseArrived = true;
            }

            else if (message.getCommand().equals("UPDATE_ORDER_RESULT")) {
                lastUpdateResult = (boolean) message.getData();
                responseArrived = true;
            }

            else if (message.getCommand().equals("ERROR")) {
                System.out.println("ERROR FROM SERVER: " + message.getData());
                responseArrived = true;
            }
        }
    }
}