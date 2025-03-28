package fr.neocle.flexbans.api.events.bukkit;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.bukkit.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.events.bukkit.authentication.LogoutEvent;
import fr.neocle.flexbans.api.events.bukkit.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.events.bukkit.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.api.events.bukkit.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.bukkit.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.events.bukkit.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.bukkit.whitelist.UserWhitelistedEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitEventDispatcher implements EventDispatcher {
    private final Plugin plugin;

    public BukkitEventDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        UserWhitelistedEvent event = new UserWhitelistedEvent(userId);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        UserUnwhitelistedEvent event = new UserUnwhitelistedEvent(userId);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerWhitelistedEvent(String playerName) {
        PlayerWhitelistedEvent event = new PlayerWhitelistedEvent(playerName);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerUnwhitelistedEvent(String playerName) {
        PlayerUnwhitelistedEvent event = new PlayerUnwhitelistedEvent(playerName);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        PlayerLoginEvent event = new PlayerLoginEvent(playerName, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        PlayerRegisterEvent event = new PlayerRegisterEvent(playerName, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        DiscordUserLoginEvent event = new DiscordUserLoginEvent(userId, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        LogoutEvent event = new LogoutEvent(playerName, userId, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }
}
