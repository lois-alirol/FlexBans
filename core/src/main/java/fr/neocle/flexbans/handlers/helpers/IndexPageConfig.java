package fr.neocle.flexbans.handlers.helpers;

import fr.neocle.flexbans.configs.ConfigManager;

/**
 * Configuration helper for IndexHandler to centralize config access and reduce duplication.
 */
public class IndexPageConfig {
    
    // Server display configuration
    public final String serverFavicon;
    public final String serverLogo;
    public final String serverColor;
    public final String serverColorDarker;
    public final String serverName;
    public final String serverDescription;
    
    // Authentication configuration
    public final boolean oauthEnabled;
    public final boolean loginEnabled;
    
    // Punishment type configuration
    public final boolean bansEnabled;
    public final boolean mutesEnabled;
    public final boolean kicksEnabled;
    public final boolean warningsEnabled;
    
    // Page size configuration
    public final int bansPageSize;
    public final int mutesPageSize;
    public final int warningsPageSize;
    public final int kicksPageSize;
    
    // Detail page configuration
    public final boolean searchPlayerEnabled;
    public final boolean searchModeratorEnabled;
    public final boolean searchPunishmentEnabled;
    public final boolean punishmentExecutionEnabled;
    
    public IndexPageConfig() {
        // Server display configuration
        this.serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
        this.serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
        this.serverColor = (String) ConfigManager.getConfigValue("server-display.color");
        this.serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
        this.serverName = (String) ConfigManager.getConfigValue("server-display.name");
        this.serverDescription = (String) ConfigManager.getConfigValue("server-display.description");
        
        // Authentication configuration
        this.oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));
        this.loginEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("password-auth.enabled"));
        
        // Punishment type configuration
        this.bansEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.bans.enabled"));
        this.mutesEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.mutes.enabled"));
        this.kicksEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.kicks.enabled"));
        this.warningsEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.warnings.enabled"));
        
        // Page size configuration
        this.bansPageSize = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.bans.max-per-page"));
        this.mutesPageSize = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.mutes.max-per-page"));
        this.warningsPageSize = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.warnings.max-per-page"));
        this.kicksPageSize = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.kicks.max-per-page"));
        
        // Detail page configuration
        this.searchPlayerEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.player.enabled"));
        this.searchModeratorEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.moderator.enabled"));
        this.searchPunishmentEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.punishment.enabled"));
        this.punishmentExecutionEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.punishment-execution-button"));
    }
    
    /**
     * Get page size for a specific punishment type.
     */
    public int getPageSizeForType(String type) {
        switch (type.toLowerCase()) {
            case "mutes":
                return mutesPageSize;
            case "warnings":
                return warningsPageSize;
            case "kicks":
                return kicksPageSize;
            case "bans":
            default:
                return bansPageSize;
        }
    }
}