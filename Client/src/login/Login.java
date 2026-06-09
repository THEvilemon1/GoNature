package login;

/**
 * Login Interface (ECB - Boundary Layer)
 *
 * Defines the contract that every login strategy must fulfill.
 * Both EmployeeLogin and TravelerLoginAndRegister implement this.
 */
public interface Login {

    /**
     * Validates that the input fields contain acceptable values
     * before attempting authentication.
     *
     * @return true if all fields are valid, false otherwise
     */
    boolean validateFields();

    /**
     * Performs the actual login operation for the specific user type.
     * Called only after validateFields() returns true.
     */
    void logInUser();
}
