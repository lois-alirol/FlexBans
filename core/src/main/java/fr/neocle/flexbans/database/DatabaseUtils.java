package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.database.punishments.BansManager;
import fr.neocle.flexbans.database.punishments.HistoryManager;
import fr.neocle.flexbans.database.punishments.KicksManager;
import fr.neocle.flexbans.database.punishments.MutesManager;
import fr.neocle.flexbans.database.servers.ServerLocksManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseUtils {
    private final DatabaseConnectionManager dbManager;
    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final BansManager bansManager;
    private final MutesManager mutesManager;
    private final KicksManager kicksManager;
    private final HistoryManager historyManager;
    private final ServerLocksManager serverLocksManager;
    private final DatabaseCleanupTask cleanupTask;
    private final DatabaseBackupTask backupTask;
    private final String databaseType;
    private final String pluginFolderPath;
    private String jdbcUrl;

    public DatabaseUtils(String pluginFolderPath, String databaseType, String host, int port, String databaseName, String username, String password, Logger logger) {
        this.databaseType = databaseType.toLowerCase();
        this.pluginFolderPath = pluginFolderPath;

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
        this.userManager = new UserManager(this, dbManager, logger);
        this.bansManager = new BansManager(dbManager, logger);
        this.mutesManager = new MutesManager(dbManager, logger);
        this.kicksManager = new KicksManager(dbManager, logger);
        this.historyManager = new HistoryManager(dbManager, logger);
        this.serverLocksManager = new ServerLocksManager(dbManager, logger);
        this.sessionManager = new SessionManager(dbManager, logger);
        this.cleanupTask = new DatabaseCleanupTask(dbManager);
        this.backupTask = new DatabaseBackupTask(pluginFolderPath, databaseType, logger);
    }

    public void initialize() throws SQLException {
        try {
            dbManager.initializeConnection();

            Connection connection = dbManager.getConnection();
            DatabaseInitializer.initializeDatabase(connection, databaseType);

            cleanupTask.startSessionCleanupTask();
            backupTask.startBackupCreationTask();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        Connection connection = dbManager.getConnection();
        return connection.prepareStatement(sql);
    }

    public void shutdown() {
        cleanupTask.shutdown();
        backupTask.shutdown();
        bansManager.shutdown();
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

    public MutesManager getMutesManager() {
        return mutesManager;
    }

    public KicksManager getKicksManager() {
        return kicksManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public ServerLocksManager getServerLocksManager() {
        return serverLocksManager;
    }
}
