package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.Dashboard.SessionManager;
import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.Punishments.BansManager;
import fr.neocle.flexbans.database.Punishments.HistoryManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseUtils {
    private final DatabaseConnectionManager dbManager;
    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final BansManager bansManager;
    private final HistoryManager historyManager;
    private final DatabaseCleanupTask cleanupTask;
    private final String databaseType;
    private String jdbcUrl;

    public DatabaseUtils(String pluginFolderPath, String databaseType, String host, int port, String databaseName, String username, String password, Logger logger) {
        this.databaseType = databaseType.toLowerCase();
        switch (this.databaseType) {
            case "mysql":
                jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?connectTimeout=5000&socketTimeout=5000";
                username = username;
                password = password;
                break;
            case "sqlite":
                jdbcUrl = "jdbc:sqlite:" + pluginFolderPath + "/database.db";
                username = "";
                password = "";
                break;
            case "h2":
            default:
                jdbcUrl = "jdbc:h2:" + pluginFolderPath + "/database";
                username = "sa";
                password = "";
                break;
        }

        this.dbManager = new DatabaseConnectionManager(jdbcUrl, username, password, logger);
        this.userManager = new UserManager(dbManager, logger);
        this.bansManager = new BansManager(dbManager, logger);
        this.historyManager = new HistoryManager(dbManager, logger);
        this.sessionManager = new SessionManager(dbManager, logger);
        this.cleanupTask = new DatabaseCleanupTask(dbManager);
    }

    public void initialize() throws SQLException {
        try {
            dbManager.initializeConnection();

            Connection connection = dbManager.getConnection();
            DatabaseInitializer.initializeDatabase(connection, databaseType);

            cleanupTask.startSessionCleanupTask();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        cleanupTask.shutdown();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public BansManager getBansManager() {
        return bansManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }
}
