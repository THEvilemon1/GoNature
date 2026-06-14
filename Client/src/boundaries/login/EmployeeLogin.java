package boundaries.login;

import java.io.IOException;
import client.ParkClient;
import common.Message;

/**
 * EmployeeLogin (ECB - Boundary Layer)
 *
 * Handles login logic specifically for park employees.
 * Employees authenticate with their username and password.
 * Implements the Login interface.
 */
public class EmployeeLogin implements Login {

    private final String username;
    private final String password;

    public EmployeeLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Employee validation rules:
     *  - username must not be blank
     *  - password must not be blank
     */
    @Override
    public boolean validateFields() {
        if (username == null || username.isBlank()) {
            System.out.println("[EmployeeLogin] Validation failed: username is empty.");
            return false;
        }
        if (password == null || password.isBlank()) {
            System.out.println("[EmployeeLogin] Validation failed: password is empty.");
            return false;
        }
        return true;
    }

    /**
     * Sends an employee login request to the server.
     * Sends username and password as a String array inside a Message.
     */
    @Override
    public void logInUser() {
        System.out.println("[EmployeeLogin] Sending employee login request for username: " + username);
        ParkClient client = ParkClient.getInstance();
        if (client == null || !client.isConnected()) {
            System.out.println("[EmployeeLogin] Failed: client is not connected.");
            return;
        }

        try {
            String[] credentials = {username, password};
            client.sendToServer(new Message("EMPLOYEE_LOGIN", credentials));
        } catch (IOException e) {
            System.out.println("[EmployeeLogin] Failed to send login request: " + e.getMessage());
        }
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}