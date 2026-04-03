package fr.neocle.flexbans.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.neocle.flexbans.logger.FlexLogger;
import org.jspecify.annotations.NonNull;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String dbType;
    private HikariDataSource dataSource;

    private static final FlexLogger LOGGER = FlexLogger.get(DatabaseConnectionManager.class);

    public DatabaseConnectionManager(String jdbcUrl, String username, String password, String dbType) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.dbType = dbType;
    }

    public void initializeConnection() {
        HikariConfig config = getHikariConfig();
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            try (Connection conn = dataSource.getConnection()) {
                LOGGER.info("Connected to database! ({})", dbType);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    private @NonNull HikariConfig getHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        if (dbType.equals("sqlite")) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
        } else {
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
        }

        config.setPoolName("flexbans-pool");
        return config;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new IllegalStateException("Connection pool not initialized");
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Database connection pool closed.");
        }
    }
}