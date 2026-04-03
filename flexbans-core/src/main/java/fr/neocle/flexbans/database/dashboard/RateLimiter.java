package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RateLimiter {
    private final DatabaseConnectionManager dbManager;
    private final ExecutorService dbExecutor;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 900000; // 15 minutes
    private static final long ATTEMPT_WINDOW = 300000;   // 5 minutes

    private static final FlexLogger LOGGER = FlexLogger.get(RateLimiter.class);

    public RateLimiter(DatabaseConnectionManager dbManager, ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Boolean> isLockedOut(String identifier, String attemptType) {
        return CompletableFuture.supplyAsync(() -> {
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
                LOGGER.error("Failed to check lockout status: ", e);
            }

            return false;
        }, dbExecutor);
    }

    public CompletableFuture<Void> recordFailedAttempt(String identifier, String attemptType) {
        return CompletableFuture.runAsync(() -> {
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

                int newAttempts = currentAttempts + 1;
                long lockedUntil = 0;

                if (newAttempts >= MAX_LOGIN_ATTEMPTS) {
                    lockedUntil = now + LOCKOUT_DURATION;
                    LOGGER.debug("Locking out {} for {} until {}", identifier, attemptType, lockedUntil);
                }

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
                LOGGER.error("Failed to record failed attempt: ", e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> clearFailedAttempts(String identifier, String attemptType) {
        return CompletableFuture.runAsync(() -> {
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
                LOGGER.error("Failed to clear failed attempts: ", e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Long> getRemainingLockoutTime(String identifier, String attemptType) {
        return CompletableFuture.supplyAsync(() -> {
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
                LOGGER.error("Failed to get lockout time: ", e);
            }

            return 0L;
        }, dbExecutor);
    }

    public CompletableFuture<Integer> getAttemptCount(String identifier, String attemptType) {
        return CompletableFuture.supplyAsync(() -> {
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

                        if (System.currentTimeMillis() - firstAttempt <= ATTEMPT_WINDOW) {
                            return rs.getInt("attempt_count");
                        }
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get attempt count: ", e);
            }

            return 0;
        }, dbExecutor);
    }

    public CompletableFuture<Integer> cleanupOldEntries() {
        return CompletableFuture.supplyAsync(() -> {
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
                    LOGGER.debug("Cleaned up {} old rate limit entries", deleted);
                }
                return deleted;

            } catch (SQLException e) {
                LOGGER.error("Failed to cleanup rate limits: ", e);
                return 0;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> unlock(String identifier, String attemptType) {
        return CompletableFuture.supplyAsync(() -> {
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
                    LOGGER.debug("Manually unlocked {} for {}", identifier, attemptType);
                    return true;
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to unlock: ", e);
            }

            return false;
        }, dbExecutor);
    }
}