package fr.neocle.flexbans.database.server;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ServerLocksManager {
    private final DatabaseConnectionManager dbManager;
    private final ActorsManager actorsManager;
    private final ExecutorService dbExecutor;

    private static final FlexLogger LOGGER = FlexLogger.get(ServerLocksManager.class);

    public ServerLocksManager(DatabaseConnectionManager dbManager,
                              ActorsManager actorsManager,
                              ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.actorsManager = actorsManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Long> insertServerLock(
            String serverName,
            String reason,
            long duration,
            UUID issuerUuid,
            String issuerName,
            String serverOrigin,
            boolean silent
    ) {
        CompletableFuture<Integer> issuerActorFuture =
                issuerUuid != null
                        ? actorsManager.getOrCreatePlayerActor(issuerUuid, issuerName)
                        : actorsManager.getOrCreateConsoleActor();

        return issuerActorFuture.thenComposeAsync(issuerActorId ->
                        CompletableFuture.supplyAsync(() -> {
                            String sql = """
                        INSERT INTO server_locks (
                            server_name, issuer_actor_id, created_at, reason,
                            duration, server_origin, silent, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'LOCKED')
                    """;

                            try (Connection conn = dbManager.getConnection();
                                 PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                                ps.setString(1, serverName);
                                ps.setInt(2, issuerActorId);
                                ps.setLong(3, System.currentTimeMillis());
                                ps.setString(4, reason);
                                ps.setLong(5, duration);
                                ps.setString(6, serverOrigin);
                                ps.setBoolean(7, silent);

                                ps.executeUpdate();

                                try (ResultSet rs = ps.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        return rs.getLong(1);
                                    }
                                }

                            } catch (SQLException e) {
                                LOGGER.error("Failed to insert server lock for {}: ", serverName, e);
                            }

                            return -1L;
                        }, dbExecutor)
                , dbExecutor);
    }

    public CompletableFuture<Boolean> unlockServer(String serverName, UUID removerUuid, String removerName, String reason) {
        CompletableFuture<Integer> removerActorFuture =
                removerUuid != null
                        ? actorsManager.getOrCreatePlayerActor(removerUuid, removerName)
                        : actorsManager.getOrCreateConsoleActor();

        return removerActorFuture.thenComposeAsync(removerActorId ->
                        CompletableFuture.supplyAsync(() -> {
                            String updateSql = """
                        UPDATE server_locks
                        SET status = 'UNLOCKED'
                        WHERE server_name = ?
                          AND status = 'LOCKED'
                    """;

                            try (Connection conn = dbManager.getConnection()) {
                                conn.setAutoCommit(false);

                                try {
                                    long lockId = -1;
                                    String selectSql = "SELECT id FROM server_locks WHERE server_name = ? AND status = 'LOCKED'";

                                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                                        ps.setString(1, serverName);
                                        try (ResultSet rs = ps.executeQuery()) {
                                            if (rs.next()) {
                                                lockId = rs.getLong("id");
                                            }
                                        }
                                    }

                                    if (lockId == -1) {
                                        conn.rollback();
                                        return false;
                                    }

                                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                                        ps.setString(1, serverName);
                                        ps.executeUpdate();
                                    }

                                    recordLockAction(lockId, removerActorId, "UNLOCKED", reason);

                                    conn.commit();
                                    return true;

                                } catch (SQLException e) {
                                    conn.rollback();
                                    throw e;
                                } finally {
                                    conn.setAutoCommit(true);
                                }

                            } catch (SQLException e) {
                                LOGGER.error("Failed to unlock server {}: ", serverName, e);
                            }

                            return false;
                        }, dbExecutor)
                , dbExecutor);
    }

    private CompletableFuture<Void> recordLockAction(
            long lockId,
            int actorId,
            String action,
            String reason
    ) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
            INSERT INTO server_lock_actions (lock_id, actor_id, action, reason, action_time)
            VALUES (?, ?, ?, ?, ?)
        """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, lockId);
                ps.setInt(2, actorId);
                ps.setString(3, action);
                ps.setString(4, reason);
                ps.setLong(5, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (SQLException e) {
                LOGGER.error("Failed to record lock action: ", e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> isServerLocked(String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM server_locks WHERE server_name = ? AND status = 'LOCKED' LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, serverName);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to check if server is locked: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<ServerLockInfo> getLockInfo(String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, issuer_actor_id, created_at, reason, duration, server_origin, silent
                FROM server_locks
                WHERE server_name = ?
                  AND status = 'LOCKED'
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, serverName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ServerLockInfo(
                                rs.getLong("id"),
                                serverName,
                                rs.getInt("issuer_actor_id"),
                                rs.getLong("created_at"),
                                rs.getString("reason"),
                                rs.getLong("duration"),
                                rs.getString("server_origin"),
                                rs.getBoolean("silent")
                        );
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get lock info: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<String> getReason(String serverName) {
        return getLockInfo(serverName).thenApply(info -> info != null ? info.reason : null);
    }

    public CompletableFuture<String> getIssuer(String serverName) {
        return getLockInfo(serverName).thenComposeAsync(info -> {
            if (info == null) return CompletableFuture.completedFuture(null);
            return actorsManager.getActorInfo(info.issuerActorId)
                    .thenApply(actorInfo -> actorInfo != null ? actorInfo.name() : null);
        }, dbExecutor);
    }

    public CompletableFuture<Long> getTime(String serverName) {
        return getLockInfo(serverName).thenApply(info -> info != null ? info.createdAt : -1L);
    }

    public static class ServerLockInfo {
        public final long id;
        public final String serverName;
        public final int issuerActorId;
        public final long createdAt;
        public final String reason;
        public final long duration;
        public final String serverOrigin;
        public final boolean silent;

        public ServerLockInfo(long id, String serverName, int issuerActorId, long createdAt,
                              String reason, long duration, String serverOrigin, boolean silent) {
            this.id = id;
            this.serverName = serverName;
            this.issuerActorId = issuerActorId;
            this.createdAt = createdAt;
            this.reason = reason;
            this.duration = duration;
            this.serverOrigin = serverOrigin;
            this.silent = silent;
        }
    }
}