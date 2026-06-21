package server;

import common.Booking;
import gui.ServerPortFrameController;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Shared static helpers used by ParkServer and BookingLifecycleService.
 *
 * These methods were previously duplicated across those two classes.
 * Centralizing them here means a bug fix or schema change only needs to
 * happen in one place, and every caller automatically benefits.
 */
public final class Utils {
    public static final double GUIDE_DISCOUNT = 0.75; // 25% off for guides
    public static final double CLUB_MEMBER_DISCOUNT = 0.9; // 10%
    public static final double DIGITAL_BOOKING_DISCOUNT = 0.85; // 15%
    public static final double PREPAYMENT_DISCOUNT = 0.88; // 12% off for prepayment ONLY FOR GUIDES.

    private Utils() {}

    // ── Booking ──────────────────────────────────────────────────────────────

    /**
     * Fetches a booking row and locks it for the duration of the current
     * transaction (FOR UPDATE). Used before any status transition so that
     * two concurrent requests cannot race on the same booking row.
     */
    public static Booking getBookingByIdForUpdate(Connection conn, String bookingId) throws SQLException {
        String sql = "SELECT * FROM booking WHERE booking_id = ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, bookingId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapBooking(rs);
        return null;
    }

    /**
     * Converts the current ResultSet row into a Booking object.
     *
     * travelerName / travelerEmail / travelerPhoneNumber are read with
     * getOptionalColumn so that queries which do not SELECT those columns
     * (e.g. lifecycle queries) still work — the missing columns simply
     * come back as null instead of throwing a SQLException.
     */
    public static Booking mapBooking(ResultSet rs) throws SQLException {
        Timestamp visitorTimestamp = rs.getTimestamp("visitorTime");
        return new Booking(
            rs.getString("booking_id"),
            rs.getString("traveler_id"),
            getOptionalColumn(rs, "travelerName"),
            getOptionalColumn(rs, "travelerEmail"),
            getOptionalColumn(rs, "travelerPhoneNumber"),
            rs.getInt("park_id"),
            rs.getInt("numberOfVisitors"),
            visitorTimestamp.toLocalDateTime(),
            rs.getString("status"),
            rs.getBoolean("organizedBooking"),
            rs.getDouble("price")
        );
    }

    // ── Park ─────────────────────────────────────────────────────────────────

    /**
     * Returns (maxCapacity - gap) for the given park, locking the row for
     * update so that capacity checks inside a transaction are consistent.
     * Both createBooking and offerSpotToNextWaitingTraveler need this to
     * decide whether a new visitor fits in the park.
     */
    public static int getParkEffectiveCapacity(Connection conn, int parkId) throws SQLException {
        String sql = "SELECT maxCapacity, gap FROM park WHERE park_id = ? FOR UPDATE";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, parkId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Selected park does not exist in the database.");
        }
        return Math.max(0, rs.getInt("maxCapacity") - rs.getInt("gap"));
    }

    // ── WaitingListEntry ─────────────────────────────────────────────────────

    /**
     * Transitions a WaitingListEntry from one status to another, identified
     * by the booking it belongs to. The fromStatus guard prevents accidental
     * double-transitions if the same entry is processed more than once.
     * Used for WAITING→EXPIRED, OFFERED→CONFIRMED, OFFERED→EXPIRED, WAITING→CANCELLED.
     */
    public static void updateWaitingListEntryByBooking(Connection conn, String bookingId,
            String fromStatus, String toStatus) throws SQLException {
        String sql = "UPDATE WaitingListEntry SET status = ?, updated_at = NOW() WHERE booking_id = ? AND status = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, toStatus);
        ps.setString(2, bookingId);
        ps.setString(3, fromStatus);
        ps.executeUpdate();
    }

    /**
     * Updates a specific WaitingListEntry by its own primary key (id).
     * Used when we already know the exact entry, e.g. when promoting the
     * first waiting traveler to OFFERED status.
     */
    public static void updateWaitingListEntryById(Connection conn, String entryId,
            String toStatus) throws SQLException {
        String sql = "UPDATE WaitingListEntry SET status = ?, updated_at = NOW() WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, toStatus);
        ps.setString(2, entryId);
        ps.executeUpdate();
    }

    // ── PreparedStatement helpers ─────────────────────────────────────────────

    /**
     * Sets a TIMESTAMP parameter, or SQL NULL when the value is null.
     * Lifecycle columns like action_required_at and action_deadline are
     * nullable, so we need this instead of a plain Timestamp.valueOf() call
     * which would throw NullPointerException on null input.
     */
    public static void setTimestampOrNull(PreparedStatement ps, int parameterIndex,
            LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        }
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    /**
     * Writes a log line to stdout and to the server UI simultaneously.
     * Pass the simple class name as className so the output reads like
     * "[BookingLifecycleService] message", matching the previous per-class style.
     */
    public static void log(String className, String message) {
        String formatted = "[" + className + "] " + message;
        System.out.println(formatted);
        if (ServerPortFrameController.instance != null) {
            ServerPortFrameController.instance.log(formatted);
        }
    }

    // ── Traveler role checks ──────────────────────────────────────────────────

    /**
     * Returns true if the traveler is registered as a guide in the DB.
     * Guides get a discount on bookings and are allowed to book for groups.
     * Returns false if the traveler_id does not exist.
     */
    public static boolean isGuide(Connection conn, String travelerId) throws SQLException {
        String sql = "SELECT guide FROM traveler WHERE traveler_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getBoolean("guide");
    }

    /**
     * Returns true if the traveler holds an active club membership.
     * Club members receive a discount calculated in calculatePrice.
     * Returns false if the traveler_id does not exist.
     */
    public static boolean isClubMember(Connection conn, String travelerId) throws SQLException {
        String sql = "SELECT clubMember FROM traveler WHERE traveler_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, travelerId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getBoolean("clubMember");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Reads a String column from a ResultSet without throwing if the column
     * is absent from the query's SELECT list. Returns null in that case,
     * which is indistinguishable from a present-but-null column — exactly
     * what we want for optional traveler contact fields.
     */
    private static String getOptionalColumn(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }
}
