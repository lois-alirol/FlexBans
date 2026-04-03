package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UserManager {
    private final DatabaseConnectionManager dbManager;
    private final ProfilesManager profilesManager;
    private final ExecutorService dbExecutor;

    private static final FlexLogger LOGGER = FlexLogger.get(UserManager.class);

    public UserManager(DatabaseConnectionManager dbManager,
                       ProfilesManager profilesManager,
                       ExecutorService dbExecutor) {
        this.dbManager = dbManager;
        this.profilesManager = profilesManager;
        this.dbExecutor = dbExecutor;
    }

    public CompletableFuture<Boolean> registerUser(String username, String password, UUID minecraftUuid) {
        return profilesManager.playerExists(minecraftUuid).thenComposeAsync(exists -> {
            if (!exists) {
                LOGGER.error("Cannot register user: Minecraft profile does not exist for UUID {}", minecraftUuid);
                return CompletableFuture.completedFuture(false);
            }

            return CompletableFuture.supplyAsync(() -> {
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                String checkSql = "SELECT id, player_uuid FROM users WHERE username = ?";
                String insertSql = "INSERT INTO users (username, password_hash, player_uuid) VALUES (?, ?, ?)";
                String updateSql = "UPDATE users SET password_hash = ?, player_uuid = ? WHERE username = ?";

                try (Connection conn = dbManager.getConnection()) {
                    boolean userExists = false;
                    UUID existingUuid = null;

                    try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, username);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                userExists = true;
                                String uuidStr = rs.getString("player_uuid");
                                existingUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                            }
                        }
                    }

                    if (userExists) {
                        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setString(1, hashedPassword);
                            ps.setString(2, minecraftUuid.toString());
                            ps.setString(3, username);
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setString(1, username);
                            ps.setString(2, hashedPassword);
                            ps.setString(3, minecraftUuid.toString());
                            ps.executeUpdate();
                        }
                    }

                    return true;

                } catch (SQLException e) {
                    LOGGER.error("Failed to register user: ", e);
                    return false;
                }
            }, dbExecutor);
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> verifyUser(String username, UUID minecraftUuid) {
        return profilesManager.playerExists(minecraftUuid).thenComposeAsync(exists -> {
            if (!exists) {
                LOGGER.error("Cannot verify user: Minecraft profile does not exist");
                return CompletableFuture.completedFuture(false);
            }

            return CompletableFuture.supplyAsync(() -> {
                String sql = """
                    UPDATE users
                    SET is_verified = TRUE,
                        verified_at = ?,
                        player_uuid = ?
                    WHERE username = ?
                      AND (player_uuid IS NULL OR player_uuid = ?)
                """;

                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setLong(1, System.currentTimeMillis());
                    ps.setString(2, minecraftUuid.toString());
                    ps.setString(3, username);
                    ps.setString(4, minecraftUuid.toString());

                    int updated = ps.executeUpdate();

                    if (updated > 0) {
                        LOGGER.info("User {} verified and linked to MC UUID {}", username, minecraftUuid);
                        return true;
                    } else {
                        LOGGER.warn("Failed to verify user {}: user not found or already linked to different profile", username);
                        return false;
                    }

                } catch (SQLException e) {
                    LOGGER.error("Failed to verify user: ", e);
                    return false;
                }
            }, dbExecutor);
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> setDiscordId(String username, String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE users SET discord_id = ? WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, discordId);
                ps.setString(2, username);

                return ps.executeUpdate() > 0;

            } catch (SQLException e) {
                LOGGER.error("Failed to set Discord ID: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> setVerificationCode(String username, String code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE users SET verification_code = ? WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, code);
                ps.setString(2, username);

                return ps.executeUpdate() > 0;

            } catch (SQLException e) {
                LOGGER.error("Failed to set verification code: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> setVerificationCodeByDiscord(String discordId, String code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE users SET verification_code = ? WHERE discord_id = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, code);
                ps.setString(2, discordId);

                return ps.executeUpdate() > 0;

            } catch (SQLException e) {
                LOGGER.error("Failed to set verification code by Discord: ", e);
                return false;
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> validateCredentials(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        return BCrypt.checkpw(password, storedHash);
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to validate credentials: ", e);
            }

            return false;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> isUserVerified(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT is_verified FROM users WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("is_verified");
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to check verification status: ", e);
            }

            return false;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> isUserRegistered(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM users WHERE username = ? AND password_hash IS NOT NULL";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to check registration status: ", e);
            }

            return false;
        }, dbExecutor);
    }

    public CompletableFuture<String> getUsernameByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT username FROM users WHERE discord_id = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, discordId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get username by Discord ID: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<UUID> getMinecraftUuid(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_uuid FROM users WHERE username = ? AND is_verified = TRUE";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uuidStr = rs.getString("player_uuid");
                        return uuidStr != null ? UUID.fromString(uuidStr) : null;
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get Minecraft UUID: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<String> getDiscordId(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT discord_id FROM users WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get Discord ID: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<String> getVerificationCode(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT verification_code FROM users WHERE username = ?";

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("verification_code");
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get verification code: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> createUserForVerification(String username, UUID minecraftUuid) {
        return profilesManager.playerExists(minecraftUuid).thenComposeAsync(exists -> {
            if (!exists) {
                LOGGER.error("Cannot create user: Minecraft profile does not exist");
                return CompletableFuture.completedFuture(false);
            }

            return CompletableFuture.supplyAsync(() -> {
                String sql = """
                    INSERT INTO users (username, password_hash, player_uuid, is_verified)
                    VALUES (?, '', ?, FALSE)
                """;

                try (Connection conn = dbManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, username);
                    ps.setString(2, minecraftUuid.toString());

                    ps.executeUpdate();
                    return true;

                } catch (SQLException e) {
                    LOGGER.error("Failed to create user for verification: ", e);
                    return false;
                }
            }, dbExecutor);
        }, dbExecutor);
    }

    public CompletableFuture<UserInfo> getUserInfo(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, username, player_uuid, is_verified, verified_at, discord_id
                FROM users
                WHERE username = ?
            """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        String uuidStr = rs.getString("player_uuid");
                        UUID mcUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                        boolean verified = rs.getBoolean("is_verified");
                        long verifiedAt = rs.getLong("verified_at");
                        String discordId = rs.getString("discord_id");

                        return new UserInfo(id, username, mcUuid, verified, verifiedAt, discordId);
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to get user info: ", e);
            }

            return null;
        }, dbExecutor);
    }

    public CompletableFuture<List<UserInfo>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, username, player_uuid, is_verified, verified_at, discord_id
                FROM users
            """;

            List<UserInfo> users = new ArrayList<>();

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");

                    String uuidStr = rs.getString("player_uuid");
                    UUID mcUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;

                    boolean verified = rs.getBoolean("is_verified");
                    long verifiedAt = rs.getLong("verified_at");
                    String discordId = rs.getString("discord_id");

                    users.add(new UserInfo(id, username, mcUuid, verified, verifiedAt, discordId));
                }

            } catch (SQLException e) {
                LOGGER.error("Failed to fetch all users: ", e);
            }

            return users;
        }, dbExecutor);
    }

    public static class UserInfo {
        public final int id;
        public final String username;
        public final UUID minecraftUuid;
        public final boolean isVerified;
        public final long verifiedAt;
        public final String discordId;

        public UserInfo(int id, String username, UUID minecraftUuid, boolean isVerified, long verifiedAt, String discordId) {
            this.id = id;
            this.username = username;
            this.minecraftUuid = minecraftUuid;
            this.isVerified = isVerified;
            this.verifiedAt = verifiedAt;
            this.discordId = discordId;
        }
    }
}