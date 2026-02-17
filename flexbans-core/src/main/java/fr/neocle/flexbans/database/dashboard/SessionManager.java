package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 * Manages user sessions with database persistence.
 * Stores session tokens, their creation/expiry times, and associated user IDs.
 */
public class SessionManager {
    private final DatabaseConnectionManager dbManager;
    private static final long SESSION_EXPIRY = 86400000; // 24 hours
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    public SessionManager(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Create a new session for a user.
     * Returns the session token.
     */
    public String createSession(int userId) {
        String sessionToken = generateSecureToken();
        long now = System.currentTimeMillis();
        long expiresAt = now + SESSION_EXPIRY;

        String sql = """
            INSERT INTO sessions (session_token, user_id, created_at, expires_at)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionToken);
            ps.setInt(2, userId);
            ps.setLong(3, now);
            ps.setLong(4, expiresAt);

            ps.executeUpdate();
            FlexLogger.info("Created session for user ID: " + userId);
            return sessionToken;

        } catch (SQLException e) {
            FlexLogger.error("Failed to create session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validate a session token and return the user ID if valid.
     * Returns null if session is invalid or expired.
     */
    public Integer validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return null;
        }

        String sql = """
            SELECT user_id, expires_at 
            FROM sessions 
            WHERE session_token = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionToken);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long expiresAt = rs.getLong("expires_at");

                    // Check if session has expired
                    if (System.currentTimeMillis() > expiresAt) {
                        deleteSession(sessionToken);
                        return null;
                    }

                    return rs.getInt("user_id");
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to validate session: " + e.getMessage());
        }

        return null;
    }

    /**
     * Delete a specific session (logout).
     */
    public boolean deleteSession(String sessionToken) {
        String sql = "DELETE FROM sessions WHERE session_token = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionToken);
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                FlexLogger.info("Deleted session");
                return true;
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to delete session: " + e.getMessage());
        }

        return false;
    }

    /**
     * Delete all sessions for a specific user (logout everywhere).
     */
    public boolean deleteAllUserSessions(int userId) {
        String sql = "DELETE FROM sessions WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int deleted = ps.executeUpdate();

            FlexLogger.info("Deleted " + deleted + " sessions for user ID: " + userId);
            return true;

        } catch (SQLException e) {
            FlexLogger.error("Failed to delete user sessions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extend session expiry (refresh session).
     */
    public boolean extendSession(String sessionToken) {
        long newExpiresAt = System.currentTimeMillis() + SESSION_EXPIRY;

        String sql = "UPDATE sessions SET expires_at = ? WHERE session_token = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, newExpiresAt);
            ps.setString(2, sessionToken);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            FlexLogger.error("Failed to extend session: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clean up expired sessions (should be run periodically).
     */
    public int cleanupExpiredSessions() {
        String sql = "DELETE FROM sessions WHERE expires_at < ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, System.currentTimeMillis());
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                FlexLogger.info("Cleaned up " + deleted + " expired sessions");
            }
            return deleted;

        } catch (SQLException e) {
            FlexLogger.error("Failed to cleanup sessions: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get session info including user ID and expiry.
     */
    public SessionInfo getSessionInfo(String sessionToken) {
        String sql = """
            SELECT user_id, created_at, expires_at 
            FROM sessions 
            WHERE session_token = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionToken);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");

                    return new SessionInfo(sessionToken, userId, createdAt, expiresAt);
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get session info: " + e.getMessage());
        }

        return null;
    }

    /**
     * Count active sessions for a user.
     */
    public int countUserSessions(int userId) {
        String sql = """
            SELECT COUNT(*) as count 
            FROM sessions 
            WHERE user_id = ? AND expires_at > ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setLong(2, System.currentTimeMillis());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to count sessions: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Generate a cryptographically secure random token.
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Data class for session information.
     */
    public static class SessionInfo {
        public final String sessionToken;
        public final int userId;
        public final long createdAt;
        public final long expiresAt;

        public SessionInfo(String sessionToken, int userId, long createdAt, long expiresAt) {
            this.sessionToken = sessionToken;
            this.userId = userId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        public long timeUntilExpiry() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
    }
}