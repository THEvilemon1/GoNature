package client;

import java.sql.Date;
import java.util.Scanner;

import common.Message;
import common.Order;

public class ParkClientMain {
	
	
    public static void main(String[] args) {
    	boolean orderFound = false;
    	
        try {
            ParkClient client = new ParkClient("localhost", 5555);
            client.openConnection();

            Scanner scanner = new Scanner(System.in);
            int orderNumber = 0;
            while (!orderFound) {            	
            	System.out.print("Enter order number: ");
            	orderNumber = scanner.nextInt();
            	// Step 1: ask server if order exists
            	ParkClient.responseArrived = false;
            	client.sendToServer(new Message("CHECK_ORDER_EXISTS", orderNumber));
            	
            	while (!ParkClient.responseArrived) {
            		Thread.sleep(100);
            	}
            	
            	if (!ParkClient.lastExistsResult) {
            		System.out.println("ERROR: Order number " + orderNumber + " does not exist.");
//            		scanner.close();
//            		return;
            	}
            	else {
            		orderFound = true;
            	}
            }

            // Step 2: order exists -> ask for new details
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

            // Step 3: send update request
            ParkClient.responseArrived = false;
            client.sendToServer(new Message("UPDATE_ORDER", updatedOrder));

            while (!ParkClient.responseArrived) {
                Thread.sleep(100);
            }

            if (ParkClient.lastUpdateResult) {
                System.out.println("SUCCESS: Order updated successfully.");
            } else {
                System.out.println("ERROR: Update failed.");
            }

            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}