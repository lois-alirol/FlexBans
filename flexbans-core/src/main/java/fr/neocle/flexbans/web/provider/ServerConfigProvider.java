package fr.neocle.flexbans.web.provider;

import java.util.*;

import fr.neocle.flexbans.web.cache.ServerConfigCache;
import fr.neocle.flexbans.logger.FlexLogger;

public class ServerConfigProvider {
    private static ServerConfigProvider instance;
    private final ServerConfigCache cache;

    private static final FlexLogger LOGGER = FlexLogger.get(ServerConfigProvider.class);

    private ServerConfigProvider() {
        this.cache = ServerConfigCache.getInstance();
    }

    public static ServerConfigProvider getInstance() {
        if (instance == null) {
            instance = new ServerConfigProvider();
        }
        return instance;
    }

    public void initialize() {
        cache.initialize();
    }

    public Map<String, Object> getServerConfig() {
        Map<String, Object> config = cache.getConfig();
        if (config == null) {
            LOGGER.debug("Configuration not initialized, initializing now...");
            initialize();
            config = cache.getConfig();
        }
        return config;
    }

    public void refresh() {
        cache.refresh();
    }

    public void forceRefresh() {
        cache.forceRefresh();
    }

    public boolean isInitialized() {
        return cache.isInitialized();
    }
}