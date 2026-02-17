package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.database.dashboard.RateLimiter;
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
    private final ActorsManager actorsManager;
    private final RateLimiter rateLimiter;
    private final SessionManager sessionManager;
    private final PunishmentsManager punishmentsManager;
    private final ServerLocksManager serverLocksManager;
    private final ProfilesManager profilesManager;
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
                jdbcUrl = "jdbc:sqlite:" + pluginFolderPath + "/database.db"
                        + "?journal_mode=WAL"
                        + "&synchronous=NORMAL"
                        + "&busy_timeout=5000";
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
        this.actorsManager = new ActorsManager(dbManager);
        this.rateLimiter = new RateLimiter(dbManager);

        this.userManager = new UserManager(dbManager, profilesManager);
        this.punishmentsManager = new PunishmentsManager(dbManager, actorsManager, profilesManager);
        this.serverLocksManager = new ServerLocksManager(dbManager, actorsManager);
        this.sessionManager = new SessionManager(dbManager);
        this.backupTask = new DatabaseBackupTask(pluginFolderPath, databaseType);
    }

    public void initialize() throws SQLException {
        try {
            dbManager.initializeConnection();

            Connection connection = dbManager.getConnection();
            SchemaInitializer.initializeDatabase(connection, databaseType);
            connection.close();

            backupTask.startBackupCreationTask();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        backupTask.shutdown();
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

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public PunishmentsManager getPunishmentsManager() {
        return punishmentsManager;
    }

    public ServerLocksManager getServerLocksManager() {
        return serverLocksManager;
    }

    public ProfilesManager getProfilesManager() { return  profilesManager; }

    public DatabaseConnectionManager getDatabaseConnectionManager() { return dbManager; }
}
