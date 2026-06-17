package server;

import gui.ServerPortFrameController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NotificationService
 *
 * Simulates sending SMS and Email reminders to travelers.
 * In a real production system, replace the log lines inside sendSMS()
 * and sendEmail() with actual gateway/API calls.
 */
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Entry point called by ReminderScheduler.
     * Sends both an SMS and an Email reminder for a given booking.
     *
     * @param bookingId   the booking's unique ID
     * @param travelerId  the traveler's ID (used to look up contact details in a real system)
     * @param visitorTime the scheduled visit time
     * @param parkId      the park the booking is for
     */
    public static void sendBookingReminder(String bookingId, String travelerId,
                                           LocalDateTime visitorTime, int parkId) {
        sendSMS(bookingId, travelerId, visitorTime, parkId);
        sendEmail(bookingId, travelerId, visitorTime, parkId);
    }

    // -------------------------------------------------------------------------
    // Private helpers — replace bodies with real gateway calls when needed
    // -------------------------------------------------------------------------

    private static void sendSMS(String bookingId, String travelerId,
                                 LocalDateTime visitorTime, int parkId) {
        String message = String.format(
            "[SMS] Reminder to traveler %s: Your visit to Park %d is in 1 hour (at %s). Booking ID: %s",
            travelerId, parkId, visitorTime.format(DISPLAY_FORMAT), bookingId
        );
        log(message);
        // TODO: integrate real SMS gateway (e.g. Twilio) here
    }

    private static void sendEmail(String bookingId, String travelerId,
                                   LocalDateTime visitorTime, int parkId) {
        String message = String.format(
            "[EMAIL] Reminder to traveler %s: Your visit to Park %d is in 1 hour (at %s). Booking ID: %s",
            travelerId, parkId, visitorTime.format(DISPLAY_FORMAT), bookingId
        );
        log(message);
        // TODO: integrate real email service (e.g. JavaMail / SendGrid) here
    }

    private static void log(String message) {
        System.out.println("[NotificationService] " + message);
        if (ServerPortFrameController.instance != null)
            ServerPortFrameController.instance.log("[Notification] " + message);
    }
}
