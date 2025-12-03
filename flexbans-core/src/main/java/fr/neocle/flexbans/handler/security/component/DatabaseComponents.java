package fr.neocle.flexbans.handler.security.component;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;

public class DatabaseComponents {
    private final DatabaseUtils databaseUtils;
    private final SessionManager sessionManager;
    private final UserManager userManager;

    public DatabaseComponents(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
        this.sessionManager = databaseUtils.getSessionManager();
        this.userManager = databaseUtils.getUserManager();
    }

    public DatabaseUtils getDatabaseUtils() {
        return databaseUtils;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }
}