package server;

import gui.ServerPortFrameController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class DBConnection {

    private static DBConnection instance = null;

    private Connection conn;
    private Timer timer;

    private static final long TIMEOUT  = 30*1000; // 30 seconds
    private static final String URL = "jdbc:mysql://localhost:3306/gonaturedb?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
//    private static final String PASSWORD = "amerhmysql";
    //private static final String PASSWORD = "roottoor";
    private static final String PASSWORD = "319Benan@@";

    private DBConnection() throws SQLException {
        openConnection();
    }

    public static DBConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            openConnection();
        }
        resetTimer();
        return conn;
    }

    private void openConnection() throws SQLException {
        conn = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("DB connection opened.");
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.updateDBStatus(true);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.log("Database connected.");
        resetTimer();
    }

    private void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("DB connection closed due to inactivity.");
                if (ServerPortFrameController.instance != null)
                    ServerPortFrameController.instance.updateDBStatus(false);
                if (ServerPortFrameController.instance != null)
                    ServerPortFrameController.instance.log("Database disconnected due to inactivity (30s timeout).");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeConnection();
            }
        }, TIMEOUT);
    }

    public static Connection getStaticConnection() throws SQLException {
        return getInstance().getConnection();
    }
}
