package fr.neocle.flexbans.database.player;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.sql.*;
import java.util.*;

/**
 * Manages player profiles, usernames, and IP address history.
 * Uses the new schema with separate tables: players, player_names, player_ips.
 */
public class ProfilesManager {
    private final DatabaseConnectionManager dbManager;

    public ProfilesManager(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Record a player login - updates player profile, username, and IP history.
     */
    public void recordPlayerLogin(UUID uuid, String username, InetAddress address) {
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
            FlexLogger.error("Failed to record login for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Record just a username (e.g., when creating a player record without full login).
     */
    public void recordUsername(UUID uuid, String username) {
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
            FlexLogger.error("Failed to record username for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update player timestamps (first_seen and last_seen).
     */
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

    /**
     * Update username history.
     */
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

    /**
     * Update IP address history.
     */
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

    // =========================
    // H2-specific update methods
    // =========================

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
                    // Handle race condition - row was inserted by another thread
                    if (e.getErrorCode() == 23505) {
                        // Retry the update
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.executeUpdate();
                        }
                    } else {
                        throw e;
                    }
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
                    // Handle race condition - row was inserted by another thread
                    if (e.getErrorCode() == 23505) {
                        // Retry the update
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.setString(3, username);
                            retryPs.executeUpdate();
                        }
                    } else {
                        throw e;
                    }
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
                    // Handle race condition - row was inserted by another thread
                    if (e.getErrorCode() == 23505) {
                        // Retry the update
                        try (PreparedStatement retryPs = conn.prepareStatement(update)) {
                            retryPs.setLong(1, now);
                            retryPs.setString(2, uuid);
                            retryPs.setBytes(3, ipBytes);
                            retryPs.executeUpdate();
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private boolean isH2Database(Connection conn) throws SQLException {
        return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
    }

    // =========================
    // Fetch Methods
    // =========================

    public List<String> getAllUsernames(UUID uuid) {
        String sql = "SELECT mc_username FROM player_names WHERE player_uuid = ? ORDER BY first_seen ASC";
        List<String> names = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("mc_username"));
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get usernames for " + uuid + ": " + e.getMessage());
        }

        return names;
    }

    public List<String> getAllIps(UUID uuid) {
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
            FlexLogger.error("Failed to get IPs for " + uuid + ": " + e.getMessage());
        }

        return results;
    }

    public List<UUID> getPlayersByIp(InetAddress address) {
        String sql = "SELECT player_uuid FROM player_ips WHERE ip = ?";
        List<UUID> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, address.getAddress());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    results.add(playerUuid);
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get players by IP: " + e.getMessage());
        }

        return results;
    }

    public String getCurrentUsername(UUID uuid) {
        String sql = "SELECT mc_username FROM player_names WHERE player_uuid = ? ORDER BY last_seen DESC LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("mc_username");
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get current username for " + uuid + ": " + e.getMessage());
        }

        return null;
    }

    public UUID getUuid(String username) {
        String sql = "SELECT player_uuid FROM player_names WHERE LOWER(mc_username) = LOWER(?) ORDER BY last_seen DESC LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get UUID for username " + username + ": " + e.getMessage());
        }

        return null;
    }

    public InetAddress getIp(String username) {
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
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("ip");
                    return InetAddress.getByAddress(bytes);
                }
            }

        } catch (Exception e) {
            FlexLogger.error("Failed to get IP for username " + username + ": " + e.getMessage());
        }

        return null;
    }

    public boolean playerExists(UUID uuid) {
        String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check if player exists: " + e.getMessage());
            return false;
        }
    }

    public void printProfile(UUID uuid) {
        FlexLogger.info("=== Profile of " + uuid + " ===");
        FlexLogger.info("Usernames: " + getAllUsernames(uuid));
        FlexLogger.info("IPs: " + getAllIps(uuid));
    }

    // =========================
    // Upsert SQL generator
    // =========================

    private String getUpsertSql(Connection conn, String table) throws SQLException {
        String db = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (db.contains("mysql")) {
            switch (table) {
                case "players":
                    return "INSERT INTO players (uuid, first_seen, last_seen) VALUES (?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
                case "player_names":
                    return "INSERT INTO player_names (player_uuid, mc_username, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
                case "player_ips":
                    return "INSERT INTO player_ips (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
            }
        }

        if (db.contains("sqlite")) {
            switch (table) {
                case "players":
                    return "INSERT INTO players (uuid, first_seen, last_seen) VALUES (?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET last_seen = excluded.last_seen";
                case "player_names":
                    return "INSERT INTO player_names (player_uuid, mc_username, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(player_uuid, mc_username) DO UPDATE SET last_seen = excluded.last_seen";
                case "player_ips":
                    return "INSERT INTO player_ips (player_uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(player_uuid, ip) DO UPDATE SET last_seen = excluded.last_seen";
            }
        }

        throw new SQLException("Unsupported DB: " + db);
    }
}