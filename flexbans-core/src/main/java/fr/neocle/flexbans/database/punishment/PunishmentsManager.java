package fr.neocle.flexbans.database.punishment;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PunishmentsManager {
    private final DatabaseConnectionManager dbManager;
    private final ActorsManager actorsManager;
    private final ProfilesManager profilesManager;
    private final ScheduledExecutorService scheduler;

    private static final UUID CONSOLE_UUID =
            UUID.fromString("f78a4d8d-d51b-4b39-98a3-230f2de0c670");

    public enum PunishmentType {
        BAN, MUTE, KICK, WARNING
    }

    public PunishmentsManager(DatabaseConnectionManager dbManager, ActorsManager actorsManager, ProfilesManager profilesManager) {
        this.dbManager = dbManager;
        this.actorsManager = actorsManager;
        this.profilesManager = profilesManager;
        this.scheduler = Executors.newScheduledThreadPool(1);

        startExpirationScheduler();
    }

    private void startExpirationScheduler() {
        scheduler.scheduleWithFixedDelay(this::updateExpiredPunishments, 15, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            FlexLogger.info("Punishment expiration scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            FlexLogger.warn("Punishment expiration scheduler interrupted");
        }
    }

    public void updateExpiredPunishments() {
        String sql = """
            UPDATE punishments 
            SET status = 'EXPIRED' 
            WHERE status = 'ACTIVE' 
              AND duration > 0 
              AND (created_at + duration) < ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            int updated = ps.executeUpdate();

            if (updated > 0) {
                FlexLogger.info("Auto-expired " + updated + " punishment(s)");
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to update expired punishments: " + e.getMessage());
        }
    }

    public long insertPunishment(
            PunishmentType type,
            UUID targetUuid,
            InetAddress targetIp,
            UUID issuerUuid,
            String issuerName,
            String reason,
            long duration,
            String serverScope,
            String serverOrigin,
            boolean silent,
            boolean ipScope
    ) {
        int issuerActorId = CONSOLE_UUID.equals(issuerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(issuerUuid, issuerName);

        long now = System.currentTimeMillis();
        Long expiresAt = (duration > 0) ? (now + duration) : null;

        String sql = """
            INSERT INTO punishments (
                type, target_uuid, ip, issuer_actor_id, created_at, 
                reason, duration, expires_at, server_scope, server_origin, 
                silent, status, ip_scope
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, type.name());
            ps.setString(2, targetUuid != null ? targetUuid.toString() : null);
            ps.setBytes(3, targetIp != null ? targetIp.getAddress() : null);
            ps.setInt(4, issuerActorId);
            ps.setLong(5, now);
            ps.setString(6, reason);
            ps.setLong(7, duration);

            if (expiresAt != null) {
                ps.setLong(8, expiresAt);
            } else {
                ps.setNull(8, java.sql.Types.BIGINT);
            }

            ps.setString(9, serverScope);
            ps.setString(10, serverOrigin);
            ps.setBoolean(11, silent);
            ps.setBoolean(12, ipScope);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to insert punishment: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    public boolean removePunishment(
            PunishmentType type,
            UUID targetUuid,
            UUID removerUuid,
            String removerName,
            String removalReason,
            String serverScope
    ) {
        int removerActorId = CONSOLE_UUID.equals(removerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(removerUuid, removerName);

        boolean isScoped = serverScope != null && !serverScope.equalsIgnoreCase("global");

        String updateSql;
        if (isScoped) {
            updateSql = """
                UPDATE punishments 
                SET status = 'REMOVED' 
                WHERE target_uuid = ? 
                  AND type = ? 
                  AND status = 'ACTIVE' 
                  AND server_scope = ?
            """;
        } else {
            updateSql = """
                UPDATE punishments 
                SET status = 'REMOVED' 
                WHERE target_uuid = ? 
                  AND type = ? 
                  AND status = 'ACTIVE'
            """;
        }

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                long punishmentId = -1;
                String selectSql = isScoped
                        ? "SELECT id FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND server_scope = ?"
                        : "SELECT id FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'";

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, targetUuid.toString());
                    ps.setString(2, type.name());
                    if (isScoped) {
                        ps.setString(3, serverScope);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            punishmentId = rs.getLong("id");
                        }
                    }
                }

                if (punishmentId == -1) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, targetUuid.toString());
                    ps.setString(2, type.name());
                    if (isScoped) {
                        ps.setString(3, serverScope);
                    }
                    ps.executeUpdate();
                }

                recordPunishmentAction(conn, punishmentId, removerActorId, "REMOVED", removalReason);

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to remove punishment: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean removePunishmentById(
            long punishmentId,
            UUID removerUuid,
            String removerName,
            String removalReason
    ) {
        int removerActorId = CONSOLE_UUID.equals(removerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(removerUuid, removerName);

        String checkSql = """
        SELECT id FROM punishments
        WHERE id = ?
          AND status = 'ACTIVE'
    """;

        String updateSql = """
        UPDATE punishments
        SET status = 'REMOVED'
        WHERE id = ?
    """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setLong(1, punishmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setLong(1, punishmentId);
                ps.executeUpdate();
            }

            recordPunishmentAction(
                    conn,
                    punishmentId,
                    removerActorId,
                    "REMOVED",
                    removalReason
            );

            conn.commit();
            return true;

        } catch (SQLException e) {
            FlexLogger.error("Failed to remove punishment by ID: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePunishment(
            long punishmentId,
            UUID updaterUuid,
            String updaterName,
            String newReason,
            long newDuration
    ) {
        int updaterActorId = CONSOLE_UUID.equals(updaterUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(updaterUuid, updaterName);

        String checkSql = """
                    SELECT id FROM punishments
                    WHERE id = ?
                      AND status = 'ACTIVE'
                """;

        String updateSql = """
                    UPDATE punishments
                    SET reason = ?, duration = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setLong(1, punishmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, newReason);
                ps.setLong(2, newDuration);
                ps.setLong(3, punishmentId);
                ps.executeUpdate();
            }

            recordPunishmentAction(
                    conn,
                    punishmentId,
                    updaterActorId,
                    "UPDATED",
                    newReason
            );

            conn.commit();
            return true;

        } catch (SQLException e) {
            FlexLogger.error("Failed to update punishment by ID: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void recordPunishmentAction(
            Connection conn,
            long punishmentId,
            int actorId,
            String action,
            String reason
    ) throws SQLException {
        String sql = """
            INSERT INTO punishment_actions (punishment_id, actor_id, action, reason, action_time)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, punishmentId);
            ps.setInt(2, actorId);
            ps.setString(3, action);
            ps.setString(4, reason);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public boolean isPlayerPunished(PunishmentType type, UUID uuid, String serverName) {
        boolean checkGlobal = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");

        String sql = checkGlobal
                ? """
                SELECT 1 FROM punishments 
                WHERE target_uuid = ? 
                  AND type = ? 
                  AND status = 'ACTIVE'
                LIMIT 1
            """
                : """
                SELECT 1 FROM punishments 
                WHERE target_uuid = ? 
                  AND type = ? 
                  AND status = 'ACTIVE'
                  AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')
                LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());

            if (!checkGlobal) {
                ps.setString(3, serverName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check punishment: " + e.getMessage());
            return false;
        }
    }

    public boolean isIpPunished(PunishmentType type, InetAddress address, String serverName) {
        List<UUID> players = profilesManager.getPlayersByIp(address);
        if (players.isEmpty()) return false;

        boolean checkGlobal = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");

        String inClause = String.join(",", Collections.nCopies(players.size(), "?"));
        String sql = checkGlobal
                ? "SELECT 1 FROM punishments WHERE target_uuid IN (" + inClause + ") AND type = ? AND status = 'ACTIVE' AND ip_scope = 1 LIMIT 1"
                : "SELECT 1 FROM punishments WHERE target_uuid IN (" + inClause + ") AND type = ? AND status = 'ACTIVE' AND ip_scope = 1 AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL') LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            for (UUID uuid : players) {
                ps.setString(i++, uuid.toString());
            }
            ps.setString(i++, type.name());

            if (!checkGlobal) {
                ps.setString(i, serverName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check IP punishment: " + e.getMessage());
            return false;
        }
    }

    public Optional<PunishmentInfo> getActivePunishmentById(long id) {
        String sql = """
        SELECT * FROM punishments
        WHERE id = ?
          AND status = 'ACTIVE'
    """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PunishmentInfo(
                            rs.getLong("id"),
                            PunishmentType.valueOf(rs.getString("type")),
                            rs.getString("target_uuid") != null
                                    ? UUID.fromString(rs.getString("target_uuid"))
                                    : null,
                            rs.getInt("issuer_actor_id"),
                            rs.getString("reason"),
                            rs.getLong("created_at"),
                            rs.getLong("duration"),
                            rs.getString("server_scope"),
                            rs.getBoolean("ip_scope")
                    ));
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to fetch punishment by ID: " + e.getMessage());
        }

        return Optional.empty();
    }


    public PunishmentInfo getPunishmentInfo(PunishmentType type, UUID targetUuid, String serverName) {
        boolean checkGlobal = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");

        String sql = checkGlobal
                ? "SELECT * FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'"
                : "SELECT * FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, targetUuid.toString());
            ps.setString(2, type.name());

            if (!checkGlobal) {
                ps.setString(3, serverName);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PunishmentInfo(
                            rs.getLong("id"),
                            type,
                            targetUuid,
                            rs.getInt("issuer_actor_id"),
                            rs.getString("reason"),
                            rs.getLong("created_at"),
                            rs.getLong("duration"),
                            rs.getString("server_scope"),
                            rs.getBoolean("ip_scope")
                    );
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get punishment info: " + e.getMessage());
        }

        return null;
    }

    public int getActiveWarningCount(UUID targetUuid, String serverScope) {
        boolean checkGlobal = serverScope == null || serverScope.isEmpty() || serverScope.equalsIgnoreCase("global");

        String sql = checkGlobal
                ? "SELECT COUNT(*) AS count FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'"
                : "SELECT COUNT(*) AS count FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, targetUuid.toString());
            ps.setString(2, PunishmentType.WARNING.name());

            if (!checkGlobal) {
                ps.setString(3, serverScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get active warning count: " + e.getMessage());
        }

        return 0;
    }

    public static class PunishmentInfo {
        public final long id;
        public final PunishmentType type;
        public final UUID targetUuid;
        public final int issuerActorId;
        public final String reason;
        public final long createdAt;
        public final long duration;
        public final String serverScope;
        public final boolean ipScope;

        public PunishmentInfo(long id, PunishmentType type, UUID targetUuid, int issuerActorId,
                              String reason, long createdAt, long duration, String serverScope, boolean ipScope) {
            this.id = id;
            this.type = type;
            this.targetUuid = targetUuid;
            this.issuerActorId = issuerActorId;
            this.reason = reason;
            this.createdAt = createdAt;
            this.duration = duration;
            this.serverScope = serverScope;
            this.ipScope = ipScope;
        }

        public long getExpiresAt() {
            return duration > 0 ? createdAt + duration : 0;
        }

        public boolean isPermanent() {
            return duration <= 0;
        }
    }
}