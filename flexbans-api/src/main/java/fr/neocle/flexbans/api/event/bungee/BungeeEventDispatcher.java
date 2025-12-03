package fr.neocle.flexbans.api.event.bungee;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.bungee.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.event.bungee.authentication.LogoutEvent;
import fr.neocle.flexbans.api.event.bungee.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.event.bungee.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.api.event.bungee.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.event.bungee.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.event.bungee.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.event.bungee.whitelist.UserWhitelistedEvent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.UUID;

public class BungeeEventDispatcher implements EventDispatcher {
    private final Plugin plugin;

    public BungeeEventDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        UserWhitelistedEvent event = new UserWhitelistedEvent(userId);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        UserUnwhitelistedEvent event = new UserUnwhitelistedEvent(userId);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerWhitelistedEvent(String username) {
        PlayerWhitelistedEvent event = new PlayerWhitelistedEvent(username);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerUnwhitelistedEvent(String username) {
        PlayerUnwhitelistedEvent event = new PlayerUnwhitelistedEvent(username);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        PlayerLoginEvent event = new PlayerLoginEvent(playerName, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        PlayerRegisterEvent event = new PlayerRegisterEvent(playerName, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        DiscordUserLoginEvent event = new DiscordUserLoginEvent(userId, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        LogoutEvent event = new LogoutEvent(playerName, userId, userAgent, ipAddress);
        plugin.getProxy().getPluginManager().callEvent(event);
    }

    @Override
    public void banAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName, String reason, long duration, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {

    }

    @Override
    public void muteAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName, String reason, long duration, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {

    }

    @Override
    public void kickAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName, String reason, String serverOrigin, boolean silent, boolean ipScope) {

    }
}
