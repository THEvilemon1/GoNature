package server;

import common.Employee;
import java.sql.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * EmployeeLoginRepository
 * Handles all DB operations related to employee login/logout.
 * Tracks logged-in employees to prevent double login.
 */
public class EmployeeLoginRepository {

    // Thread-safe set of currently logged-in usernames
    private static final Set<String> loggedInEmployees =
        Collections.synchronizedSet(new HashSet<>());

    /**
     * Attempts to log in an employee.
     * Returns Employee object if successful.
     * Returns "ALREADY_LOGGED_IN" if user is already logged in.
     * Returns null if credentials are wrong.
     */
    public static Object loginEmployee(String username, String password) throws SQLException {
        if (loggedInEmployees.contains(username)) {
            return "ALREADY_LOGGED_IN";
        }

        Connection conn = DBConnection.getStaticConnection();
String sql = "SELECT e.employee_id, e.salary, e.park_id, u.username, e.role, e.user_id, " +
             "u.firstName, u.lastName, u.email, u.phoneNumber " +
             "FROM employee e, user u " +
             "WHERE e.user_id = u.user_id AND u.username = ? AND u.password = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Employee employee = new Employee(
                rs.getInt("employee_id"),
                rs.getInt("salary"),
                rs.getInt("park_id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("user_id"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                rs.getString("email"),
                rs.getString("phoneNumber"),
                getNullableInt(rs, "maxCapacity"),
                getNullableInt(rs, "gap"),
                getNullableInt(rs, "defaultStayTime")
            );
            loggedInEmployees.add(username);
            System.out.println("[EmployeeLoginRepository] Employee logged in: " + username);
            return employee;
        }
        return null;
    }

    /**
     * Logs out an employee.
     */
    public static void logoutEmployee(String username) {
        loggedInEmployees.remove(username);
        System.out.println("[EmployeeLoginRepository] Employee logged out: " + username);
    }

    /**
     * Check if an employee is currently logged in.
     */
    public static boolean isLoggedIn(String username) {
        return loggedInEmployees.contains(username);
    }

    private static Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
