package fr.neocle.flexbans.common.database;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;

import java.nio.file.Path;
import java.sql.SQLException;

public class DatabaseInitializer {
    private static final FlexLogger LOGGER = FlexLogger.get(DatabaseInitializer.class);
    private final Path dataFolder;

    public DatabaseInitializer(Path dataFolder) { this.dataFolder = dataFolder; }

    public DatabaseUtils initialize() throws SQLException {
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

        LOGGER.info("Initializing database (" + type + ") at " + host + ":" + port);

        try {
            DatabaseUtils databaseUtils = new DatabaseUtils(dataFolder.toAbsolutePath().toString(), type, host, port, database, username, password);
            databaseUtils.initialize();

            LOGGER.info("Database initialized successfully!");
            return databaseUtils;
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize database: ", e);
            throw e;
        }
    }
}