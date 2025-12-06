package fr.neocle.flexbans.database.punishment;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WarningsManager {
    private final DatabaseConnectionManager dbManager;
    private final ScheduledExecutorService scheduler;

    public WarningsManager(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
        this.scheduler = Executors.newScheduledThreadPool(1);

        startExpirationScheduler();
    }

    private void startExpirationScheduler() {
        scheduler.scheduleWithFixedDelay(this::updateExpiredWarnings, 15, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            FlexLogger.info("Warning expiration scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            FlexLogger.warn("Warning expiration scheduler interrupted while shutting down");
        }
    }

    public void updateExpiredWarnings() {
        int expiresAfterDays = ConfigManager.getInt("punishments-system.built-in.warnings.expires-after-days");
        long expirationTime = TimeUnit.DAYS.toMillis(expiresAfterDays);

        String query = "UPDATE flexbans_warnings SET status = 'expired' " +
                "WHERE status = 'active' AND (time + ?) < ?";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, expirationTime);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            FlexLogger.error("Failed to update expired warnings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insertWarning(UUID targetUUID, String targetName, UUID issuerUUID, String issuerName, String reason, long duration, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {
        String query = "INSERT INTO flexbans_warnings (target_uuid, target_name, issuer_uuid, issuer_name, reason, time, duration, server_scope, server_origin, silent, ip_scope) " +
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

    public void updatePreviousWarningStatus(UUID targetUUID) {
        String query = "UPDATE flexbans_warnings SET status = 'expired' WHERE target_uuid = ? AND status = 'active'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            FlexLogger.error("Failed to update previous warnings for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeWarning(UUID targetUUID, UUID removerUUID, String removerUsername, String removalReason) {
        String query = "UPDATE flexbans_warnings SET remover_uuid = ?, remover_name = ?, removal_reason = ?, removal_time = ?, status = 'removed' WHERE target_uuid = ? AND status = 'active'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, removerUUID.toString());
            stmt.setString(2, removerUsername);
            stmt.setString(3, removalReason);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, targetUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasActiveWarning(UUID uuid, String serverName) {
        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            try (Connection connection = dbManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = ? AND status = 'active'"
                 )) {

                statement.setString(1, uuid.toString());
                statement.setString(2, serverName);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'"
             )) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isIpWarned(String ip, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = """
                        SELECT fw.id
                        FROM flexbans_warnings fw
                        JOIN flexbans_history fh ON fw.target_uuid = fh.player_uuid
                        WHERE fh.ip = ?
                        AND fw.status = 'active'
                        AND fw.server_scope = ?
                    """;
        } else {
            query = """
                        SELECT fw.id
                        FROM flexbans_warnings fw
                        JOIN flexbans_history fh ON fw.target_uuid = fh.player_uuid
                        WHERE fh.ip = ?
                        AND fw.status = 'active'
                        AND fw.server_scope = 'Global'
                    """;
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ip);

            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return true;
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
            query = "SELECT reason FROM flexbans_warnings WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT reason FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
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
            FlexLogger.error("Failed to get warning reason for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public long getDuration(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT duration FROM flexbans_warnings WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT duration FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
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
            FlexLogger.error("Failed to get warning duration for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public long getTime(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT time FROM flexbans_warnings WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT time FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
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
            FlexLogger.error("Failed to get warning time for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public String getIssuer(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT issuer_name FROM flexbans_warnings WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT issuer_name FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
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
            FlexLogger.error("Failed to get warning issuer for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public int getWarningCount(UUID targetUUID, String serverName) {
        String query;

        if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
            query = "SELECT COUNT(*) as count FROM flexbans_warnings WHERE target_uuid = ? AND (server_scope = ? OR server_scope = 'Global') AND status = 'active'";
        } else {
            query = "SELECT COUNT(*) as count FROM flexbans_warnings WHERE target_uuid = ? AND server_scope = 'Global' AND status = 'active'";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, targetUUID.toString());
            if (serverName != null && !serverName.isEmpty() && !serverName.equalsIgnoreCase("global")) {
                stmt.setString(2, serverName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get warning count for " + targetUUID + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public void printEntireDatabase() {
        String query = "SELECT * FROM flexbans_warnings";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            FlexLogger.info("=== Warnings Table ===");

            while (rs.next()) {
                FlexLogger.info("ID: " + rs.getInt("id") +
                        ", Target UUID: " + rs.getString("target_uuid") +
                        ", Target Name: " + rs.getString("target_name") +
                        ", Issuer UUID: " + rs.getString("issuer_uuid") +
                        ", Issuer Name: " + rs.getString("issuer_name") +
                        ", Reason: " + rs.getString("reason") +
                        ", Warning Time: " + rs.getLong("time") +
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