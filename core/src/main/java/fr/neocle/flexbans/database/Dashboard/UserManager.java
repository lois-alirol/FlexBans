package fr.neocle.flexbans.database.Dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class UserManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public UserManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertUsername(String username, String code) throws SQLException {
        String checkSQL = "SELECT id, username, discord_id FROM users WHERE username = ? OR discord_id IS NOT NULL;";
        String updateSQL = "UPDATE users SET username = ?, discord_id = ?, verification_code = ? WHERE id = ?;";
        String deleteSQL = "DELETE FROM users WHERE id = ?;";
        String insertSQL = "INSERT INTO users (username, verification_code) VALUES (?, ?);";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkSQL)) {

            checkStmt.setString(1, username);
            ResultSet resultSet = checkStmt.executeQuery();

            Integer usernameRowId = null;
            Integer discordRowId = null;
            String discordId = null;

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String dbUsername = resultSet.getString("username");
                String dbDiscordId = resultSet.getString("discord_id");

                if (dbUsername != null && dbUsername.equals(username)) {
                    usernameRowId = id;
                } else if (dbDiscordId != null) {
                    discordRowId = id;
                    discordId = dbDiscordId;
                }
            }

            if (usernameRowId != null && discordRowId != null) {
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                    deleteStmt.setInt(1, discordRowId);
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, username);
                    updateStmt.setString(2, discordId); // Now updates Discord ID
                    updateStmt.setString(3, code);
                    updateStmt.setInt(4, usernameRowId);
                    updateStmt.executeUpdate();
                }

            } else if (usernameRowId != null) {
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, username);
                    updateStmt.setString(2, discordId); // Keep the existing Discord ID
                    updateStmt.setString(3, code);
                    updateStmt.setInt(4, usernameRowId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, code);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    public void insertDiscordId(String discordId) throws SQLException {
        String mergeSQL = "MERGE INTO users (discord_id) KEY (discord_id) VALUES (?)";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement mergeStmt = connection.prepareStatement(mergeSQL)) {
            mergeStmt.setString(1, discordId);
            mergeStmt.executeUpdate();
        }
    }

    public void insertVerificationCodeFromDiscordId(String discordId, String verificationCode) throws SQLException {
        String updateSQL = "UPDATE users SET verification_code = ? WHERE discord_id = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setString(1, verificationCode);
            preparedStatement.setString(2, discordId);
            preparedStatement.executeUpdate();
        }
    }

    public void insertVerificationCodeFromPlayerName(String playerName, String verificationCode) throws SQLException {
        String updateSQL = "UPDATE users SET verification_code = ? WHERE username = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setString(1, verificationCode);
            preparedStatement.setString(2, playerName);
            preparedStatement.executeUpdate();
        }
    }

    public void registerUser(String username, String password) {
        try {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (Connection connection = dbManager.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Failed to register user: " + e.getMessage());
        }
    }

    public void setVerifiedStatusForPlayerName(String playerName, boolean bool) {
        String updateSQL = "UPDATE users SET is_verified = ? WHERE username = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setBoolean(1, bool);
            preparedStatement.setString(2, playerName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error setting verified status: " + e.getMessage());
        }
    }

    public void setVerifiedStatusForDiscordId(String userId, boolean bool) {
        String updateSQL = "UPDATE users SET is_verified = ? WHERE discord_id = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setBoolean(1, bool);
            preparedStatement.setString(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error setting verified status: " + e.getMessage());
        }
    }

    public String getUsername(String discordId) {
        String selectSQL = "SELECT username FROM users WHERE discord_id = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, discordId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            logger.severe("Error fetching username: " + e.getMessage());
        }
        return null;
    }

    public boolean isUserVerified(String userId) {
        String selectSQL = "SELECT is_verified FROM users WHERE discord_id = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("is_verified");
            }
        } catch (SQLException e) {
            logger.severe("Error checking user verification: " + e.getMessage());
        }
        return false;
    }


    public boolean isPlayerVerified(String playerName) {
        String selectSQL = "SELECT is_verified FROM users WHERE username = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, playerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean("is_verified");
            }
        } catch (SQLException e) {
            logger.severe("Error checking user verification: " + e.getMessage());
        }
        return false;
    }


    public boolean isUserRegistered(String username) {
        String selectSQL = "SELECT username FROM users WHERE username = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.severe("Error checking user registration: " + e.getMessage());
        }
        return false;
    }

    public String getUsernameFromDiscordId(String discordId) {
        String selectSQL = "SELECT username FROM users WHERE discord_id = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, discordId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            logger.severe("Error fetching username from Discord ID: " + e.getMessage());
        }
        return null;
    }

    public boolean validateUserCredentials(String username, String password) {
        String selectSQL = "SELECT password FROM users WHERE username = ?;";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String storedHashedPassword = resultSet.getString("password");
                return BCrypt.checkpw(password, storedHashedPassword);
            }
        } catch (SQLException e) {
            logger.severe("Error validating user credentials: " + e.getMessage());
        }
        return false;
    }
}
