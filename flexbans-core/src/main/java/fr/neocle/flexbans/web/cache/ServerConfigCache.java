package fr.neocle.flexbans.web.cache;

import java.util.*;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.logger.FlexLogger;

public class ServerConfigCache {
    private static ServerConfigCache instance;
    private Map<String, Object> cachedConfig;
    private boolean initialized = false;

    private ServerConfigCache() {
    }

    public static ServerConfigCache getInstance() {
        if (instance == null) {
            instance = new ServerConfigCache();
        }
        return instance;
    }

    public Map<String, Object> getConfig() {
        if (!initialized || cachedConfig == null) {
            FlexLogger.warn("Server configuration cache not initialized. Call initialize() first.");
            return null;
        }
        return new HashMap<>(cachedConfig);
    }

    public synchronized void initialize() {
        if (initialized) {
            FlexLogger.warn("Server configuration cache already initialized");
            return;
        }

        try {
            buildConfiguration();
            initialized = true;
            FlexLogger.info("Server configuration cache initialized successfully");
        } catch (Exception e) {
            FlexLogger.error("Error initializing server configuration cache:  " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void refresh() {
        try {
            buildConfiguration();
            FlexLogger.info("Server configuration cache refreshed manually");
        } catch (Exception e) {
            FlexLogger.error("Error refreshing server configuration cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void forceRefresh() {
        try {
            buildConfiguration();
            FlexLogger.info("Server configuration cache force-refreshed");
        } catch (Exception e) {
            FlexLogger.error("Error force-refreshing server configuration cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildConfiguration() {
        Map<String, Object> config = new HashMap<>();

        boolean isSecured = (ConfigManager.getBoolean("webserver.https") &&
                (
                        ConfigManager.getBoolean("discord-oauth.enabled") ||
                        ConfigManager.getBoolean("password-auth.enabled"))
                ) || true;

        boolean isRevocationEnabled = isSecured && ConfigManager.getBoolean("webserver.pages.punishments.allow-revocation");
        boolean isExecutionEnabled = isSecured && ConfigManager.getBoolean("webserver.pages.punishments.allow-execution");

        config.put("serverName", ConfigManager.getString("server-display.name"));
        config.put("serverDescription", ConfigManager.getString("server-display.description"));
        config.put("serverFavicon", ConfigManager.getString("server-display.favicon"));
        config.put("serverLogo", ConfigManager.getString("server-display.logo"));
        config.put("serverColor", ConfigManager.getString("server-display.color"));
        config.put("serverColorHover", ConfigManager.getString("server-display.darker-color"));
        config.put("isSecured", isSecured);

        config.put("punishmentRevocation", isRevocationEnabled);
        config.put("punishmentExecution", isExecutionEnabled);
        config.put("detailsPageEnabled", ConfigManager.getBoolean("webserver.pages.details.punishment.enabled"));

        config.put("punishments", buildPunishmentsConfig());
        config.put("histories", buildHistoriesConfig());
        config.put("oauth", buildOAuthConfig());

        this.cachedConfig = config;
    }

    private Map<String, Object> buildPunishmentsConfig() {
        Map<String, Object> punishments = new HashMap<>();

        punishments.put("bans", buildPunishmentTypeConfig("bans"));
        punishments.put("mutes", buildPunishmentTypeConfig("mutes"));
        punishments.put("warnings", buildPunishmentTypeConfig("warnings"));
        punishments.put("kicks", buildPunishmentTypeConfig("kicks"));

        return punishments;
    }

    private Map<String, Object> buildPunishmentTypeConfig(String type) {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", ConfigManager.getBoolean("webserver.pages.punishments." + type + ".enabled"));
        config.put("maxPerPage", ConfigManager.getInt("webserver.pages.punishments." + type + ".max-per-page"));
        return config;
    }

    private Map<String, Object> buildHistoriesConfig() {
        Map<String, Object> histories = new HashMap<>();
        histories.put("playerMaxPerPage", ConfigManager.getInt("webserver.pages.details.player.max-per-page"));
        histories.put("moderatorMaxPerPage", ConfigManager.getInt("webserver.pages.details.moderator.max-per-page"));
        return histories;
    }

    private Map<String, Boolean> buildOAuthConfig() {
        Map<String, Boolean> oauth = new HashMap<>();
        oauth.put("discord", ConfigManager.getBoolean("discord-oauth.enabled"));
        oauth.put("google", ConfigManager.getBoolean("google-oauth.enabled"));
        oauth.put("github", ConfigManager.getBoolean("github-oauth.enabled"));
        oauth.put("x", ConfigManager.getBoolean("x-oauth.enabled"));
        return oauth;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized void clear() {
        cachedConfig = null;
        initialized = false;
        FlexLogger.info("Server configuration cache cleared");
    }
}