package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database-backed rate limiter for login attempts and other actions.
 * Prevents brute force attacks by tracking failed attempts.
 */
public class RateLimiter {
    private final DatabaseConnectionManager dbManager;

    // Configuration
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 900000; // 15 minutes
    private static final long ATTEMPT_WINDOW = 300000; // 5 minutes

    public RateLimiter(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Check if an identifier is currently locked out.
     */
    public boolean isLockedOut(String identifier, String attemptType) {
        String sql = """
            SELECT locked_until 
            FROM rate_limits 
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, attemptType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long lockedUntil = rs.getLong("locked_until");
                    if (lockedUntil > 0 && System.currentTimeMillis() < lockedUntil) {
                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check lockout status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Record a failed attempt and potentially lock out the identifier.
     */
    public void recordFailedAttempt(String identifier, String attemptType) {
        long now = System.currentTimeMillis();

        String selectSql = """
            SELECT attempt_count, first_attempt, last_attempt 
            FROM rate_limits 
            WHERE identifier = ? AND attempt_type = ?
        """;

        String insertSql = """
            INSERT INTO rate_limits (identifier, attempt_type, attempt_count, first_attempt, last_attempt)
            VALUES (?, ?, 1, ?, ?)
        """;

        String updateSql = """
            UPDATE rate_limits 
            SET attempt_count = ?, last_attempt = ?, locked_until = ?
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection()) {
            // Check existing attempts
            int currentAttempts = 0;
            long firstAttempt = now;

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, identifier);
                ps.setString(2, attemptType);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentAttempts = rs.getInt("attempt_count");
                        firstAttempt = rs.getLong("first_attempt");
                        long lastAttempt = rs.getLong("last_attempt");

                        // Reset if outside attempt window
                        if (now - firstAttempt > ATTEMPT_WINDOW) {
                            currentAttempts = 0;
                            firstAttempt = now;
                        }
                    }
                }
            }

            // Increment attempts
            int newAttempts = currentAttempts + 1;
            long lockedUntil = 0;

            // Lock out if max attempts reached
            if (newAttempts >= MAX_LOGIN_ATTEMPTS) {
                lockedUntil = now + LOCKOUT_DURATION;
                FlexLogger.warn("Locking out " + identifier + " for " + attemptType + " until " + lockedUntil);
            }

            // Insert or update
            if (currentAttempts == 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, identifier);
                    ps.setString(2, attemptType);
                    ps.setLong(3, firstAttempt);
                    ps.setLong(4, now);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, newAttempts);
                    ps.setLong(2, now);
                    ps.setLong(3, lockedUntil);
                    ps.setString(4, identifier);
                    ps.setString(5, attemptType);
                    ps.executeUpdate();
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to record failed attempt: " + e.getMessage());
        }
    }

    /**
     * Clear failed attempts for an identifier (after successful login).
     */
    public void clearFailedAttempts(String identifier, String attemptType) {
        String sql = """
            DELETE FROM rate_limits 
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, attemptType);
            ps.executeUpdate();

        } catch (SQLException e) {
            FlexLogger.error("Failed to clear failed attempts: " + e.getMessage());
        }
    }

    /**
     * Get remaining lockout time in milliseconds.
     * Returns 0 if not locked out.
     */
    public long getRemainingLockoutTime(String identifier, String attemptType) {
        String sql = """
            SELECT locked_until 
            FROM rate_limits 
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, attemptType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long lockedUntil = rs.getLong("locked_until");
                    long now = System.currentTimeMillis();

                    if (lockedUntil > now) {
                        return lockedUntil - now;
                    }
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get lockout time: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get current attempt count for an identifier.
     */
    public int getAttemptCount(String identifier, String attemptType) {
        String sql = """
            SELECT attempt_count, first_attempt 
            FROM rate_limits 
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, attemptType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long firstAttempt = rs.getLong("first_attempt");

                    // Check if still within attempt window
                    if (System.currentTimeMillis() - firstAttempt <= ATTEMPT_WINDOW) {
                        return rs.getInt("attempt_count");
                    }
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get attempt count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Clean up old rate limit entries (should be run periodically).
     */
    public int cleanupOldEntries() {
        long cutoff = System.currentTimeMillis() - (LOCKOUT_DURATION * 2);

        String sql = """
            DELETE FROM rate_limits 
            WHERE last_attempt < ? 
              AND (locked_until IS NULL OR locked_until < ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, cutoff);
            ps.setLong(2, System.currentTimeMillis());

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                FlexLogger.info("Cleaned up " + deleted + " old rate limit entries");
            }
            return deleted;

        } catch (SQLException e) {
            FlexLogger.error("Failed to cleanup rate limits: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Manually unlock an identifier (admin override).
     */
    public boolean unlock(String identifier, String attemptType) {
        String sql = """
            UPDATE rate_limits 
            SET locked_until = NULL, attempt_count = 0 
            WHERE identifier = ? AND attempt_type = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, attemptType);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                FlexLogger.info("Manually unlocked " + identifier + " for " + attemptType);
                return true;
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to unlock: " + e.getMessage());
        }

        return false;
    }
}