package fr.neocle.flexbans.database.dashboard;

import fr.neocle.flexbans.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SessionManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public SessionManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertSessionData(String sessionId, String username) {
        String insertSQL = "INSERT INTO sessions (session_id, user_id) VALUES (?, (SELECT id FROM users WHERE username = ?))";

        try (Connection connection = dbManager.getConnection();
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

        try (Connection connection = dbManager.getConnection();
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

        try (Connection connection = dbManager.getConnection();
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
}
