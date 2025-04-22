package fr.neocle.flexbans.database.Dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class UserManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;
    private final DatabaseUtils db;

    public UserManager(DatabaseUtils db, DatabaseConnectionManager dbManager, Logger logger) {
        this.db = db;
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertUsername(String username, String code) throws SQLException {
        String checkCodeSQL = "SELECT id, username, discord_id FROM users WHERE verification_code = ?";
        String checkUsernameSQL = "SELECT id FROM users WHERE username = ?";
        String insertSQL = "INSERT INTO users (username, verification_code) VALUES (?, ?)";
        String updateSQL = "UPDATE users SET username = ? WHERE verification_code = ?";
        String mergeSQL = "UPDATE users SET discord_id = ? WHERE username = ?";

        try (Connection connection = dbManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean codeExists = false;
                boolean usernameExists = false;
                String existingDiscordId = null;
                Integer existingCodeId = null;

                try (PreparedStatement checkCodeStmt = connection.prepareStatement(checkCodeSQL)) {
                    checkCodeStmt.setString(1, code);
                    ResultSet codeResult = checkCodeStmt.executeQuery();

                    if (codeResult.next()) {
                        codeExists = true;
                        existingCodeId = codeResult.getInt("id");
                        existingDiscordId = codeResult.getString("discord_id");
                    }
                }

                try (PreparedStatement checkUsernameStmt = connection.prepareStatement(checkUsernameSQL)) {
                    checkUsernameStmt.setString(1, username);
                    ResultSet usernameResult = checkUsernameStmt.executeQuery();

                    if (usernameResult.next()) {
                        usernameExists = true;
                    }
                }

                if (codeExists) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setString(1, username);
                        updateStmt.setString(2, code);
                        updateStmt.executeUpdate();
                    }
                } else if (usernameExists && existingDiscordId != null) {
                    try (PreparedStatement mergeStmt = connection.prepareStatement(mergeSQL)) {
                        mergeStmt.setString(1, existingDiscordId);
                        mergeStmt.setString(2, username);
                        mergeStmt.executeUpdate();
                    }
                } else if (!usernameExists && !codeExists) {
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, username);
                        insertStmt.setString(2, code);
                        insertStmt.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void insertDiscordId(String discordId) throws SQLException {
        String dbType = db.getDatabaseType().toLowerCase();
        String sql;

        if (dbType.contains("h2")) {
            sql = "MERGE INTO users (discord_id) KEY (discord_id) VALUES (?)";
        } else if (dbType.contains("sqlite")) {
            sql = "INSERT OR IGNORE INTO users (discord_id) VALUES (?)";
        } else {
            sql = "INSERT INTO users (discord_id) VALUES (?) ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id)";
        }

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            stmt.executeUpdate();
        }
    }

    public boolean setDiscordIdFromCode(String username, String code) {
        String getRowWithCodeSQL = "SELECT id, discord_id FROM users WHERE verification_code = ? AND username IS NULL";
        String getUserRowSQL = "SELECT id FROM users WHERE username = ?";
        String updateUserRowSQL = "UPDATE users SET discord_id = ? WHERE username = ?";
        String deleteCodeRowSQL = "DELETE FROM users WHERE id = ?";

        try (Connection connection = dbManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Integer codeRowId = null;
                String discordId = null;

                try (PreparedStatement getCodeRowStmt = connection.prepareStatement(getRowWithCodeSQL)) {
                    getCodeRowStmt.setString(1, code);
                    ResultSet resultSet = getCodeRowStmt.executeQuery();

                    if (resultSet.next()) {
                        codeRowId = resultSet.getInt("id");
                        discordId = resultSet.getString("discord_id");
                    }
                }

                if (codeRowId == null || discordId == null || discordId.isEmpty()) {
                    logger.info(codeRowId + " " + discordId);
                    connection.rollback();
                    return false;
                }

                boolean usernameExists = false;
                try (PreparedStatement getUserStmt = connection.prepareStatement(getUserRowSQL)) {
                    getUserStmt.setString(1, username);
                    ResultSet resultSet = getUserStmt.executeQuery();
                    usernameExists = resultSet.next();
                }

                if (!usernameExists) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteCodeRowSQL)) {
                    deleteStmt.setInt(1, codeRowId);
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement updateStmt = connection.prepareStatement(updateUserRowSQL)) {
                    updateStmt.setString(1, discordId);
                    updateStmt.setString(2, username);
                    updateStmt.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                logger.severe("Error setting Discord ID from code: " + e.getMessage());
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("Database connection error: " + e.getMessage());
            return false;
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
            String checkSQL = "SELECT id FROM users WHERE username = ?";
            String updateSQL = "UPDATE users SET password = ? WHERE username = ?";
            String insertSQL = "INSERT INTO users (username, password) VALUES (?, ?)";

            try (Connection connection = dbManager.getConnection()) {
                boolean userExists = false;
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSQL)) {
                    checkStmt.setString(1, username);
                    ResultSet resultSet = checkStmt.executeQuery();
                    userExists = resultSet.next();
                }

                if (userExists) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setString(2, username);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, username);
                        insertStmt.setString(2, hashedPassword);
                        insertStmt.executeUpdate();
                    }
                }
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
        String selectSQL = "SELECT username FROM users WHERE username = ? AND password IS NOT NULL AND password != '';";

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
