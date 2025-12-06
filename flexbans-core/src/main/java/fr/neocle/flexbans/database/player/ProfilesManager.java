package fr.neocle.flexbans.database.player;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.sql.*;
import java.util.*;

public class ProfilesManager {
    private final DatabaseConnectionManager dbManager;

    public ProfilesManager(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    public void recordPlayerLogin(UUID uuid, String username, InetAddress address) {
        String uuidStr = uuid.toString();
        long now = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            updateProfileTimestamps(conn, uuidStr, now);
            updateUsernameHistory(conn, uuidStr, username, now);
            updateIpHistory(conn, uuidStr, address.getAddress(), now);

            conn.commit();
        } catch (SQLException e) {
            FlexLogger.error("Failed to record login for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateProfileTimestamps(Connection conn, String uuid, long now) throws SQLException {
        String sql = """
                INSERT INTO flexbans_profiles (uuid, first_seen, last_seen)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET last_seen = excluded.last_seen
                """;

        if (conn.getMetaData().getDatabaseProductName().contains("MySQL")) {
            sql = """
                INSERT INTO flexbans_profiles (uuid, first_seen, last_seen)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)
                """;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setLong(2, now);
            ps.setLong(3, now);
            ps.executeUpdate();
        }
    }

    private void updateUsernameHistory(Connection conn, String uuid, String username, long now) throws SQLException {
        String sql = """
                INSERT INTO flexbans_names (uuid, username, first_seen, last_seen)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid, username) DO UPDATE SET last_seen = excluded.last_seen
                """;

        if (conn.getMetaData().getDatabaseProductName().contains("MySQL")) {
            sql = """
                INSERT INTO flexbans_names (uuid, username, first_seen, last_seen)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)
                """;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, username);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    private void updateIpHistory(Connection conn, String uuid, byte[] ipBytes, long now) throws SQLException {
        String sql = """
                INSERT INTO flexbans_ips (uuid, ip, first_seen, last_seen)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid, ip) DO UPDATE SET last_seen = excluded.last_seen
                """;

        if (conn.getMetaData().getDatabaseProductName().contains("MySQL")) {
            sql = """
                INSERT INTO flexbans_ips (uuid, ip, first_seen, last_seen)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)
                """;
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setBytes(2, ipBytes);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    public List<String> getAllUsernames(UUID uuid) {
        String sql = "SELECT username FROM flexbans_names WHERE uuid = ? ORDER BY first_seen ASC";
        List<String> names = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get usernames for " + uuid + ": " + e.getMessage());
        }

        return names;
    }

    public List<String> getAllIps(UUID uuid) {
        String sql = "SELECT ip FROM flexbans_ips WHERE uuid = ?";
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
        String sql = "SELECT uuid FROM flexbans_ips WHERE ip = ?";
        List<UUID> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, address.getAddress());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("uuid"));
                    results.add(playerUuid);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    public String getCurrentUsername(UUID uuid) {
        String sql = """
            SELECT username
            FROM flexbans_names
            WHERE uuid = ?
            ORDER BY last_seen DESC
            LIMIT 1
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get current username for " + uuid + ": " + e.getMessage());
        }

        return null;
    }

    public UUID getUuid(String username) {
        String sql = """
            SELECT uuid FROM flexbans_names
            WHERE LOWER(username) = LOWER(?)
            ORDER BY last_seen DESC
            LIMIT 1
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to get UUID for username " + username + ": " + e.getMessage());
        }

        return null;
    }

    public InetAddress getIp(String username) {
        String sql = """
        SELECT ip FROM flexbans_ips
        WHERE uuid = (
            SELECT uuid FROM flexbans_names
            WHERE LOWER(username) = LOWER(?)
            ORDER BY last_seen DESC
            LIMIT 1
        )
        ORDER BY last_seen DESC
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

        } catch (SQLException e) {
            FlexLogger.error("Failed to get IP for username " + username + ": " + e.getMessage());
        } catch (Exception e) {
            FlexLogger.error("Failed to parse IP address for username " + username + ": " + e.getMessage());
        }

        return null;
    }

    public void printProfile(UUID uuid) {
        FlexLogger.info("=== Profile of " + uuid + " ===");
        FlexLogger.info("Usernames: " + getAllUsernames(uuid));
        FlexLogger.info("IPs: " + getAllIps(uuid));
    }
}
