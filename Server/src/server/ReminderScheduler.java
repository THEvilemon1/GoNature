package server;

import gui.ServerPortFrameController;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ReminderScheduler (Server-side Singleton)
 *
 * Runs a background task every CHECK_INTERVAL_SECONDS seconds.
 *
 * All booking timing rules are delegated to BookingLifecycleService so there is
 * one central place for waitlist expiration, spot offers, reminder requests,
 * and automatic system cancellations.
 */
public class ReminderScheduler {

    // --- Tuning constants ----------------------------------------------------

    /** How often the scheduler wakes up and checks the DB. */
    private static final int CHECK_INTERVAL_SECONDS = 5;

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
     * Safe to call only once.
     */
    public void start() {
        BookingLifecycleService.ensureLifecycleColumns();
        executor.scheduleAtFixedRate(
            BookingLifecycleService::processAutomaticTransitions,
            0,
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        log("Started — checking lifecycle rules every " + CHECK_INTERVAL_SECONDS + "s.");
    }

    /** Gracefully shuts down the scheduler. */
    public void stop() {
        executor.shutdown();
        log("Stopped.");
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
