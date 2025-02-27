package fr.neocle.litebansweb.api.events.bukkit;

import fr.neocle.litebansweb.api.events.EventDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitEventDispatcher implements EventDispatcher {
    private final Plugin plugin;

    public BukkitEventDispatcher(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        BukkitUserWhitelistedEvent event = new BukkitUserWhitelistedEvent(userId);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        BukkitUserUnwhitelistedEvent event = new BukkitUserUnwhitelistedEvent(userId);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerWhitelistedEvent(String playerName) {
        BukkitPlayerWhitelistedEvent event = new BukkitPlayerWhitelistedEvent(playerName);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerUnwhitelistedEvent(String playerName) {
        BukkitPlayerUnwhitelistedEvent event = new BukkitPlayerUnwhitelistedEvent(playerName);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        BukkitPlayerLoginEvent event = new BukkitPlayerLoginEvent(playerName, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        BukkitPlayerRegisterEvent event = new BukkitPlayerRegisterEvent(playerName, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        BukkitDiscordUserLoginEvent event = new BukkitDiscordUserLoginEvent(userId, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        BukkitLogoutEvent event = new BukkitLogoutEvent(playerName, userId, userAgent, ipAddress);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }
}
