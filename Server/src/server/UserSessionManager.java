package server;

import ocsf.server.ConnectionToClient;
import java.util.HashMap;
import java.util.Map;

/**
 * UserSessionManager (Server-side Singleton)
 * 
 * Tracks which user is currently logged in and from which client connection.
 * Enforces the rule: "One user per computer" - if a user logs in from another
 * computer, the old session is automatically disconnected.
 * 
 * Thread-safe: uses synchronized methods to prevent race conditions.
 */
public class UserSessionManager {
    
    private static UserSessionManager instance;
    
    // Map: travelerId → ConnectionToClient
    private final Map<String, ConnectionToClient> activeSessions = new HashMap<>();
    
    private UserSessionManager() {
        // Private constructor — only accessible via getInstance()
    }
    
    /**
     * Get or create the singleton instance.
     */
    public static synchronized UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }
    
    /**
     * Register a user login from a client connection.
     * If this user is already logged in from another connection, that connection is closed.
     * 
     * @param travelerId the user's traveler ID
     * @param clientConnection the current client connection
     * @return the old connection if user was already logged in, or null if first login
     */
    public synchronized ConnectionToClient loginUser(String travelerId, ConnectionToClient clientConnection) {
        ConnectionToClient oldConnection = activeSessions.get(travelerId);
        
        // If user is already logged in from a different connection, disconnect old session
        if (oldConnection != null && oldConnection != clientConnection) {
            System.out.println("[UserSessionManager] User " + travelerId + 
                " logging in from new connection. Disconnecting old session.");
            activeSessions.put(travelerId, clientConnection);
            return oldConnection; // Return old connection to be closed
        }
        
        // First login or same connection
        activeSessions.put(travelerId, clientConnection);
        System.out.println("[UserSessionManager] User " + travelerId + " logged in from " + 
            clientConnection.getInetAddress().getHostAddress());
        return null;
    }
    
    /**
     * Logout a user by removing them from active sessions.
     * 
     * @param travelerId the user's traveler ID
     */
    public synchronized void logoutUser(String travelerId) {
        activeSessions.remove(travelerId);
        System.out.println("[UserSessionManager] User " + travelerId + " logged out.");
    }
    
    /**
     * Remove a connection from all active sessions (e.g., when client disconnects abruptly).
     * 
     * @param clientConnection the disconnected client connection
     */
    public synchronized void removeConnection(ConnectionToClient clientConnection) {
        activeSessions.values().removeIf(conn -> conn == clientConnection);
    }
    
    /**
     * Get the client connection for a logged-in user.
     * Returns null if user is not logged in.
     * 
     * @param travelerId the user's traveler ID
     * @return the ConnectionToClient or null
     */
    public synchronized ConnectionToClient getConnection(String travelerId) {
        return activeSessions.get(travelerId);
    }
    
    /**
     * Check if a user is currently logged in.
     * 
     * @param travelerId the user's traveler ID
     * @return true if logged in, false otherwise
     */
    public synchronized boolean isLoggedIn(String travelerId) {
        return activeSessions.containsKey(travelerId);
    }
    
    /**
     * Get total number of active sessions.
     */
    public synchronized int getActiveSessionCount() {
        return activeSessions.size();
    }
}
