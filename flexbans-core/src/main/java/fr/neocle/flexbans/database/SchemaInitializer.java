package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.query.DatabaseQueries;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initializeDatabase(Connection connection, String dbType) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "users"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "sessions"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "bans"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "mutes"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "kicks"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "warnings"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "server_locks"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "profiles"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "ips"));
            statement.executeUpdate(DatabaseQueries.getCreateTableQuery(dbType, "names"));
        }
    }
}