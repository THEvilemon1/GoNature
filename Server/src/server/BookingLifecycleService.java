package server;

import common.Booking;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized booking lifecycle rules.
 *
 * This class owns all automatic transitions related to:
 * - waitlist expiration when visit time arrives
 * - offering a freed spot to the next traveler for one hour
 * - reminder confirmation one day before visit for two hours
 * - automatic SYSTEM_CANCEL handling and chained promotion to the next traveler
 */
public final class BookingLifecycleService {

    static final int WAITLIST_CONFIRMATION_WINDOW_HOURS = 1;
    static final int REMINDER_CONFIRMATION_WINDOW_HOURS = 2;
    static final int REMINDER_HOURS_BEFORE_VISIT = 24;

    private BookingLifecycleService() {
    }

    public static void ensureLifecycleColumns() {
        try {
            Connection conn = DBConnection.getStaticConnection();
            ensureBookingColumn(conn, "action_required_at", "DATETIME NULL");
            ensureBookingColumn(conn, "action_deadline", "DATETIME NULL");
            ensureBookingColumn(conn, "reminder_sent_at", "DATETIME NULL");
            ensureBookingColumn(conn, "last_notification_type", "VARCHAR(64) NULL");
        } catch (SQLException e) {
            log("Could not ensure lifecycle columns: " + e.getMessage());
        }
    }

    public static void processAutomaticTransitions() {
        try {
            Connection conn = DBConnection.getStaticConnection();
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                expireArrivedWaitlistBookings(conn);
                expirePendingWaitlistConfirmations(conn);
                sendReminderConfirmationRequests(conn);
                expirePendingReminderConfirmations(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (Exception e) {
            log("Automatic transition error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void handleConfirmedBookingCancelled(Connection conn, Booking cancelledBooking) throws SQLException {
        offerSpotToNextWaitingTraveler(conn, cancelledBooking.getParkId(), cancelledBooking.getVisitorTime());
    }

    public static void handleSpotFreed(Connection conn, int parkId, LocalDateTime visitorTime) throws SQLException {
        offerSpotToNextWaitingTraveler(conn, parkId, visitorTime);
    }

    public static boolean confirmBooking(int bookingId, String travelerId) throws SQLException {
        Connection conn = DBConnection.getStaticConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            Booking booking = Utils.getBookingByIdForUpdate(conn, bookingId);
            if (booking == null || !travelerId.equals(booking.getTravelerId())) {
                conn.rollback();
                return false;
            }

            if (Booking.STATUS_PENDING_WAITLIST_CONFIRMATION.equals(booking.getStatus())) {
                updateBookingStatus(conn, bookingId, Booking.STATUS_CONFIRMED, null, null, null);
                Utils.updateWaitingListEntryByBooking(conn,bookingId, "OFFERED", "CONFIRMED");
                sendSpotConfirmedNotification(conn, bookingId);
                conn.commit();
                return true;
            }

            if (Booking.STATUS_PENDING_REMINDER_CONFIRMATION.equals(booking.getStatus())) {
                updateBookingStatus(conn, bookingId, Booking.STATUS_CONFIRMED, null, null, null);
                sendReminderConfirmedNotification(conn, bookingId);
                conn.commit();
                return true;
            }

            conn.rollback();
            return false;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static void expireArrivedWaitlistBookings(Connection conn) throws SQLException {
        List<Booking> expiredBookings = findBookingsByStatusBefore(conn,
            Booking.STATUS_WAITING_LIST,
            LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            systemCancelBooking(conn, booking, "WAITLIST_TIME_ARRIVED", false);
            Utils.updateWaitingListEntryByBooking(conn,booking.getBookingId(), "WAITING", "EXPIRED");
            sendWaitlistExpiredNotification(conn, booking.getBookingId());
        }
    }

    private static void expirePendingWaitlistConfirmations(Connection conn) throws SQLException {
        List<Booking> expiredBookings = findBookingsByStatusWithDeadline(conn,
            Booking.STATUS_PENDING_WAITLIST_CONFIRMATION,
            LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            systemCancelBooking(conn, booking, "WAITLIST_CONFIRMATION_TIMEOUT", true);
            Utils.updateWaitingListEntryByBooking(conn,booking.getBookingId(), "OFFERED", "EXPIRED");
            sendWaitlistOfferExpiredNotification(conn, booking.getBookingId());
        }
    }

    private static void sendReminderConfirmationRequests(Connection conn) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lowerBound = now.plusHours(REMINDER_HOURS_BEFORE_VISIT).minusMinutes(5);
        LocalDateTime upperBound = now.plusHours(REMINDER_HOURS_BEFORE_VISIT).plusMinutes(5);

        String sql = "SELECT * FROM booking WHERE status = ? AND reminder_sent_at IS NULL AND visitorTime BETWEEN ? AND ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, Booking.STATUS_CONFIRMED);
        ps.setTimestamp(2, Timestamp.valueOf(lowerBound));
        ps.setTimestamp(3, Timestamp.valueOf(upperBound));
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Booking booking = Utils.mapBooking(rs);
            // LocalDateTime deadline = now.plusHours(REMINDER_CONFIRMATION_WINDOW_HOURS);
            LocalDateTime deadline = now.plusSeconds(30); // For testing purposes, set to 30 seconds instead of 1 hour
            updateBookingStatus(conn,
                booking.getBookingId(),
                Booking.STATUS_PENDING_REMINDER_CONFIRMATION,
                now,
                deadline,
                "REMINDER_CONFIRMATION_REQUEST");
            NotificationService.sendReminderConfirmationRequired(conn, booking.getBookingId(), deadline);
        }
    }

    private static void expirePendingReminderConfirmations(Connection conn) throws SQLException {
        List<Booking> expiredBookings = findBookingsByStatusWithDeadline(conn,
            Booking.STATUS_PENDING_REMINDER_CONFIRMATION,
            LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            systemCancelBooking(conn, booking, "REMINDER_CONFIRMATION_TIMEOUT", true);
            sendReminderExpiredNotification(conn, booking.getBookingId());
        }
    }

    private static void offerSpotToNextWaitingTraveler(Connection conn, int parkId, LocalDateTime slotTime) throws SQLException {
        int capacity = Utils.getParkEffectiveCapacity(conn, parkId);
        int occupiedVisitors = getOccupiedVisitorsForSlot(conn, parkId, slotTime);
        int remainingCapacity = capacity - occupiedVisitors;

        if (remainingCapacity <= 0) {
            return;
        }

        // Fetch waiting entries in registration order. We can offer multiple bookings
        // when capacity allows, but never skip over the head of the queue.
        String sql = "SELECT wle.id, b.* FROM WaitingList wl "
            + "JOIN WaitingListEntry wle ON wle.waitingList_id = wl.waitingList_id "
            + "JOIN booking b ON b.booking_id = wle.booking_id "
            + "WHERE wl.park_id = ? AND wl.slot_time = ? AND wle.status = ? AND b.status = ? "
            + "ORDER BY wle.registered_at ASC FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setTimestamp(2, Timestamp.valueOf(slotTime));
        ps.setString(3, "WAITING");
        ps.setString(4, Booking.STATUS_WAITING_LIST);
        ResultSet rs = ps.executeQuery();

        LocalDateTime now = LocalDateTime.now();

        while (rs.next() && remainingCapacity > 0) {
            Booking waitingBooking = Utils.mapBooking(rs);
            String waitingEntryId = rs.getString("id");

            if (waitingBooking.getNumberOfVisitors() > remainingCapacity) {
                break;
            }

            LocalDateTime deadline = now.plusHours(WAITLIST_CONFIRMATION_WINDOW_HOURS);
            updateBookingStatus(conn,
                waitingBooking.getBookingId(),
                Booking.STATUS_PENDING_WAITLIST_CONFIRMATION,
                now,
                deadline,
                "WAITLIST_SPOT_OFFERED");
            Utils.updateWaitingListEntryById(conn, waitingEntryId, "OFFERED");
            NotificationService.sendWaitlistPromotionOffer(conn, waitingBooking.getBookingId(), deadline);

            remainingCapacity -= waitingBooking.getNumberOfVisitors();
        }
    }

    private static void systemCancelBooking(Connection conn, Booking booking, String reason, boolean offerNextSpot) throws SQLException {
        updateBookingStatus(conn, booking.getBookingId(), Booking.STATUS_SYSTEM_CANCEL, null, null, reason);
        if (offerNextSpot) {
            offerSpotToNextWaitingTraveler(conn, booking.getParkId(), booking.getVisitorTime());
        }
    }

    private static void updateBookingStatus(Connection conn, int bookingId, String status,
                                            LocalDateTime actionRequiredAt,
                                            LocalDateTime actionDeadline,
                                            String notificationType) throws SQLException {
        String sql = "UPDATE booking SET status = ?, action_required_at = ?, action_deadline = ?, "
            + "reminder_sent_at = CASE WHEN ? = 'REMINDER_CONFIRMATION_REQUEST' THEN NOW() ELSE reminder_sent_at END, "
            + "last_notification_type = ? WHERE booking_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, status);
        Utils.setTimestampOrNull(ps,2, actionRequiredAt);
        Utils.setTimestampOrNull(ps,3, actionDeadline);
        ps.setString(4, notificationType);
        ps.setString(5, notificationType);
        ps.setInt(6, bookingId);
        ps.executeUpdate();
    }

    private static List<Booking> findBookingsByStatusBefore(Connection conn, String status, LocalDateTime cutoff) throws SQLException {
        String sql = "SELECT * FROM booking WHERE status = ? AND visitorTime <= ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, status);
        ps.setTimestamp(2, Timestamp.valueOf(cutoff));
        ResultSet rs = ps.executeQuery();
        return readBookings(rs);
    }

    private static List<Booking> findBookingsByStatusWithDeadline(Connection conn, String status, LocalDateTime cutoff) throws SQLException {
        String sql = "SELECT * FROM booking WHERE status = ? AND action_deadline IS NOT NULL AND action_deadline <= ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, status);
        ps.setTimestamp(2, Timestamp.valueOf(cutoff));
        ResultSet rs = ps.executeQuery();
        return readBookings(rs);
    }

    private static List<Booking> readBookings(ResultSet rs) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        while (rs.next()) {
            bookings.add(Utils.mapBooking(rs));
        }
        return bookings;
    }

    private static void sendWaitlistExpiredNotification(Connection conn, int bookingId) throws SQLException {
        NotificationService.sendSystemCancellation(conn, bookingId,
            "Your booking stayed on the waiting list until the visit time arrived.");
    }

    private static void sendWaitlistOfferExpiredNotification(Connection conn, int bookingId) throws SQLException {
        NotificationService.sendSystemCancellation(conn, bookingId,
            "Your offered spot expired because it was not confirmed within one hour.");
    }

    private static void sendReminderExpiredNotification(Connection conn, int bookingId) throws SQLException {
        NotificationService.sendSystemCancellation(conn, bookingId,
            "Your booking was not confirmed within two hours after the reminder.");
    }

    private static void sendSpotConfirmedNotification(Connection conn, int bookingId) throws SQLException {
        NotificationService.sendBookingConfirmed(conn, bookingId,
            "Your booking was confirmed after you accepted the available spot.");
    }

    private static void sendReminderConfirmedNotification(Connection conn, int bookingId) throws SQLException {
        NotificationService.sendBookingConfirmed(conn, bookingId,
            "Your visit reminder was confirmed successfully.");
    }

    private static int getOccupiedVisitorsForSlot(Connection conn, int parkId, LocalDateTime visitorTime) throws SQLException {
        String sql = "SELECT COALESCE(SUM(numberOfVisitors), 0) AS occupiedVisitors FROM booking "
            + "WHERE park_id = ? AND visitorTime = ? AND status IN (?, ?, ?) FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ps.setTimestamp(2, Timestamp.valueOf(visitorTime));
        ps.setString(3, Booking.STATUS_CONFIRMED);
        ps.setString(4, Booking.STATUS_PENDING_WAITLIST_CONFIRMATION);
        ps.setString(5, Booking.STATUS_PENDING_REMINDER_CONFIRMATION);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("occupiedVisitors") : 0;
    }

    private static void ensureBookingColumn(Connection conn, String columnName, String definition) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, "booking", columnName);
        if (!columns.next()) {
            conn.createStatement().executeUpdate("ALTER TABLE booking ADD COLUMN " + columnName + " " + definition);
            log("Added booking column: " + columnName);
        }
    }

    private static void log(String message) {
        Utils.log("BookingLifecycleService", message);
    }

}
