package fr.neocle.flexbans.database.server;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages server locks using the new schema with actors table.
 */
public class ServerLocksManager {
    private final DatabaseConnectionManager dbManager;
    private final ActorsManager actorsManager;

    public ServerLocksManager(DatabaseConnectionManager dbManager, ActorsManager actorsManager) {
        this.dbManager = dbManager;
        this.actorsManager = actorsManager;
    }

    /**
     * Insert a new server lock.
     */
    public long insertServerLock(
            String serverName,
            String reason,
            long duration,
            UUID issuerUuid,
            String issuerName,
            String serverOrigin,
            boolean silent
    ) {
        int issuerActorId = issuerUuid != null
                ? actorsManager.getOrCreatePlayerActor(issuerUuid, issuerName)
                : actorsManager.getOrCreateConsoleActor();

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
            FlexLogger.error("Failed to insert server lock for " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Unlock a server.
     */
    public boolean unlockServer(String serverName, UUID removerUuid, String removerName, String reason) {
        int removerActorId = removerUuid != null
                ? actorsManager.getOrCreatePlayerActor(removerUuid, removerName)
                : actorsManager.getOrCreateConsoleActor();

        String updateSql = """
            UPDATE server_locks 
            SET status = 'UNLOCKED' 
            WHERE server_name = ? 
              AND status = 'LOCKED'
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Get lock ID
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

                // Update lock status
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, serverName);
                    ps.executeUpdate();
                }

                // Record action
                recordLockAction(conn, lockId, removerActorId, "UNLOCKED", reason);

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to unlock server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Record a server lock action.
     */
    private void recordLockAction(
            Connection conn,
            long lockId,
            int actorId,
            String action,
            String reason
    ) throws SQLException {
        String sql = """
            INSERT INTO server_lock_actions (lock_id, actor_id, action, reason, action_time)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lockId);
            ps.setInt(2, actorId);
            ps.setString(3, action);
            ps.setString(4, reason);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    /**
     * Check if a server is locked.
     */
    public boolean isServerLocked(String serverName) {
        String sql = "SELECT 1 FROM server_locks WHERE server_name = ? AND status = 'LOCKED' LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, serverName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check if server is locked: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get lock information.
     */
    public ServerLockInfo getLockInfo(String serverName) {
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
            FlexLogger.error("Failed to get lock info: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get reason for a locked server.
     */
    public String getReason(String serverName) {
        ServerLockInfo info = getLockInfo(serverName);
        return info != null ? info.reason : null;
    }

    /**
     * Get issuer name for a locked server.
     */
    public String getIssuer(String serverName) {
        ServerLockInfo info = getLockInfo(serverName);
        if (info != null) {
            var actorInfo = actorsManager.getActorInfo(info.issuerActorId);
            return actorInfo != null ? actorInfo.name : null;
        }
        return null;
    }

    /**
     * Get lock time for a locked server.
     */
    public long getTime(String serverName) {
        ServerLockInfo info = getLockInfo(serverName);
        return info != null ? info.createdAt : -1;
    }

    /**
     * Data class for server lock information.
     */
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