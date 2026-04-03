package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.query.DatabaseQueries;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    private static final FlexLogger LOGGER = FlexLogger.get(SchemaInitializer.class);

    public static void initializeDatabase(Connection connection, String dbType) throws SQLException {
        String[] tables = {
                "players", "player_names", "player_ips", "users",
                "actors", "sessions", "rate_limits", "punishments",
                "server_locks", "punishment_actions", "server_lock_actions"
        };

        try (Statement statement = connection.createStatement()) {
            for (String table : tables) {
                statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, table));
                LOGGER.info("Table ready: " + table);
            }
            LOGGER.info("Database schema initialized successfully!");
        }
    }
}