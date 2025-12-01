package fr.neocle.flexbans.database.servers;

import fr.neocle.flexbans.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class ServerLocksManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public ServerLocksManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertServerLock(String serverName, String reason, long duration, UUID issuerUUID, String issuerName, String serverOrigin, boolean silent) {
        String query = "INSERT INTO flexbans_server_locks (server_name, start_time, reason, duration, issuer_uuid, issuer_name, server_origin, silent) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);
            stmt.setString(2, String.valueOf(System.currentTimeMillis()));
            stmt.setString(3, reason);
            stmt.setLong(4, duration);
            stmt.setString(5, issuerUUID.toString());
            stmt.setString(6, issuerName);
            stmt.setString(7, serverOrigin);
            stmt.setBoolean(8, silent);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to insert server lock for " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setServerLockEnd(UUID issuerUUID, String issuerName, String serverName) {
        String query = "UPDATE flexbans_server_locks SET remover_uuid = ?, remover_name = ?, removal_time = ?, status = 'unlocked'" +
                "WHERE server_name = ? AND status = 'locked'";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, issuerUUID.toString());
            stmt.setString(2, issuerName);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, serverName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to update server lock end time for " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isServerLocked(String serverName) {
        String query = "SELECT 1 FROM flexbans_server_locks WHERE server_name = ? AND status = 'locked' LIMIT 1";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.severe("Failed to check if server is locked for " + serverName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getReason(String serverName) {
        String query = "SELECT reason FROM flexbans_server_locks WHERE server_name = ? AND status = 'locked' LIMIT 1";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reason");
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to fetch reason for locked server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String getIssuer(String serverName) {
        String query = "SELECT issuer_name FROM flexbans_server_locks WHERE server_name = ? AND status = 'locked' LIMIT 1";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("issuer_name");
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to fetch issuer for locked server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public long getTime(String serverName) {
        String query = "SELECT start_time FROM flexbans_server_locks WHERE server_name = ? AND status = 'locked' LIMIT 1";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Long.parseLong(rs.getString("start_time"));
                }
            }

        } catch (SQLException e) {
            logger.severe("Failed to fetch start time for locked server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
}
