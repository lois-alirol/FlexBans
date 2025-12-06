package fr.neocle.flexbans.common.database;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.SQLException;

public class DatabaseInitializer {
    public DatabaseInitializer() {}

    public DatabaseUtils initialize() throws SQLException {
        // Validate config is loaded
        String type = ConfigManager.getString("database.type");
        if (type == null || type.isEmpty()) {
            throw new IllegalStateException("Database type not configured!  Check your config.yml");
        }

        String host = ConfigManager.getString("database. hostname");
        if (host == null || host.isEmpty()) {
            throw new IllegalStateException("Database hostname not configured! Check your config.yml");
        }

        int port = ConfigManager.getInt("database.port");
        if (port <= 0) {
            throw new IllegalStateException("Database port invalid! Check your config.yml");
        }

        String database = ConfigManager.getString("database.database");
        if (database == null || database.isEmpty()) {
            throw new IllegalStateException("Database name not configured! Check your config.yml");
        }

        String username = ConfigManager.getString("database.username");
        String password = ConfigManager.getString("database.password");

        FlexLogger.info("Initializing database (" + type + ") at " + host + ":" + port);

        try {
            DatabaseUtils databaseUtils = new DatabaseUtils("./plugins/FlexBans", type, host, port, database, username, password);
            databaseUtils.initialize();

            FlexLogger.info("Database initialized successfully!");
            return databaseUtils;
        } catch (SQLException e) {
            FlexLogger.error("Failed to initialize database: " + e.getMessage());
            throw e;
        }
    }
}