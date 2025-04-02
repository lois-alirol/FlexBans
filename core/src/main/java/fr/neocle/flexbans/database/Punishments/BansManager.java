package fr.neocle.flexbans.database.Punishments;

import fr.neocle.flexbans.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class BansManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public BansManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
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

    public void removeBan(UUID targetUUID, UUID removerUUID, String removerUsername, String removalReason) {
        String query = "UPDATE flexbans_bans SET remover_uuid = ?, remover_name = ?, removal_reason = ?, status = 'removed' WHERE target_uuid = ? AND status = 'active'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, removerUUID.toString());
            stmt.setString(2, removerUsername);
            stmt.setString(3, removalReason);
            stmt.setString(4, targetUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerBanned(UUID targetUUID) {
        String query = "SELECT * FROM flexbans_bans WHERE target_uuid = ? AND status = 'active'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, targetUUID.toString());

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

    public boolean isIpBanned(String ip) {
        String query = """
                    SELECT fb.id
                    FROM flexbans_bans fb
                    JOIN flexbans_history fh ON fb.target_uuid = fh.player_uuid
                    WHERE fh.ip = ?
                    AND fb.status = 'active'
                """;

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ip);

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


    public void printEntireDatabase() {
        String query = "SELECT * FROM flexbans_bans";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            logger.info("=== Bans Table ===");

            while (rs.next()) {
                logger.info("ID: " + rs.getInt("id") +
                        ", Target UUID: " + rs.getString("target_uuid") +
                        ", Target Name: " + rs.getString("target_name") +
                        ", Issuer UUID: " + rs.getString("issuer_uuid") +
                        ", Issuer Name: " + rs.getString("issuer_name") +
                        ", Reason: " + rs.getString("reason") +
                        ", Ban Time: " + rs.getLong("ban_time") +
                        ", Duration: " + rs.getLong("duration") +
                        ", Server Scope: " + rs.getString("server_scope") +
                        ", Silent: " + rs.getBoolean("silent"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
