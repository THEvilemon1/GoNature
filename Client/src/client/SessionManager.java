package client;

import common.VisitorLoginResult;

/**
 * SessionManager (Singleton)
 * 
 * Tracks the currently logged-in user throughout the entire application.
 * Any controller can check who's logged in by calling:
 *   SessionManager.getInstance().getCurrentUser()
 * 
 * Usage:
 *   // After successful login:
 *   SessionManager.getInstance().setCurrentUser(visitorLoginResult);
 *   
 *   // In any controller:
 *   VisitorLoginResult user = SessionManager.getInstance().getCurrentUser();
 *   if (user != null) {
 *       System.out.println("Logged in as: " + user.getTravelerId());
 *   }
 *   
 *   // On logout:
 *   SessionManager.getInstance().logout();
 */
public class SessionManager {
    
    private static SessionManager instance;
    private VisitorLoginResult currentUser;
    
    private SessionManager() {
        // Private constructor — only accessible via getInstance()
    }
    
    /**
     * Get or create the singleton instance.
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Store the logged-in user after successful authentication.
     */
    public void setCurrentUser(VisitorLoginResult user) {
        this.currentUser = user;
    }
    
    /**
     * Get the currently logged-in user.
     * Returns null if no one is logged in.
     */
    public VisitorLoginResult getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Get the traveler ID of the logged-in user.
     * Returns null if no one is logged in.
     */
    public String getTravelerId() {
        return currentUser != null ? currentUser.getTravelerId() : null;
    }
    
    /**
     * Get the national ID of the logged-in user.
     * Returns null if no one is logged in.
     */
    public String getNationalId() {
        return currentUser != null ? currentUser.getNationalId() : null;
    }
    
    /**
     * Clear the session (logout).
     */
    public void logout() {
        currentUser = null;
    }
}
