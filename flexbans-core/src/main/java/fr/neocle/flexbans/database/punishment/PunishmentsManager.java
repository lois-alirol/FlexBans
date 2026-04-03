package fr.neocle.flexbans.database.punishment;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class PunishmentsManager {
    private final DatabaseConnectionManager dbManager;
    private final ActorsManager actorsManager;
    private final ProfilesManager profilesManager;
    private final ExecutorService dbExecutor;

    private static final FlexLogger LOGGER = FlexLogger.get(PunishmentsManager.class);

    private static final UUID CONSOLE_UUID =
            UUID.fromString("f78a4d8d-d51b-4b39-98a3-230f2de0c670");

    public enum PunishmentType {
        BAN, MUTE, KICK, WARNING
    }

    public PunishmentsManager(DatabaseConnectionManager dbManager, ActorsManager actorsManager,
                              ProfilesManager profilesManager, ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.actorsManager = actorsManager;
        this.profilesManager = profilesManager;
        this.dbExecutor = dbExecutor;
    }

    public void start() {
        TaskScheduler.get().runRepeating(
                () -> CompletableFuture.runAsync(this::updateExpiredPunishments, dbExecutor),
                5000
        );
    }

    private CompletableFuture<Void> updateExpiredPunishments() {
        return CompletableFuture.runAsync(() -> {
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
                    LOGGER.info("Auto-expired {} punishment(s)", updated);
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to update expired punishments: ", e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Long> insertPunishment(
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
        CompletableFuture<Integer> actorFuture = CONSOLE_UUID.equals(issuerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(issuerUuid, issuerName);

        return actorFuture.thenCompose(actorId ->
                CompletableFuture.supplyAsync(() -> {
                    long now = System.currentTimeMillis();
                    Long expiresAt = duration > 0 ? now + duration : null;

                    String sql =
                            "INSERT INTO punishments (" +
                                    "type, target_uuid, ip, issuer_actor_id, created_at, " +
                                    "reason, duration, expires_at, server_scope, server_origin, " +
                                    "silent, status, ip_scope" +
                                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";

                    try (Connection conn = dbManager.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, type.name());
                        ps.setString(2, targetUuid != null ? targetUuid.toString() : null);
                        ps.setBytes(3, targetIp != null ? targetIp.getAddress() : null);
                        ps.setInt(4, actorId);
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
                            if (rs.next()) return rs.getLong(1);
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Failed to insert punishment", e);
                    }

                    return -1L;
                }, dbExecutor)
        );
    }

    public CompletableFuture<Boolean> removePunishment(
            PunishmentType type,
            UUID targetUuid,
            UUID removerUuid,
            String removerName,
            String removalReason,
            String serverScope
    ) {
        CompletableFuture<Integer> actorFuture = CONSOLE_UUID.equals(removerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(removerUuid, removerName);

        return actorFuture.thenCompose(removerActorId ->
                CompletableFuture.supplyAsync(() -> {
                    boolean isScoped = serverScope != null && !serverScope.equalsIgnoreCase("global");

                    String selectSql = isScoped
                            ? "SELECT id FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND server_scope = ?"
                            : "SELECT id FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'";

                    String updateSql = isScoped
                            ? "UPDATE punishments SET status = 'REMOVED' WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND server_scope = ?"
                            : "UPDATE punishments SET status = 'REMOVED' WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'";

                    try (Connection conn = dbManager.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
                            long punishmentId = -1;

                            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                                ps.setString(1, targetUuid.toString());
                                ps.setString(2, type.name());
                                if (isScoped) ps.setString(3, serverScope);

                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) punishmentId = rs.getLong("id");
                                }
                            }

                            if (punishmentId == -1) {
                                conn.rollback();
                                return false;
                            }

                            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                                ps.setString(1, targetUuid.toString());
                                ps.setString(2, type.name());
                                if (isScoped) ps.setString(3, serverScope);

                                ps.executeUpdate();
                            }

                            recordPunishmentAction(punishmentId, removerActorId, "REMOVED", removalReason);
                            conn.commit();
                            return true;

                        } catch (SQLException e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Failed to remove punishment", e);
                    }

                    return false;
                }, dbExecutor)
        );
    }

    public CompletableFuture<Boolean> removePunishmentById(
            long punishmentId,
            UUID removerUuid,
            String removerName,
            String removalReason
    ) {
        CompletableFuture<Integer> actorFuture = CONSOLE_UUID.equals(removerUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(removerUuid, removerName);

        return actorFuture.thenCompose(removerActorId ->
                CompletableFuture.supplyAsync(() -> {
                    String checkSql = "SELECT id FROM punishments WHERE id = ? AND status = 'ACTIVE'";
                    String updateSql = "UPDATE punishments SET status = 'REMOVED' WHERE id = ?";

                    try (Connection conn = dbManager.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
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

                            recordPunishmentAction(punishmentId, removerActorId, "REMOVED", removalReason);
                            conn.commit();
                            return true;

                        } catch (SQLException e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Failed to remove punishment by ID", e);
                    }

                    return false;
                }, dbExecutor)
        );
    }

    public CompletableFuture<Boolean> updatePunishment(
            long punishmentId,
            UUID updaterUuid,
            String updaterName,
            String newReason,
            long newDuration
    ) {
        CompletableFuture<Integer> actorFuture = CONSOLE_UUID.equals(updaterUuid)
                ? actorsManager.getOrCreateConsoleActor()
                : actorsManager.getOrCreatePlayerActor(updaterUuid, updaterName);

        return actorFuture.thenCompose(updaterActorId ->
                CompletableFuture.supplyAsync(() -> {
                    String checkSql = "SELECT id FROM punishments WHERE id = ? AND status = 'ACTIVE'";
                    String updateSql = "UPDATE punishments SET reason = ?, duration = ? WHERE id = ?";

                    try (Connection conn = dbManager.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
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

                            recordPunishmentAction(punishmentId, updaterActorId, "UPDATED", newReason);
                            conn.commit();
                            return true;

                        } catch (SQLException e) {
                            conn.rollback();
                            throw e;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Failed to update punishment by ID", e);
                    }

                    return false;
                }, dbExecutor)
        );
    }

    private CompletableFuture<Void> recordPunishmentAction(
            long punishmentId,
            int actorId,
            String action,
            String reason
    ) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
            INSERT INTO punishment_actions (punishment_id, actor_id, action, reason, action_time)
            VALUES (?, ?, ?, ?, ?)
        """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, punishmentId);
                ps.setInt(2, actorId);
                ps.setString(3, action);
                ps.setString(4, reason);
                ps.setLong(5, System.currentTimeMillis());

                ps.executeUpdate();

            } catch (SQLException e) {
                LOGGER.error("Failed to record punishment action: ", e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> isPlayerPunished(PunishmentType type, UUID uuid, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            boolean checkGlobal = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");

            String sql = checkGlobal
                    ? "SELECT 1 FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' LIMIT 1"
                    : "SELECT 1 FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL') LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                if (!checkGlobal) ps.setString(3, serverName);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to check punishment: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> isIpPunished(PunishmentType type, InetAddress address, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
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
                for (UUID uuid : players) ps.setString(i++, uuid.toString());
                ps.setString(i++, type.name());
                if (!checkGlobal) ps.setString(i, serverName);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to check IP punishment: ", e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Optional<PunishmentInfo>> getActivePunishmentById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE id = ? AND status = 'ACTIVE'";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapPunishmentInfo(rs));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch punishment by ID: ", e);
            }

            return Optional.<PunishmentInfo>empty();
        }, dbExecutor);
    }

    public CompletableFuture<PunishmentInfo> getPunishmentInfo(
            PunishmentType type, UUID targetUuid, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            boolean checkGlobal = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");

            String sql = checkGlobal
                    ? "SELECT * FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'"
                    : "SELECT * FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, targetUuid.toString());
                ps.setString(2, type.name());
                if (!checkGlobal) ps.setString(3, serverName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapPunishmentInfo(rs);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get punishment info: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<Integer> getActiveWarningCount(UUID targetUuid, String serverScope) {
        return CompletableFuture.supplyAsync(() -> {
            boolean checkGlobal = serverScope == null || serverScope.isEmpty() || serverScope.equalsIgnoreCase("global");

            String sql = checkGlobal
                    ? "SELECT COUNT(*) AS count FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE'"
                    : "SELECT COUNT(*) AS count FROM punishments WHERE target_uuid = ? AND type = ? AND status = 'ACTIVE' AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, targetUuid.toString());
                ps.setString(2, PunishmentType.WARNING.name());
                if (!checkGlobal) ps.setString(3, serverScope);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("count");
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get active warning count: ", e);
            }

            return 0;
        }, dbExecutor);
    }

    private PunishmentInfo mapPunishmentInfo(ResultSet rs) throws SQLException {
        return new PunishmentInfo(
                rs.getLong("id"),
                PunishmentType.valueOf(rs.getString("type")),
                rs.getString("target_uuid") != null ? UUID.fromString(rs.getString("target_uuid")) : null,
                rs.getInt("issuer_actor_id"),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getLong("duration"),
                rs.getString("server_scope"),
                rs.getBoolean("ip_scope")
        );
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