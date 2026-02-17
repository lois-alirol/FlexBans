package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.query.DatabaseQueries;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {


    public static void initializeDatabase(Connection connection, String dbType) throws SQLException {
        FlexLogger.info("Initializing database schema for " + dbType + "...");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "players"));
            FlexLogger.info("✓ Created table: players");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "player_names"));
            FlexLogger.info("✓ Created table: player_names");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "player_ips"));
            FlexLogger.info("✓ Created table: player_ips");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "users"));
            FlexLogger.info("✓ Created table: users");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "actors"));
            FlexLogger.info("✓ Created table: actors");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "sessions"));
            FlexLogger.info("✓ Created table: sessions");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "rate_limits"));
            FlexLogger.info("✓ Created table: rate_limits");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "punishments"));
            FlexLogger.info("✓ Created table: punishments");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "server_locks"));
            FlexLogger.info("✓ Created table: server_locks");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "punishment_actions"));
            FlexLogger.info("✓ Created table: punishment_actions");

            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "server_lock_actions"));
            FlexLogger.info("✓ Created table: server_lock_actions");

            FlexLogger.info("Database schema initialized successfully!");
        }
    }
}