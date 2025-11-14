package fr.neocle.flexbans.handlers.security.components;

import fr.neocle.flexbans.configs.ConfigManager;

public class AuthConfig {
    private final boolean oauthEnabled;
    private final boolean loginEnabled;

    public AuthConfig() {
        this.oauthEnabled = ConfigManager.getBoolean("discord-oauth.enabled");
        this.loginEnabled = ConfigManager.getBoolean("password-auth.enabled");
    }

    public boolean isOauthEnabled() {
        return oauthEnabled;
    }

    public boolean isLoginEnabled() {
        return loginEnabled;
    }
}