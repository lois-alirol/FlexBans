package fr.neocle.litebansweb.api.events.bungee;

import fr.neocle.litebansweb.api.events.EventDispatcher;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeEventDispatcher implements EventDispatcher {
    private final Plugin plugin;

    public BungeeEventDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        BungeeUserWhitelistedEvent event = new BungeeUserWhitelistedEvent(userId);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        BungeeUserUnwhitelistedEvent event = new BungeeUserUnwhitelistedEvent(userId);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerWhitelistedEvent(String username) {
        BungeePlayerWhitelistedEvent event = new BungeePlayerWhitelistedEvent(username);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerUnwhitelistedEvent(String username) {
        BungeePlayerUnwhitelistedEvent event = new BungeePlayerUnwhitelistedEvent(username);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        BungeePlayerLoginEvent event = new BungeePlayerLoginEvent(playerName, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        BungeePlayerRegisterEvent event = new BungeePlayerRegisterEvent(playerName, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        BungeeDiscordUserLoginEvent event = new BungeeDiscordUserLoginEvent(userId, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        BungeeLogoutEvent event = new BungeeLogoutEvent(playerName, userId, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }
}
