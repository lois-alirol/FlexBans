package fr.neocle.flexbans.database.player;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ProfilesManager {
    private final DatabaseConnectionManager dbManager;
    private final ExecutorService dbExecutor;

    private static final FlexLogger LOGGER = FlexLogger.get(ProfilesManager.class);

    public ProfilesManager(DatabaseConnectionManager dbManager, ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Void> recordPlayerLogin(UUID uuid, String username, InetAddress address) {
        return CompletableFuture.runAsync(() -> {
            String uuidStr = uuid.toString();
            long now = System.currentTimeMillis();

            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    updatePlayerTimestamps(conn, uuidStr, now);
                    updateUsernameHistory(conn, uuidStr, username, now);
                    updateIpHistory(conn, uuidStr, address.getAddress(), now);
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to record login for {}: ", uuid, e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> recordUsername(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            String uuidStr = uuid.toString();
            long now = System.currentTimeMillis();

            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    updatePlayerTimestamps(conn, uuidStr, now);
                    updateUsernameHistory(conn, uuidStr, username, now);
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to record username for {}: ", uuid, e);
            }
        }, dbExecutor);
    }

    private void updatePlayerTimestamps(Connection conn, String uuid, long now) throws SQLException {
        if (isH2Database(conn)) {
            updatePlayerTimestampsH2(conn, uuid, now);
        } else {
            String sql = getUpsertSql(conn, "players");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setLong(2, now);
                ps.setLong(3, now);
                ps.executeUpdate();
            }
        }
    }

    private void updateUsernameHistory(Connection conn, String uuid, String username, long now) throws SQLException {
        if (isH2Database(conn)) {
            updateUsernameHistoryH2(conn, uuid, username, now);
        } else {
            String sql = getUpsertSql(conn, "player_names");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setString(2, username);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
        }
    }

    private void updateIpHistory(Connection conn, String uuid, byte[] ipBytes, long now) throws SQLException {
        if (isH2Database(conn)) {
            updateIpHistoryH2(conn, uuid, ipBytes, now);
        } else {
            String sql = getUpsertSql(conn, "player_ips");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setBytes(2, ipBytes);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
        }
    }

    private void updatePlayerTimestampsH2(Connection conn, String uuid, long now) throws SQLException {
        String update = "UPDATE players SET last_seen = ? WHERE uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setLong(1, now);
            ps.setString(2, uuid);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                String insert = "INSERT INTO players (uuid, first_seen, last_seen) VALUES (?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insert)) {
                    insertPs.setString(1, uuid);
                    insertPs.setLong(2, now);
                    insertPs.setLong(3, now);
                    insertPs.executeUpdate();
                } catch (SQLException e) {
                    if (e.getErrorCode() == 23505) {
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.executeUpdate();
                        }
                    } else throw e;
                }
            }
        }
    }

    private void updateUsernameHistoryH2(Connection conn, String uuid, String username, long now) throws SQLException {
        String update = "UPDATE player_names SET last_seen = ? WHERE player_uuid = ? AND mc_username = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setLong(1, now);
            ps.setString(2, uuid);
            ps.setString(3, username);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                String insert = "INSERT INTO player_names (player_uuid, mc_username, first_seen, last_seen) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insert)) {
                    insertPs.setString(1, uuid);
                    insertPs.setString(2, username);
                    insertPs.setLong(3, now);
                    insertPs.setLong(4, now);
                    insertPs.executeUpdate();
                } catch (SQLException e) {
                    if (e.getErrorCode() == 23505) {
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.setString(3, username);
                            retryPs.executeUpdate();
                        }
                    } else throw e;
                }
            }
        }
    }

    private void updateIpHistoryH2(Connection conn, String uuid, byte[] ipBytes, long now) throws SQLException {
        String update = "UPDATE player_ips SET last_seen = ? WHERE player_uuid = ? AND ip = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setLong(1, now);
            ps.setString(2, uuid);
            ps.setBytes(3, ipBytes);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                String insert = "INSERT INTO player_ips (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insert)) {
                    insertPs.setString(1, uuid);
                    insertPs.setBytes(2, ipBytes);
                    insertPs.setLong(3, now);
                    insertPs.setLong(4, now);
                    insertPs.executeUpdate();
                } catch (SQLException e) {
                    if (e.getErrorCode() == 23505) {
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.setBytes(3, ipBytes);
                            retryPs.executeUpdate();
                        }
                    } else throw e;
                }
            }
        }
    }

    private boolean isH2Database(Connection conn) throws SQLException {
        return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
    }

    // --- Public read methods ---

    public CompletableFuture<List<String>> getAllUsernames(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT mc_username FROM player_names WHERE player_uuid = ? ORDER BY first_seen ASC";
            List<String> names = new ArrayList<>();

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) names.add(rs.getString("mc_username"));
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get usernames for {}: ", uuid, e);
            }

            return names;
        }, dbExecutor);
    }

    public CompletableFuture<List<String>> getAllIps(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT ip FROM player_ips WHERE player_uuid = ? ORDER BY last_seen DESC";
            List<String> results = new ArrayList<>();

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        byte[] bytes = rs.getBytes("ip");
                        results.add(InetAddress.getByAddress(bytes).getHostAddress());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get IPs for {}: ", uuid, e);
            }

            return results;
        }, dbExecutor);
    }

    // NOTE: this one stays synchronous — it's called internally from
    // PunishmentsManager.isIpPunished() which is already running on the DB thread
    public List<UUID> getPlayersByIp(InetAddress address) {
        String sql = "SELECT player_uuid FROM player_ips WHERE ip = ?";
        List<UUID> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBytes(1, address.getAddress());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(UUID.fromString(rs.getString("player_uuid")));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get players by IP: ", e);
        }

        return results;
    }

    public CompletableFuture<String> getCurrentUsername(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT mc_username FROM player_names WHERE player_uuid = ? ORDER BY last_seen DESC LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("mc_username");
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get current username for {}: ", uuid, e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<UUID> getUuid(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_uuid FROM player_names WHERE LOWER(mc_username) = LOWER(?) ORDER BY last_seen DESC LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return UUID.fromString(rs.getString("player_uuid"));
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get UUID for username {}: ", username, e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<InetAddress> getIp(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT pi.ip FROM player_ips pi
                INNER JOIN player_names pn ON pi.player_uuid = pn.player_uuid
                WHERE LOWER(pn.mc_username) = LOWER(?)
                ORDER BY pi.last_seen DESC
                LIMIT 1
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return InetAddress.getByAddress(rs.getBytes("ip"));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get IP for username {}: ", username, e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to check if player exists: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<List<ModeratorHistoryEntry>> getIssuedPunishments(UUID moderatorUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ModeratorHistoryEntry> entries = new ArrayList<>();

            String query = """
                SELECT p.id, p.type, pn.mc_username AS target_name, p.reason,
                       p.created_at, p.duration, p.status, p.ip_scope
                FROM punishments p
                JOIN actors a ON a.id = p.issuer_actor_id
                JOIN player_names pn ON pn.player_uuid = p.target_uuid
                WHERE a.player_uuid = ?
                ORDER BY p.created_at DESC
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, moderatorUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean ipScope = rs.getBoolean("ip_scope");
                        String baseType = rs.getString("type").toLowerCase();
                        entries.add(new ModeratorHistoryEntry(
                                rs.getInt("id"), "issued",
                                ipScope ? "ip-" + baseType : baseType,
                                rs.getString("target_name"), rs.getString("reason"),
                                rs.getLong("created_at"), rs.getLong("duration"),
                                rs.getString("status"), null, ipScope
                        ));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch issued punishments for {}: ", moderatorUuid, e);
            }

            return entries;
        }, dbExecutor);
    }

    public CompletableFuture<List<ModeratorHistoryEntry>> getRemovedPunishments(UUID moderatorUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<ModeratorHistoryEntry> entries = new ArrayList<>();

            String query = """
                SELECT p.id, p.type, pn.mc_username AS target_name, pa.reason AS removal_reason,
                       pa.action_time, p.ip_scope
                FROM punishment_actions pa
                JOIN punishments p ON p.id = pa.punishment_id
                JOIN actors a ON a.id = pa.actor_id
                JOIN player_names pn ON pn.player_uuid = p.target_uuid
                WHERE a.player_uuid = ?
                  AND pa.action IN ('UNBAN', 'UNMUTE', 'UNWARN')
                ORDER BY pa.action_time DESC
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, moderatorUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean ipScope = rs.getBoolean("ip_scope");
                        String baseType = rs.getString("type").toLowerCase();
                        entries.add(new ModeratorHistoryEntry(
                                rs.getInt("id"), "removed",
                                ipScope ? "ip-" + baseType : baseType,
                                rs.getString("target_name"), rs.getString("removal_reason"),
                                rs.getLong("action_time"), 0, "removed", null, ipScope
                        ));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch removed punishments for {}: ", moderatorUuid, e);
            }

            return entries;
        }, dbExecutor);
    }

    public CompletableFuture<List<HistoryEntry>> getPlayerHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<HistoryEntry> entries = new ArrayList<>();

            String query = """
                SELECT p.id, p.type, a.name AS issuer_name, p.reason,
                       p.created_at, p.status, p.ip_scope,
                       pa.reason AS removal_reason
                FROM punishments p
                JOIN actors a ON a.id = p.issuer_actor_id
                LEFT JOIN punishment_actions pa
                       ON pa.punishment_id = p.id
                      AND pa.action IN ('UNBAN', 'UNMUTE', 'UNWARN', 'EXPIRE')
                WHERE p.target_uuid = ?
                ORDER BY p.created_at DESC
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean ipScope = rs.getBoolean("ip_scope");
                        String baseType = rs.getString("type").toLowerCase();
                        entries.add(new HistoryEntry(
                                rs.getInt("id"),
                                ipScope ? "ip-" + baseType : baseType,
                                rs.getString("issuer_name"),
                                rs.getString("reason"),
                                rs.getLong("created_at"),
                                rs.getString("status"),
                                rs.getString("removal_reason"),
                                ipScope
                        ));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to fetch history for {}: ", playerUuid, e);
            }

            return entries;
        }, dbExecutor);
    }

    private String getUpsertSql(Connection conn, String table) throws SQLException {
        String db = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (db.contains("mysql")) {
            return switch (table) {
                case "players" -> "INSERT INTO players (uuid, first_seen, last_seen) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
                case "player_names" -> "INSERT INTO player_names (player_uuid, mc_username, first_seen, last_seen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
                case "player_ips" -> "INSERT INTO player_ips (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
                default -> throw new SQLException("Unknown table: " + table);
            };
        }

        if (db.contains("sqlite")) {
            return switch (table) {
                case "players" -> "INSERT INTO players (uuid, first_seen, last_seen) VALUES (?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET last_seen = excluded.last_seen";
                case "player_names" -> "INSERT INTO player_names (player_uuid, mc_username, first_seen, last_seen) VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, mc_username) DO UPDATE SET last_seen = excluded.last_seen";
                case "player_ips" -> "INSERT INTO player_ips (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, ip) DO UPDATE SET last_seen = excluded.last_seen";
                default -> throw new SQLException("Unknown table: " + table);
            };
        }

        throw new SQLException("Unsupported DB: " + db);
    }

    public record HistoryEntry(
            int id, String type, String issuerName, String reason,
            long time, String status, String removalReason, boolean ipScope
    ) {}

    public record ModeratorHistoryEntry(
            int id, String action, String type, String targetName,
            String reason, long timestamp, long duration,
            String status, String removedBy, boolean ipScope
    ) {}
}