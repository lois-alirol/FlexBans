package fr.neocle.flexbans.database.actor;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages actors (staff members, console, system) in the database.
 * Actors are entities that can issue punishments.
 */
public class ActorsManager {
    private final DatabaseConnectionManager dbManager;

    public ActorsManager(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Get or create an actor ID for a player.
     * @param playerUuid The player's UUID
     * @param playerName The player's current username
     * @return The actor ID
     */
    public int getOrCreatePlayerActor(UUID playerUuid, String playerName) {
        String selectSql = "SELECT id FROM actors WHERE player_uuid = ? AND type = 'PLAYER'";
        String insertSql = "INSERT INTO actors (type, player_uuid, name) VALUES ('PLAYER', ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            // Try to find existing actor
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int actorId = rs.getInt("id");
                        // Update name if changed
                        updateActorName(actorId, playerName);
                        return actorId;
                    }
                }
            }

            // Create new actor
            try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerName);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get or create player actor: " + e.getMessage());
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to create actor for player " + playerUuid);
    }

    /**
     * Get or create the console actor ID.
     * @return The console actor ID
     */
    public int getOrCreateConsoleActor() {
        String selectSql = "SELECT id FROM actors WHERE type = 'CONSOLE' LIMIT 1";
        String insertSql = "INSERT INTO actors (type, name) VALUES ('CONSOLE', 'Console')";

        try (Connection conn = dbManager.getConnection()) {
            // Try to find existing console actor
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }

            // Create new console actor
            try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get or create console actor: " + e.getMessage());
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to create console actor");
    }

    /**
     * Get or create a system actor ID.
     * @param systemName The system name (e.g., "AUTO_EXPIRE", "MIGRATION")
     * @return The system actor ID
     */
    public int getOrCreateSystemActor(String systemName) {
        String selectSql = "SELECT id FROM actors WHERE type = 'SYSTEM' AND name = ?";
        String insertSql = "INSERT INTO actors (type, name) VALUES ('SYSTEM', ?)";

        try (Connection conn = dbManager.getConnection()) {
            // Try to find existing system actor
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, systemName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }

            // Create new system actor
            try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, systemName);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get or create system actor: " + e.getMessage());
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to create system actor: " + systemName);
    }

    /**
     * Update an actor's name (typically when a player changes username).
     */
    private void updateActorName(int actorId, String newName) {
        String sql = "UPDATE actors SET name = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, actorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FlexLogger.error("Failed to update actor name: " + e.getMessage());
        }
    }

    /**
     * Get actor information by ID.
     */
    public ActorInfo getActorInfo(int actorId) {
        String sql = "SELECT type, player_uuid, name FROM actors WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("type");
                    String uuidStr = rs.getString("player_uuid");
                    String name = rs.getString("name");

                    UUID playerUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                    return new ActorInfo(actorId, type, playerUuid, name);
                }
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to get actor info: " + e.getMessage());
        }

        return null;
    }

    /**
     * Simple data class for actor information.
     */
    public static class ActorInfo {
        public final int id;
        public final String type;
        public final UUID playerUuid;
        public final String name;

        public ActorInfo(int id, String type, UUID playerUuid, String name) {
            this.id = id;
            this.type = type;
            this.playerUuid = playerUuid;
            this.name = name;
        }
    }
}