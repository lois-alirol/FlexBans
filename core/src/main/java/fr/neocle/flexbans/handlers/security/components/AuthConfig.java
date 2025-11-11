package fr.neocle.flexbans.handlers.security.components;

import fr.neocle.flexbans.configs.ConfigManager;

public class AuthConfig {
    private final boolean oauthEnabled;
    private final boolean loginEnabled;

    public AuthConfig() {
        this.oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));
        this.loginEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("password-auth.enabled"));
    }

    public boolean isOauthEnabled() {
        return oauthEnabled;
    }

    public boolean isLoginEnabled() {
        return loginEnabled;
    }
}