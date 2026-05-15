package client;

import java.sql.Date;
import java.util.Scanner;
import common.Message;
import common.Order;

public class ParkClientMain {
    
    // Volatile flags to handle thread synchronization safely
    private static volatile boolean responseArrived = false;
    private static boolean lastExistsResult = false;
    private static boolean lastUpdateResult = false;

    public static void main(String[] args) {
        boolean orderFound = false;
        
        try {
            ParkClient client = new ParkClient("localhost", 5555);
            
            // Set up the listener to catch asynchronous messages from the server
            client.setListener(new ServerResponseListener() {
                @Override
                public void onOrderExistsResult(boolean exists) {
                    lastExistsResult = exists;
                    responseArrived = true;
                }

                @Override
                public void onUpdateOrderResult(boolean success) {
                    lastUpdateResult = success;
                    responseArrived = true;
                }

                @Override
                public void onError(String errorMsg) {
                    System.out.println("\nServer Error: " + errorMsg);
                    responseArrived = true;
                }
            });

            client.openConnection();
            Scanner scanner = new Scanner(System.in);
            int orderNumber = 0;

            // Step 1: Ask server if order exists
            while (!orderFound) {                
                System.out.print("Enter order number: ");
                orderNumber = scanner.nextInt();
                
                responseArrived = false;
                client.sendToServer(new Message("CHECK_ORDER_EXISTS", orderNumber));
                
                // Wait for background thread to update responseArrived
                while (!responseArrived) {
                    Thread.sleep(100);
                }
                
                if (!lastExistsResult) {
                    System.out.println("ERROR: Order number " + orderNumber + " does not exist.");
                } else {
                    orderFound = true;
                }
            }

            // Step 2: Order exists -> ask for new details
            System.out.print("Enter new order date (YYYY-MM-DD): ");
            String newDateStr = scanner.next();

            System.out.print("Enter new number of visitors: ");
            int newVisitors = scanner.nextInt();

            Order updatedOrder = new Order(
                    orderNumber,
                    Date.valueOf(newDateStr),
                    newVisitors,
                    0,
                    0,
                    null
            );

            // Step 3: Send update request
            responseArrived = false;
            client.sendToServer(new Message("UPDATE_ORDER", updatedOrder));

            while (!responseArrived) {
                Thread.sleep(100);
            }

            if (lastUpdateResult) {
                System.out.println("SUCCESS: Order updated successfully.");
            } else {
                System.out.println("ERROR: Update failed.");
            }

            scanner.close();
            client.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
