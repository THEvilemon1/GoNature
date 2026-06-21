package login;

import client.ParkClient;
import common.Message;

/**
 * EmployeeLogin (ECB - Boundary Layer)
 *
 * Handles login logic specifically for park employees.
 * Employees authenticate with their employee ID and password.
 * Implements the Login interface.
 */
public class EmployeeLogin implements Login {

    private final String employeeId;
    private final String password;

    /**
     * @param employeeId the employee's unique identifier
     * @param password   the employee's password (plaintext; hashing done server-side)
     */
    public EmployeeLogin(String employeeId, String password) {
        this.employeeId = employeeId;
        this.password   = password;
    }

    // -------------------------------------------------------------------------
    // Login interface implementation
    // -------------------------------------------------------------------------

    /**
     * Employee validation rules:
     *  - employeeId must not be blank
     *  - password must not be blank
     *  - employeeId must be numeric (employees have numeric IDs in GoNature)
     */
    @Override
    public boolean validateFields() {
        if (employeeId == null || employeeId.isBlank()) {
            System.out.println("[EmployeeLogin] Validation failed: employee ID is empty.");
            return false;
        }
        if (password == null || password.isBlank()) {
            System.out.println("[EmployeeLogin] Validation failed: password is empty.");
            return false;
        }
        if (!employeeId.matches("\\d+")) {
            System.out.println("[EmployeeLogin] Validation failed: employee ID must be numeric.");
            return false;
        }
        return true;
    }

    /**
     * Sends an employee login request to the server via the message bus.
     * The actual network call is delegated to loginController, which holds
     * a reference to the OCSF client.
     */
    @Override
    public void logInUser() {
        System.out.println("[EmployeeLogin] Sending employee login request for ID: " + employeeId);

        try {
            Message msg = new Message("EMPLOYEE_LOGIN",new String[] { employeeId, password});
            ParkClient.getInstance().sendToServer(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Getters (read-only; fields are set once via constructor)
    // -------------------------------------------------------------------------

    public String getEmployeeId() { return employeeId; }
    public String getPassword()   { return password;   }
}
