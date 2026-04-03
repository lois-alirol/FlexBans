package fr.neocle.flexbans.database.actor;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ActorsManager {
    private final DatabaseConnectionManager dbManager;
    private final ExecutorService dbExecutor;

    private static final FlexLogger LOGGER = FlexLogger.get(ActorsManager.class);

    public ActorsManager(DatabaseConnectionManager dbManager, ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Integer> getOrCreatePlayerActor(UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT id FROM actors WHERE player_uuid = ? AND type = 'PLAYER'";
            String insertSql = "INSERT INTO actors (type, player_uuid, name) VALUES ('PLAYER', ?, ?)";

            try (Connection conn = dbManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int actorId = rs.getInt("id");
                            updateActorName(actorId, playerName);
                            return actorId;
                        }
                    }
                }

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
                LOGGER.error("Failed to get or create player actor: ", e);
            }

            throw new RuntimeException("Failed to create actor for player " + playerUuid);
        }, dbExecutor);
    }

    public CompletableFuture<Integer> getOrCreateConsoleActor() {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT id FROM actors WHERE type = 'CONSOLE' LIMIT 1";
            String insertSql = "INSERT INTO actors (type, name) VALUES ('CONSOLE', 'Console')";

            try (Connection conn = dbManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("id");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to get or create console actor: ", e);
            }

            throw new RuntimeException("Failed to create console actor");
        }, dbExecutor);
    }

    public CompletableFuture<Integer> getOrCreateSystemActor(String systemName) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT id FROM actors WHERE type = 'SYSTEM' AND name = ?";
            String insertSql = "INSERT INTO actors (type, name) VALUES ('SYSTEM', ?)";

            try (Connection conn = dbManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, systemName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("id");
                        }
                    }
                }

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
                LOGGER.error("Failed to get or create system actor: ", e);
            }

            throw new RuntimeException("Failed to create system actor: " + systemName);
        }, dbExecutor);
    }

    public CompletableFuture<ActorInfo> getActorInfo(int actorId) {
        return CompletableFuture.supplyAsync(() -> {
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
                LOGGER.error("Failed to get actor info: ", e);
            }

            return null;
        }, dbExecutor);
    }

    private CompletableFuture<Void> updateActorName(int actorId, String newName) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE actors SET name = ? WHERE id = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newName);
                ps.setInt(2, actorId);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Failed to update actor name: ", e);
            }
        }, dbExecutor);
    }

    public record ActorInfo(int id, String type, UUID playerUuid, String name) {}
}