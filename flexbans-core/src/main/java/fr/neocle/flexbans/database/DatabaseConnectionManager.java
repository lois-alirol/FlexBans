package fr.neocle.flexbans.database;

import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnectionManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConnectionManager(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public void initializeConnection() {
        try {
            //Class.forName("com.mysql.cj.jdbc.Driver");
            //Class.forName("org.sqlite.JDBC");
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load all JDBC Drivers", e);
        }

        try {
            try (Connection connection = getConnection()) {
                FlexLogger.info("Connected to database!");
            }
        } catch (SQLException e) {
            FlexLogger.error("Failed to initialize database connection: " + e.getMessage());
        }
    }
}
