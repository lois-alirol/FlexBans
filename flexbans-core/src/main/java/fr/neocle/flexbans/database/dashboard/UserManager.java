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

/**
 * Manages web users and their verification/authentication.
 * When is_verified=true, the user is permanently linked to a Minecraft profile.
 */
public class UserManager {
    private final DatabaseConnectionManager dbManager;
    private final ProfilesManager profilesManager;

    public UserManager(DatabaseConnectionManager dbManager, ProfilesManager profilesManager) {
        this.dbManager = dbManager;
        this.profilesManager = profilesManager;
    }

    /**
     * Register a new user with username and password.
     * The user must be linked to a Minecraft profile for this to work.
     */
    public boolean registerUser(String username, String password, UUID minecraftUuid) {
        // Ensure the Minecraft profile exists
        if (!profilesManager.playerExists(minecraftUuid)) {
            FlexLogger.error("Cannot register user: Minecraft profile does not exist for UUID " + minecraftUuid);
            return false;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String checkSql = "SELECT id, player_uuid FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password_hash, player_uuid) VALUES (?, ?, ?)";
        String updateSql = "UPDATE users SET password_hash = ?, player_uuid = ? WHERE username = ?";

        try (Connection conn = dbManager.getConnection()) {
            boolean userExists = false;
            UUID existingUuid = null;

            // Check if user exists
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
                // Update existing user
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, hashedPassword);
                    ps.setString(2, minecraftUuid.toString());
                    ps.setString(3, username);
                    ps.executeUpdate();
                }
            } else {
                // Insert new user
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    ps.setString(3, minecraftUuid.toString());
                    ps.executeUpdate();
                }
            }

            return true;

        } catch (SQLException e) {
            FlexLogger.error("Failed to register user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify a user and permanently link them to their Minecraft profile.
     * Once verified, the link is permanent and immutable.
     */
    public boolean verifyUser(String username, UUID minecraftUuid) {
        if (!profilesManager.playerExists(minecraftUuid)) {
            FlexLogger.error("Cannot verify user: Minecraft profile does not exist");
            return false;
        }

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
                FlexLogger.info("User " + username + " verified and linked to MC UUID " + minecraftUuid);
                return true;
            } else {
                FlexLogger.warn("Failed to verify user " + username + " - user not found or already linked to different profile");
                return false;
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to verify user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set or update Discord ID for a user.
     */
    public boolean setDiscordId(String username, String discordId) {
        String sql = "UPDATE users SET discord_id = ? WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, discordId);
            ps.setString(2, username);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            FlexLogger.error("Failed to set Discord ID: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set verification code for username verification flow.
     */
    public boolean setVerificationCode(String username, String code) {
        String sql = "UPDATE users SET verification_code = ? WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, username);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            FlexLogger.error("Failed to set verification code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set verification code for Discord verification flow.
     */
    public boolean setVerificationCodeByDiscord(String discordId, String code) {
        String sql = "UPDATE users SET verification_code = ? WHERE discord_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, discordId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            FlexLogger.error("Failed to set verification code by Discord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate user credentials for login.
     */
    public boolean validateCredentials(String username, String password) {
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
            FlexLogger.error("Failed to validate credentials: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if a user is verified.
     */
    public boolean isUserVerified(String username) {
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
            FlexLogger.error("Failed to check verification status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if user is registered (has password).
     */
    public boolean isUserRegistered(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password_hash IS NOT NULL";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            FlexLogger.error("Failed to check registration status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get username from Discord ID.
     */
    public String getUsernameByDiscordId(String discordId) {
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
            FlexLogger.error("Failed to get username by Discord ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get Minecraft UUID linked to a user account.
     */
    public UUID getMinecraftUuid(String username) {
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
            FlexLogger.error("Failed to get Minecraft UUID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get Discord ID for a username.
     */
    public String getDiscordId(String username) {
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
            FlexLogger.error("Failed to get Discord ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get verification code for a user.
     */
    public String getVerificationCode(String username) {
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
            FlexLogger.error("Failed to get verification code: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create a user entry for Minecraft profile verification.
     * This is used when a player wants to claim their profile on the web interface.
     */
    public boolean createUserForVerification(String username, UUID minecraftUuid) {
        if (!profilesManager.playerExists(minecraftUuid)) {
            FlexLogger.error("Cannot create user: Minecraft profile does not exist");
            return false;
        }

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
            FlexLogger.error("Failed to create user for verification: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get user info including verification status and linked Minecraft profile.
     */
    public UserInfo getUserInfo(String username) {
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
            FlexLogger.error("Failed to get user info: " + e.getMessage());
        }

        return null;
    }

    public List<UserInfo> getAllUsers() {
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
            FlexLogger.error("Failed to fetch all users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Data class for user information.
     */
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