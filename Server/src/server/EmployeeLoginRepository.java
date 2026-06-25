package server;

import common.Employee;
import java.sql.*;

/**
 * EmployeeLoginRepository
 * Handles all DB operations related to employee login/logout.
 */
public class EmployeeLoginRepository {

    /**
     * Attempts to log in an employee.
     * Returns Employee object if successful.
     * Returns null if credentials are wrong.
     */
    public static Object loginEmployee(String username, String password) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        String sql = "SELECT e.employee_id, e.salary, e.park_id, u.username, e.role, e.user_id, " +
             "u.firstName, u.lastName, u.email, u.phoneNumber, " +
             "p.maxCapacity AS maxCapacity, p.gap AS gap, p.defaultStayTime AS defaultStayTime " +
             "FROM employee e " +
             "JOIN user u ON e.user_id = u.user_id " +
             "LEFT JOIN park p ON e.park_id = p.park_id " +
             "WHERE u.username = ? AND u.password = ?";
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
            System.out.println("[EmployeeLoginRepository] Employee logged in: " + username);
            return employee;
        }
        return null;
    }

    /**
     * Logs out an employee.
     */
    public static void logoutEmployee(String username) {
        System.out.println("[EmployeeLoginRepository] Employee logged out: " + username);
    }

    private static Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
