package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static Connection conn;

    public static Connection getConnection() throws SQLException {

        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/gonature?serverTimezone=UTC",
                "root",
                "amerhmysql"
            );
        }

        return conn;
    }
}