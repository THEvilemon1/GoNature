package client;

import boundaries.login.*;

/**
 * loginController (ECB - Control Layer)
 *
 * Coordinates the login flow between the JavaFX UI (LoginPageController)
 * and the login strategy objects (EmployeeLogin / TravelerLoginAndRegister).
 *
 * The lollipop symbol in the class diagram means loginController *uses*
 * the Login interface — it does not care which concrete strategy it holds.
 *
 * Responsibilities:
 *  1. Receive the chosen Login strategy from the UI controller.
 *  2. Validate fields via the strategy.
 *  3. Trigger the login via the strategy.
 *  4. Expose a logout that disconnects all currently logged-in users.
 */
public class loginController {

    /** The concrete login strategy injected at construction time. */
    private final Login loginStrategy;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param login the concrete login strategy to use for this session
     *              (either EmployeeLogin or TravelerLoginAndRegister)
     */
    public loginController(Login login) {
        this.loginStrategy = login;
    }

    // -------------------------------------------------------------------------
    // Public API  (matches the class diagram exactly)
    // -------------------------------------------------------------------------

    /**
     * Entry point called by the JavaFX controller when the user clicks "Login".
     * Validates first; only proceeds to logInUser() if validation passes.
     *
     * @return true if login was attempted, false if validation blocked it
     */
    public boolean login() {
        if (!validateFields()) {
            return false; // UI layer should already show the error message
        }
        loginStrategy.logInUser();
        return true;
    }

    /**
     * Delegates field validation to the current login strategy.
     *
     * @return true if all fields are valid
     */
    public boolean validateFields() {
        return loginStrategy.validateFields();
    }

    /**
     * Logs out the current user and navigates back to the login screen.
     * Called from any screen that has a "Logout" button.
     */
    public void logout() {
        System.out.println("[loginController] Logging out current user.");
        // TODO: clear session state, navigate to LoginPage.fxml
    }

    /**
     * Forcefully disconnects ALL users that are currently logged in.
     * Used by server-side admin operations or emergency shutdown.
     */
    public void logoutLoggedInUsers() {
        System.out.println("[loginController] Logging out ALL logged-in users.");
        // TODO: send LOGOUT_ALL message to server
    }
}
