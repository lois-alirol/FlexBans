package fr.neocle.flexbans.database.punishment;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BansManager {
    private final DatabaseConnectionManager dbManager;
    private final ProfilesManager profilesManager;
    private final ScheduledExecutorService scheduler;

    public BansManager(DatabaseConnectionManager dbManager, ProfilesManager profilesManager) {
        this.dbManager = dbManager;
        this.profilesManager = profilesManager;
        this.scheduler = Executors.newScheduledThreadPool(1);

        startExpirationScheduler();
    }

    private void startExpirationScheduler() {
        scheduler.scheduleWithFixedDelay(this::updateExpiredBans, 15, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            FlexLogger.info("Ban expiration scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            FlexLogger.warn("Ban expiration scheduler interrupted while shutting down");
        }
    }

    public void updateExpiredBans() {
        String query = "UPDATE flexbans_bans SET status = 'expired' " +
                "WHERE status = 'active' AND duration > 0 AND (time + duration) < ?";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            FlexLogger.error("Failed to update expired bans: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insertBan(UUID targetUUID, String targetName, UUID issuerUUID, String issuerName, String reason, long duration, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {
        String query = "INSERT INTO flexbans_bans (target_uuid, target_name, issuer_uuid, issuer_name, reason, time, duration, server_scope, server_origin, silent, ip_scope) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, targetUUID.toString());
            stmt.setString(2, targetName);
            stmt.setString(3, issuerUUID.toString());
            stmt.setString(4, issuerName);
            stmt.setString(5, reason);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.setLong(7, duration);
            stmt.setString(8, serverScope);
            stmt.setString(9, serverOrigin);
            stmt.setBoolean(10, silent);
            stmt.setBoolean(11, ipScope);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePreviousBanStatus(UUID targetUUID) {
        String query = "UPDATE flexbans_bans SET status = 'expired' WHERE target_uuid = ? AND status = 'active'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            FlexLogger.error("Failed to update previous bans for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeBan(UUID targetUUID,
                          UUID removerUUID,
                          String removerUsername,
                          String removalReason,
                          String serverScope) {

        boolean scoped = serverScope != null
                && !serverScope.equalsIgnoreCase("global");

        String sql;

        if (scoped) {
            sql = """
            UPDATE flexbans_bans
            SET remover_uuid = ?,
                remover_name = ?,
                removal_reason = ?,
                removal_time = ?,
                status = 'removed'
            WHERE target_uuid = ?
              AND status = 'active'
              AND server_scope = ?
        """;
        } else {
            sql = """
            UPDATE flexbans_bans
            SET remover_uuid = ?,
                remover_name = ?,
                removal_reason = ?,
                removal_time = ?,
                status = 'removed'
            WHERE target_uuid = ?
              AND status = 'active'
        """;
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            long now = System.currentTimeMillis();

            stmt.setString(1, removerUUID.toString());
            stmt.setString(2, removerUsername);
            stmt.setString(3, removalReason);
            stmt.setLong(4, now);
            stmt.setString(5, targetUUID.toString());

            if (scoped) {
                stmt.setString(6, serverScope);
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UUID getBannedUuidFromIp(InetAddress ip, String serverName) {
        try {
            List<UUID> playersWithIp = profilesManager.getPlayersByIp(ip);

            if (playersWithIp.isEmpty()) return null;

            boolean checkGlobalOnly = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");
            String serverScope = checkGlobalOnly ? "Global" : serverName;

            String inClause = playersWithIp.stream(). map(u -> "? ").collect(Collectors.joining(","));

            String sql = "SELECT target_uuid FROM flexbans_bans " +
                    "WHERE target_uuid IN (" + inClause + ") " +
                    "AND status='active' " +
                    "AND ip_scope=1 ";

            if (checkGlobalOnly) {
                sql += "AND server_scope='Global'";
            } else {
                sql += "AND (server_scope=? OR UPPER(server_scope)='GLOBAL')";
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int i = 1;
                for (UUID uuid : playersWithIp) stmt.setString(i++, uuid.toString());

                if (! checkGlobalOnly) stmt.setString(i, serverScope);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String bannedUuidStr = rs.getString("target_uuid");
                        return UUID.fromString(bannedUuidStr);
                    }
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get banned UUID from IP " + ip + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean isPlayerBanned(UUID uuid, String serverName) {
        final boolean scopedServer = serverName != null
                && !serverName.isEmpty()
                && !serverName.equalsIgnoreCase("global");

        final String sql;
        if (scopedServer) {
            sql = """
            SELECT 1 FROM flexbans_bans
            WHERE target_uuid = ?
              AND status = 'active'
              AND (server_scope = ? OR UPPER(server_scope) = 'GLOBAL')
            """;
        } else {
            sql = """
            SELECT 1 FROM flexbans_bans
            WHERE target_uuid = ?
              AND status = 'active'
            """;
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            if (scopedServer) {
                statement.setString(2, serverName);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isPlayerBannedGlobal(UUID uuid) {
        final String sql = """
        SELECT 1 FROM flexbans_bans
        WHERE target_uuid = ?
          AND status = 'active'
          AND UPPER(server_scope) = 'GLOBAL'
    """;

        try (Connection connection = dbManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isIpBanned(InetAddress address, String serverName) {
        try {
            List<UUID> playersWithIp = profilesManager.getPlayersByIp(address);

            if (playersWithIp.isEmpty()) return false;

            boolean checkGlobalOnly = serverName == null || serverName.isEmpty() || serverName.equalsIgnoreCase("global");
            String serverScope = checkGlobalOnly ? "Global" : serverName;

            String inClause = playersWithIp.stream().map(u -> "?").collect(Collectors.joining(","));

            String sql = "SELECT 1 FROM flexbans_bans " +
                    "WHERE target_uuid IN (" + inClause + ") " +
                    "AND status='active' " +
                    "AND ip_scope=1 ";

            if (checkGlobalOnly) {
                sql += "AND server_scope='Global'";
            } else {
                sql += "AND (server_scope=? OR UPPER(server_scope)='GLOBAL')";
            }

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int i = 1;
                for (UUID uuid : playersWithIp) stmt.setString(i++, uuid.toString());

                if (!checkGlobalOnly) stmt.setString(i, serverScope);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[IP BAN] Player with IP " + address + " is banned on server " + serverScope);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getReason(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT reason FROM flexbans_bans WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT reason FROM flexbans_bans WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reason");
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get ban reason for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public long getDuration(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT duration FROM flexbans_bans WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT duration FROM flexbans_bans WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("duration");
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get ban duration for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public long getTime(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT time FROM flexbans_bans WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT time FROM flexbans_bans WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("time");
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get ban time for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public String getIssuer(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT issuer_name FROM flexbans_bans WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT issuer_name FROM flexbans_bans WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("issuer_name");
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get ban issuer for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public void printEntireDatabase() {
        String query = "SELECT * FROM flexbans_bans";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            FlexLogger.info("=== Bans Table ===");

            while (rs.next()) {
                FlexLogger.info("ID: " + rs.getInt("id") +
                        ", Target UUID: " + rs.getString("target_uuid") +
                        ", Target Name: " + rs.getString("target_name") +
                        ", Issuer UUID: " + rs.getString("issuer_uuid") +
                        ", Issuer Name: " + rs.getString("issuer_name") +
                        ", Reason: " + rs.getString("reason") +
                        ", Ban Time: " + rs.getLong("time") +
                        ", Duration: " + rs.getLong("duration") +
                        ", Server Scope: " + rs.getString("server_scope") +
                        ", Status: " + rs.getString("status") +
                        ", Silent: " + rs.getBoolean("silent"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}