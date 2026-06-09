package boundaries.login;

/**
 * TravelerLoginAndRegister (ECB - Boundary Layer)
 *
 * Handles login AND first-time registration for park visitors (travelers).
 * Travelers identify with their Israeli ID number.
 * If their ID exists in the DB → login.  If not → auto-register then login.
 *
 * Implements the Login interface.
 */
public class TravelerLoginAndRegister implements Login {

    private final String travelerId; // Israeli 9-digit ID number

    /**
     * @param travelerId the traveler's Israeli ID number (9 digits)
     */
    public TravelerLoginAndRegister(String travelerId) {
        this.travelerId = travelerId;
    }

    // -------------------------------------------------------------------------
    // Login interface implementation
    // -------------------------------------------------------------------------

    /**
     * Traveler validation rules:
     *  - travelerId must not be blank
     *  - travelerId must be exactly 9 digits (Israeli ID format)
     */
    @Override
    public boolean validateFields() {
        if (travelerId == null || travelerId.isBlank()) {
            System.out.println("[TravelerLoginAndRegister] Validation failed: ID is empty.");
            return false;
        }
        if (!travelerId.matches("\\d{9}")) {
            System.out.println("[TravelerLoginAndRegister] Validation failed: ID must be exactly 9 digits.");
            return false;
        }
        return true;
    }

    /**
     * Sends a traveler login/register request to the server.
     * The server decides whether this is a new registration or an existing login.
     */
    @Override
    public void logInUser() {
        System.out.println("[TravelerLoginAndRegister] Sending traveler login/register request for ID: " + travelerId);
        // TODO: Build a Message object and send via client:
        //   Message msg = new Message("TRAVELER_LOGIN", travelerId);
        //   ParkClient.getClient().sendToServer(msg);
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    public String getTravelerId() { return travelerId; }
}
