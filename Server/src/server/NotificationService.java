package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationService {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void sendWaitlistPromotionOffer(Connection conn, String bookingId, LocalDateTime deadline) throws SQLException {
        BookingNotificationContext context = getContext(conn, bookingId);
        String message = String.format(
            "Hello %s, a spot is now available for your visit to Park %d at %s. Please confirm before %s or the booking will be cancelled.",
            context.name,
            context.parkId,
            format(context.visitorTime),
            format(deadline)
        );
        notifyTraveler(context, "WAITLIST_PROMOTION", message);
    }

    public static void sendReminderConfirmationRequired(Connection conn, String bookingId, LocalDateTime deadline) throws SQLException {
        BookingNotificationContext context = getContext(conn, bookingId);
        String message = String.format(
            "Hello %s, your visit to Park %d is scheduled for %s. Please confirm before %s or the booking will be cancelled automatically.",
            context.name,
            context.parkId,
            format(context.visitorTime),
            format(deadline)
        );
        notifyTraveler(context, "REMINDER_CONFIRMATION", message);
    }

    public static void sendSystemCancellation(Connection conn, String bookingId, String reason) throws SQLException {
        BookingNotificationContext context = getContext(conn, bookingId);
        String message = String.format(
            "Hello %s, your booking %s for Park %d at %s was cancelled by the system. Reason: %s",
            context.name,
            context.bookingId,
            context.parkId,
            format(context.visitorTime),
            reason
        );
        notifyTraveler(context, "SYSTEM_CANCEL", message);
    }

    public static void sendBookingConfirmed(Connection conn, String bookingId, String reason) throws SQLException {
        BookingNotificationContext context = getContext(conn, bookingId);
        String message = String.format(
            "Hello %s, your booking %s for Park %d at %s is confirmed. %s",
            context.name,
            context.bookingId,
            context.parkId,
            format(context.visitorTime),
            reason
        );
        notifyTraveler(context, "BOOKING_CONFIRMED", message);
    }

    private static BookingNotificationContext getContext(Connection conn, String bookingId) throws SQLException {
        String sql = "SELECT b.booking_id, b.traveler_id, b.park_id, b.visitorTime, "
            + "COALESCE(NULLIF(b.travelerName, ''), TRIM(CONCAT(u.firstName, ' ', u.lastName))) AS contactName, "
            + "COALESCE(NULLIF(b.travelerEmail, ''), u.email) AS contactEmail, "
            + "COALESCE(NULLIF(b.travelerPhoneNumber, ''), u.phoneNumber) AS contactPhoneNumber "
            + "FROM booking b "
            + "JOIN `user` u ON u.user_id = b.traveler_id "
            + "WHERE b.booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Booking not found for notification: " + bookingId);
        }

        Timestamp visitorTimestamp = rs.getTimestamp("visitorTime");
        return new BookingNotificationContext(
            rs.getString("booking_id"),
            rs.getString("traveler_id"),
            rs.getInt("park_id"),
            visitorTimestamp.toLocalDateTime(),
            buildName(rs.getString("contactName")),
            rs.getString("contactEmail"),
            rs.getString("contactPhoneNumber")
        );
    }

    private static String buildName(String name) {
        String cleaned = name == null ? "" : name.trim();
        return cleaned.isEmpty() ? "Traveler" : cleaned;
    }

    private static void notifyTraveler(BookingNotificationContext context, String eventType, String message) {
        sendSMS(context, eventType, message);
        sendEmail(context, eventType, message);
    }

    private static void sendSMS(BookingNotificationContext context, String eventType, String message) {
        String destination = context.phoneNumber == null || context.phoneNumber.isBlank()
            ? "NO_PHONE_ON_FILE"
            : context.phoneNumber;
        log(String.format("[SMS][%s][Traveler %s -> %s] %s",
            eventType,
            context.travelerId,
            destination,
            message));
    }

    private static void sendEmail(BookingNotificationContext context, String eventType, String message) {
        String destination = context.email == null || context.email.isBlank()
            ? "NO_EMAIL_ON_FILE"
            : context.email;
        log(String.format("[EMAIL][%s][Traveler %s -> %s] %s",
            eventType,
            context.travelerId,
            destination,
            message));
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime.format(DISPLAY_FORMAT);
    }

    private static void log(String message) {
        Utils.log("NotificationService", message);
    }

    private static final class BookingNotificationContext {
        private final String bookingId;
        private final String travelerId;
        private final int parkId;
        private final LocalDateTime visitorTime;
    private final String name;
        private final String email;
        private final String phoneNumber;

        private BookingNotificationContext(String bookingId, String travelerId, int parkId,
                                           LocalDateTime visitorTime, String name, String email, String phoneNumber) {
            this.bookingId = bookingId;
            this.travelerId = travelerId;
            this.parkId = parkId;
            this.visitorTime = visitorTime;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }
    }
}
