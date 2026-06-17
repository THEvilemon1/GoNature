package server;

import common.Booking;
import gui.ServerPortFrameController;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Fires every day at 23:59 and cancels bookings that were never paid/completed.
public class EndOfDayScheduler {

    private static final LocalTime FIRE_AT = LocalTime.of(23, 59, 59);
    private static final long DAY_SECONDS = TimeUnit.DAYS.toSeconds(1);

    private static final String SAFE_STATUS =
        "'CHECKED_IN','CHECKED_OUT',"              +
        "'" + Booking.STATUS_CANCELLED     + "'," +
        "'" + Booking.STATUS_SYSTEM_CANCEL + "'";

    private static EndOfDayScheduler instance;

    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EndOfDayScheduler");
            t.setDaemon(true);
            return t;
        });

    private final AtomicBoolean started = new AtomicBoolean(false);

    private EndOfDayScheduler() {}

    public static EndOfDayScheduler getInstance() {
        if (instance == null) instance = new EndOfDayScheduler();
        return instance;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            log("Already started.");
            return;
        }

        long initialDelay = secondsUntilFireTime();
        executor.scheduleAtFixedRate(
            this::cancelUnpaidBookings,
            initialDelay,
            DAY_SECONDS,
            TimeUnit.SECONDS
        );

        log("Started. First run at: " + LocalDateTime.now().plusSeconds(initialDelay));
    }

    public void stop() {
        executor.shutdown();
        started.set(false);
    }

    private long secondsUntilFireTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = LocalDate.now().atTime(FIRE_AT);
        if (!now.isBefore(target)) target = target.plusDays(1);
        return ChronoUnit.SECONDS.between(now, target);
    }

    private void cancelUnpaidBookings() {
        log("Running for " + LocalDate.now() + "...");
        try {
            Connection conn = DBConnection.getStaticConnection();

            String countSql =
                "SELECT COUNT(*) FROM booking " +
                "WHERE DATE(visitorTime) = CURDATE() " +
                "  AND status NOT IN (" + SAFE_STATUS + ")";
            ResultSet rs = conn.createStatement().executeQuery(countSql);
            int count = rs.next() ? rs.getInt(1) : 0;

            if (count == 0) {
                log("No bookings to cancel.");
                return;
            }

            String updateSql =
                "UPDATE booking SET status = '" + Booking.STATUS_SYSTEM_CANCEL + "' " +
                "WHERE DATE(visitorTime) = CURDATE() " +
                "  AND status NOT IN (" + SAFE_STATUS + ")";
            conn.createStatement().executeUpdate(updateSql);

            log(count + " booking(s) moved to SYSTEM_CANCEL.");

        } catch (Exception e) {
            System.err.println("[EndOfDayScheduler] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("[EndOfDayScheduler] " + message);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.log("[EndOfDayScheduler] " + message);
    }
}

