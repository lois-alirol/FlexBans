package fr.neocle.flexbans.database.Punishments;

import fr.neocle.flexbans.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class HistoryManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public HistoryManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertPlayerData(UUID uuid, String playerName, String ip) {
        String sql = "INSERT INTO flexbans_history (player_uuid, player_name, ip) VALUES (?, ?, ?)";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, ip);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean playerExists(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM flexbans_history WHERE player_uuid = ?";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void printEntireDatabase() {
        String query = "SELECT * FROM flexbans_history";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            logger.info("=== History Table ===");

            while (rs.next()) {
                logger.info("ID: " + rs.getInt("id") +
                        ", Player UUID: " + rs.getString("player_uuid") +
                        ", Player Name: " + rs.getString("player_name") +
                        ", IP: " + rs.getString("ip"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
