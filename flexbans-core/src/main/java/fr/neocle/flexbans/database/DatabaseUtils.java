package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.database.punishment.*;
import fr.neocle.flexbans.database.server.ServerLocksManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseUtils {
    private final DatabaseConnectionManager dbManager;
    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final BansManager bansManager;
    private final MutesManager mutesManager;
    private final KicksManager kicksManager;
    private final WarningsManager warningsManager;
    private final ServerLocksManager serverLocksManager;
    private final ProfilesManager profilesManager;
    private final DatabaseCleanupTask cleanupTask;
    private final DatabaseBackupTask backupTask;
    private final String databaseType;
    private final String pluginFolderPath;
    private String jdbcUrl;

    public DatabaseUtils(String pluginFolderPath, String databaseType, String host, int port, String databaseName, String username, String password) {
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

        this.dbManager = new DatabaseConnectionManager(jdbcUrl, username, password);
        this.profilesManager = new ProfilesManager(dbManager);

        this.userManager = new UserManager(this, dbManager);
        this.bansManager = new BansManager(dbManager, profilesManager);
        this.mutesManager = new MutesManager(dbManager, profilesManager);
        this.kicksManager = new KicksManager(dbManager);
        this.warningsManager = new WarningsManager(dbManager);
        this.serverLocksManager = new ServerLocksManager(dbManager);
        this.sessionManager = new SessionManager(dbManager);
        this.cleanupTask = new DatabaseCleanupTask(dbManager);
        this.backupTask = new DatabaseBackupTask(pluginFolderPath, databaseType);
    }

    public void initialize() throws SQLException {
        try {
            dbManager.initializeConnection();

            Connection connection = dbManager.getConnection();
            SchemaInitializer.initializeDatabase(connection, databaseType);

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

    public WarningsManager getWarningsManager() { return warningsManager; }

    public ServerLocksManager getServerLocksManager() {
        return serverLocksManager;
    }

    public ProfilesManager getProfilesManager() { return  profilesManager; }

    public DatabaseConnectionManager getDatabaseConnectionManager() { return dbManager; }
}
