package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class SessionManager {
    private final DatabaseConnectionManager dbManager;
    private final ExecutorService dbExecutor;

    private static final long SESSION_EXPIRY = 86400000;
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final FlexLogger LOGGER = FlexLogger.get(SessionManager.class);

    public SessionManager(DatabaseConnectionManager dbManager, ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<String> createSession(int userId) {
        return CompletableFuture.supplyAsync(() -> {
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

                LOGGER.debug("Created session for user ID: {}", userId);
                return sessionToken;

            } catch (SQLException e) {
                LOGGER.error("Failed to create session: ", e);
                return null;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Integer> validateSession(String sessionToken) {
        return CompletableFuture.supplyAsync(() -> {
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

                        if (System.currentTimeMillis() > expiresAt) {
                            deleteSessionInternal(sessionToken);
                            return null;
                        }

                        return rs.getInt("user_id");
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to validate session: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> deleteSession(String sessionToken) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM sessions WHERE session_token = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, sessionToken);
                int deleted = ps.executeUpdate();

                if (deleted > 0) {
                    LOGGER.debug("Deleted {} active session(s)", deleted);
                    return true;
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to delete session: ", e);
            }

            return false;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> deleteAllUserSessions(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM sessions WHERE user_id = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, userId);
                int deleted = ps.executeUpdate();

                LOGGER.debug("Deleted {} sessions for user ID: {}", deleted, userId);
                return true;

            } catch (SQLException e) {
                LOGGER.error("Failed to delete user sessions: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> extendSession(String sessionToken) {
        return CompletableFuture.supplyAsync(() -> {
            long newExpiresAt = System.currentTimeMillis() + SESSION_EXPIRY;

            String sql = "UPDATE sessions SET expires_at = ? WHERE session_token = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, newExpiresAt);
                ps.setString(2, sessionToken);

                return ps.executeUpdate() > 0;

            } catch (SQLException e) {
                LOGGER.error("Failed to extend session: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Integer> cleanupExpiredSessions() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM sessions WHERE expires_at < ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, System.currentTimeMillis());
                int deleted = ps.executeUpdate();

                if (deleted > 0) {
                    LOGGER.info("Cleaned up {} expired session(s)", deleted);
                }
                return deleted;

            } catch (SQLException e) {
                LOGGER.error("Failed to cleanup sessions: ", e);
                return 0;
            }
        }, dbExecutor);
    }

    public CompletableFuture<SessionInfo> getSessionInfo(String sessionToken) {
        return CompletableFuture.supplyAsync(() -> {
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
                LOGGER.error("Failed to get session info: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<Integer> countUserSessions(int userId) {
        return CompletableFuture.supplyAsync(() -> {
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
                LOGGER.error("Failed to count sessions: ", e);
            }

            return 0;
        }, dbExecutor);
    }

    private CompletableFuture<Void> deleteSessionInternal(String sessionToken) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM sessions WHERE session_token = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sessionToken);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Failed to delete session: ", e);
            }
        }, dbExecutor);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

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