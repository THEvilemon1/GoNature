package server;

import gui.ServerPortFrameController;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ReminderScheduler (Server-side Singleton)
 *
 * Runs a background task every CHECK_INTERVAL_SECONDS seconds.
 * Each tick it queries the DB for PENDING or CONFIRMED bookings whose
 * visitorTime falls inside the window:
 *
 *   [NOW + 55 min  …  NOW + 65 min]
 *
 * — i.e. "due in roughly one hour" — and whose reminder has not yet been sent.
 *
 * When such a booking is found:
 *   1. NotificationService.sendBookingReminder() is called  (simulates SMS + Email)
 *   2. The booking's reminder_sent flag is set to 1 in the DB
 *      so it is never processed again, even after a server restart.
 *
 * The column reminder_sent is created automatically if it does not exist.
 */
public class ReminderScheduler {

    // --- Tuning constants ----------------------------------------------------

    /** How many minutes before visitorTime the reminder is sent. */
    private static final int REMINDER_MINUTES_BEFORE = 60;

    /**
     * Half-width of the detection window (minutes).
     * The scheduler fires every CHECK_INTERVAL_SECONDS, so the window must be
     * at least as wide as that interval converted to minutes.
     * A ±5-minute window is safe for a 60-second polling interval and also
     * lets the scheduler recover bookings it missed during a brief downtime.
     */
    private static final int WINDOW_HALF_WIDTH_MINUTES = 5;

    /** How often the scheduler wakes up and checks the DB. */
    private static final int CHECK_INTERVAL_SECONDS = 60;

    // -------------------------------------------------------------------------

    private static ReminderScheduler instance;

    /** Single-thread executor — a daemon so it doesn't block JVM shutdown. */
    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ReminderScheduler");
            t.setDaemon(true);
            return t;
        });

    private ReminderScheduler() {}

    /** Returns the singleton instance (lazy, not thread-safe at startup — call once from main thread). */
    public static ReminderScheduler getInstance() {
        if (instance == null) {
            instance = new ReminderScheduler();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Public lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the scheduler.
     * Safe to call only once. Also ensures the reminder_sent DB column exists.
     */
    public void start() {
        ensureReminderSentColumn();
        executor.scheduleAtFixedRate(
            this::checkAndSendReminders,
            0,
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        log("Started — checking every " + CHECK_INTERVAL_SECONDS + "s "
            + "for bookings due in " + REMINDER_MINUTES_BEFORE + " min "
            + "(window ±" + WINDOW_HALF_WIDTH_MINUTES + " min).");
    }

    /** Gracefully shuts down the scheduler. */
    public void stop() {
        executor.shutdown();
        log("Stopped.");
    }

    // -------------------------------------------------------------------------
    // Core logic — runs on the scheduler thread every CHECK_INTERVAL_SECONDS
    // -------------------------------------------------------------------------

    private void checkAndSendReminders() {
        try {
            Connection conn = DBConnection.getStaticConnection();

            // Lower bound: REMINDER_MINUTES_BEFORE - WINDOW_HALF_WIDTH (e.g. NOW + 55 min)
            // Upper bound: REMINDER_MINUTES_BEFORE + WINDOW_HALF_WIDTH (e.g. NOW + 65 min)
            LocalDateTime now = LocalDateTime.now();
            Timestamp lowerBound = Timestamp.valueOf(
                now.plusMinutes(REMINDER_MINUTES_BEFORE - WINDOW_HALF_WIDTH_MINUTES));
            Timestamp upperBound = Timestamp.valueOf(
                now.plusMinutes(REMINDER_MINUTES_BEFORE + WINDOW_HALF_WIDTH_MINUTES));

            String sql =
                "SELECT booking_id, traveler_id, park_id, visitorTime " +
                "FROM booking " +
                "WHERE status = 'CONFIRMED' " +
                "  AND reminder_sent = 0 " +
                "  AND visitorTime BETWEEN ? AND ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, lowerBound);
            ps.setTimestamp(2, upperBound);

            ResultSet rs = ps.executeQuery();

            int sentCount = 0;
            while (rs.next()) {
                String bookingId   = rs.getString("booking_id");
                String travelerId  = rs.getString("traveler_id");
                int    parkId      = rs.getInt("park_id");
                LocalDateTime visitorTime = rs.getTimestamp("visitorTime").toLocalDateTime();

                // 1. Simulate sending SMS + Email
                NotificationService.sendBookingReminder(bookingId, travelerId, visitorTime, parkId);

                // 2. Persist the flag so this booking is never reminded again
                markReminderSent(conn, bookingId);
                sentCount++;
            }

            if (sentCount > 0) {
                log("Reminders sent for " + sentCount + " booking(s).");
            }

        } catch (Exception e) {
            System.err.println("[ReminderScheduler] Error during reminder check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // DB helpers
    // -------------------------------------------------------------------------

    /**
     * Sets reminder_sent = 1 for the given booking so it is never reminded again.
     */
    private void markReminderSent(Connection conn, String bookingId) throws SQLException {
        String sql = "UPDATE booking SET reminder_sent = 1 WHERE booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ps.executeUpdate();
    }

    /**
     * Checks whether the reminder_sent column exists in the booking table,
     * and creates it (DEFAULT 0) if it does not.
     *
     * This lets the scheduler work without a manual DB migration step.
     */
    private void ensureReminderSentColumn() {
        try {
            Connection conn = DBConnection.getStaticConnection();
            DatabaseMetaData meta = conn.getMetaData();

            // getColumns returns a row if the column exists
            ResultSet cols = meta.getColumns(null, null, "booking", "reminder_sent");
            if (!cols.next()) {
                String alterSql =
                    "ALTER TABLE booking " +
                    "ADD COLUMN reminder_sent TINYINT(1) NOT NULL DEFAULT 0";
                conn.createStatement().executeUpdate(alterSql);
                log("Column 'reminder_sent' added to booking table.");
            } else {
                log("Column 'reminder_sent' already exists — OK.");
            }
        } catch (SQLException e) {
            System.err.println("[ReminderScheduler] Could not ensure 'reminder_sent' column: "
                + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    private void log(String message) {
        System.out.println("[ReminderScheduler] " + message);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.log("[ReminderScheduler] " + message);
    }
}
