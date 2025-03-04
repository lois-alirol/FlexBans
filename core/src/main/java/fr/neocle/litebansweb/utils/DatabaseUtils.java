package fr.neocle.litebansweb.utils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseUtils {
    private final Logger logger;
    private final String jdbcUrl;

    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startSessionCleanupTask(Connection connection) {
        scheduler.scheduleAtFixedRate(() -> {
            deleteOldSessions();
        }, 0, 30, TimeUnit.MINUTES);
    }

    public DatabaseUtils(String pluginFolderPath, Logger logger) {
        this.logger = logger;
        this.jdbcUrl = "jdbc:h2:" + pluginFolderPath.toString() + "/database";
    }

    public void initializeConnection() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.severe("H2 Driver not found: " + e.getMessage());
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD)) {
            logger.info("Connected to H2 database!");
            initializeDatabase(connection);
            startSessionCleanupTask(connection);
        } catch (SQLException e) {
            logger.severe("Failed to initialize database connection: " + e.getMessage());
        }
    }

    private static void initializeDatabase(Connection connection) throws SQLException {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) UNIQUE,
                    discord_id VARCHAR(255) UNIQUE,
                    verification_code VARCHAR(255),
                    is_verified BOOLEAN DEFAULT FALSE,
                    password VARCHAR(255)
                );

                CREATE TABLE IF NOT EXISTS sessions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(255) UNIQUE NOT NULL,
                    session_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    user_id INT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD);
    }

    public void insertUsername(String username, String code) throws SQLException {
        String checkSQL = "SELECT id, username, discord_id FROM users WHERE username = ? OR discord_id IS NOT NULL;";
        String updateSQL = "UPDATE users SET username = ?, discord_id = ?, verification_code = ? WHERE id = ?;";
        String deleteSQL = "DELETE FROM users WHERE id = ?;";
        String insertSQL = "INSERT INTO users (username, verification_code) VALUES (?, ?);";
    
        try (Connection connection = getConnection();
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
    
        try (Connection connection = getConnection();
             PreparedStatement mergeStmt = connection.prepareStatement(mergeSQL)) {
            mergeStmt.setString(1, discordId);
            mergeStmt.executeUpdate();
        }
    }
    
    public void insertVerificationCodeFromDiscordId(String discordId, String verificationCode) throws SQLException {
        String updateSQL = "UPDATE users SET verification_code = ? WHERE discord_id = ?;";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateSQL)) {
            preparedStatement.setString(1, verificationCode);
            preparedStatement.setString(2, discordId);
            preparedStatement.executeUpdate();
        }
    }

    public void insertVerificationCodeFromPlayerName(String playerName, String verificationCode) throws SQLException {
        String updateSQL = "UPDATE users SET verification_code = ? WHERE username = ?;";

        try (Connection connection = getConnection();
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
            try (Connection connection = getConnection(); 
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
        
        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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

        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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
        
        try (Connection connection = getConnection();
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

    public void insertSessionData(String sessionId, String username) {
        String insertSQL = "INSERT INTO sessions (session_id, user_id) VALUES (?, (SELECT id FROM users WHERE username = ?))";
    
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setString(1, sessionId);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error inserting session data: " + e.getMessage());
        }
    }    

    public String getPlayerFromSessionId(String sessionId) {
        String selectSQL = "SELECT u.username FROM users u " +
                           "JOIN sessions s ON u.id = s.user_id " +
                           "WHERE s.session_id = ?;";
        
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
            preparedStatement.setString(1, sessionId);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
        } catch (SQLException e) {
            logger.severe("Error fetching user from session ID: " + e.getMessage());
        }
        return null;
    }    

    public void deleteSessionByPlayerName(String username) {
        String sql = "DELETE FROM sessions WHERE user_id = (SELECT id FROM users WHERE username = ?)";
    
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Deleted session for user: " + username);
            } else {
                logger.warning("No session found for user: " + username);
            }

        } catch (SQLException e) {
            logger.severe("Error deleting session for user " + username + ": " + e.getMessage());
        }
    }
    
    public void deleteAllSessions() {
        String sql = "DELETE FROM sessions";
    
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error deleting all sessions: " + e.getMessage());
        }
    }
    
    public void deleteOldSessions() {
        String sql = "DELETE FROM sessions WHERE session_date < ?";
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(14);
        
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            Timestamp thresholdTimestamp = Timestamp.valueOf(thresholdDate);
            
            preparedStatement.setTimestamp(1, thresholdTimestamp);
            preparedStatement.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    // for debug only, never used in production
    public void printEntireDatabase() {
        String selectSQL = "SELECT * FROM users;";
        
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
    
            logger.info("=== Printing Entire Database ===");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String discordId = resultSet.getString("discord_id");
                String verificationCode = resultSet.getString("verification_code");
                String password = resultSet.getString("password");
                String isVerified = resultSet.getBoolean("is_verified") ? "true" : "false";
                
                logger.info(String.format("ID: %d, Username: %s, Discord ID: %s, Verification Code: %s, Password: %s, Verified: %s",
                    id, username, discordId, verificationCode, password, isVerified));
            }
            logger.info("=== End of Database ===");
        } catch (SQLException e) {
            logger.severe("Error printing database: " + e.getMessage());
        }
    }

}
