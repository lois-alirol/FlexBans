package fr.neocle.flexbans.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnectionManager {
    private final Logger logger;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseConnectionManager(String jdbcUrl, String username, String password, Logger logger) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.logger = logger;
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
                logger.info("Connected to database!");
            }
        } catch (SQLException e) {
            logger.severe("Failed to initialize database connection: " + e.getMessage());
        }
    }
}
