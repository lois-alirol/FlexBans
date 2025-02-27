package fr.neocle.litebansweb.bukkit;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neocle.litebansweb.bukkit.commands.BaseCommandBukkit;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.Bootstrap;
import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.api.events.bukkit.BukkitEventDispatcher;
import fr.neocle.litebansweb.api.events.bukkit.BukkitUserWhitelistedEvent;
import fr.neocle.litebansweb.api.impl.LitebansWebAPIImpl;

import java.nio.file.Paths;
import java.util.Map;

public class LitebansWebBukkit extends JavaPlugin implements Listener {
    private Bootstrap bootstrap;

    @Override
    public void onEnable() {

        if (Bukkit.getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
            warnInvalidSetup("BungeeCord");
            return;
        }

        if (Bukkit.getServer().spigot().getPaperConfig().getBoolean("proxies.velocity.enabled")) {
            warnInvalidSetup("Velocity");
            return;
        }

        EventDispatcher eventDispatcher = new BukkitEventDispatcher(this);

        LitebansWebAPIImpl.initialize(
            Paths.get("plugins", "LitebansWeb", "config.yml"), 
            getLogger(), 
            eventDispatcher
        );

        Bukkit.getPluginManager().registerEvents(this, this);

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), getLogger(), "spigot", null, eventDispatcher);

        int pluginId = 23868;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "Spigot", getServer().getVersion());
    }

    public void warnInvalidSetup(String proxyType) {
        getLogger().severe("      / \\\\");
        getLogger().severe("     /   \\\\");
        getLogger().severe("    /  |  \\\\     Plugin is running behind a " + proxyType + " instance! ");
        getLogger().severe("   /   |   \\\\    Please use LitebansWeb on the proxy.");
        getLogger().severe("  /         \\\\   Now disabling...");
        getLogger().severe(" /     o     \\\\");
        getLogger().severe("/_____________\\\\");
        getServer().getPluginManager().disablePlugin(this);
    }

    private String getAddressFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return String.valueOf(webserverConfig.getOrDefault("url", "undefined, check config.yml"));
    }

    private int getPortFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Integer.parseInt(String.valueOf(webserverConfig.getOrDefault("port", "8080")));
    }

    private void registerCommands() {
        BaseCommandBukkit baseCommand = new BaseCommandBukkit(
            bootstrap.getAPI(),
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", getLogger()),
            getLogger()
        );
    
        getCommand("litebansweb").setExecutor(baseCommand);
        getCommand("litebansweb").setTabCompleter(baseCommand);
    }

    @EventHandler
    public void onPlayerWhitelisted(BukkitUserWhitelistedEvent event) {
        getLogger().info("User " + event.getUserId() + " was whitelisted!");
    }

}
