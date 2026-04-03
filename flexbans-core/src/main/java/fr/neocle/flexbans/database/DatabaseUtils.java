package fr.neocle.flexbans.database;

import fr.neocle.flexbans.database.actor.ActorsManager;
import fr.neocle.flexbans.database.dashboard.RateLimiter;
import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.database.punishment.*;
import fr.neocle.flexbans.database.server.ServerLocksManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseUtils {
    private final ExecutorService dbExecutor;
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
    private String jdbcUrl;

    public DatabaseUtils(String pluginFolderPath, String databaseType, String host, int port, String databaseName, String username, String password) {
        this.databaseType = databaseType.toLowerCase();

        switch (this.databaseType) {
            case "mysql":
                this.dbExecutor = Executors.newFixedThreadPool(2, r -> {
                    Thread t = new Thread(r, "flexbans-db");
                    t.setDaemon(true);
                    return t;
                });

                jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?connectTimeout=5000&socketTimeout=5000";
                username = username;
                password = password;
                break;
            case "sqlite":
            default:
                this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "flexbans-db");
                    t.setDaemon(true);
                    return t;
                });

                jdbcUrl = "jdbc:sqlite:" + pluginFolderPath + "/sqlite.db"
                        + "?journal_mode=WAL"
                        + "&synchronous=NORMAL"
                        + "&busy_timeout=5000"
                        + "&foreign_keys=ON"
                        + "&cache_size=-64000";
                username = "";
                password = "";
                break;
        }

        this.dbManager = new DatabaseConnectionManager(jdbcUrl, username, password, this.databaseType);
        this.profilesManager = new ProfilesManager(dbManager, dbExecutor);
        this.actorsManager = new ActorsManager(dbManager, dbExecutor);
        this.rateLimiter = new RateLimiter(dbManager, dbExecutor);

        this.userManager = new UserManager(dbManager, profilesManager, dbExecutor);
        this.punishmentsManager = new PunishmentsManager(dbManager, actorsManager, profilesManager, dbExecutor);
        this.serverLocksManager = new ServerLocksManager(dbManager, actorsManager, dbExecutor);
        this.sessionManager = new SessionManager(dbManager, dbExecutor);
        this.backupTask = new DatabaseBackupTask(pluginFolderPath, databaseType);
    }

    public void initialize() throws SQLException {
        try {
            dbManager.initializeConnection();

            Connection connection = dbManager.getConnection();
            SchemaInitializer.initializeDatabase(connection, databaseType);
            connection.close();

            backupTask.start();
            punishmentsManager.start();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS))
                dbExecutor.shutdownNow();
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
        }
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
