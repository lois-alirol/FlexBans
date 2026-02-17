package fr.neocle.flexbans.web.handler;

import fr.neocle.flexbans.web.provider.ServerConfigProvider;
import fr.neocle.flexbans.web.response.ApiResponse;

public class ConfigHandler {
    private final ServerConfigProvider configProvider;

    public ConfigHandler(ServerConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public ApiResponse<?> getConfig() {
        return ApiResponse.success(configProvider.getServerConfig());
    }
}