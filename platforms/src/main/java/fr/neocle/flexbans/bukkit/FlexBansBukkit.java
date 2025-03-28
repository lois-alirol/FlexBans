package fr.neocle.flexbans.bukkit;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.bukkit.BukkitEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bukkit.commands.BaseCommandBukkit;
import fr.neocle.flexbans.bukkit.listener.ChatMute;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;
import java.util.Map;

public class FlexBansBukkit extends JavaPlugin implements Listener {
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

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                getLogger(),
                eventDispatcher
        );

        Bukkit.getPluginManager().registerEvents(this, this);

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), getLogger(), "spigot", eventDispatcher, this);

        int pluginId = 23868;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "Spigot", getServer().getVersion(), true);
    }

    public void warnInvalidSetup(String proxyType) {
        getLogger().warning("      / \\\\");
        getLogger().warning("     /   \\\\");
        getLogger().warning("    /  |  \\\\     Plugin is running behind a " + proxyType + " instance! ");
        getLogger().warning("   /   |   \\\\    Make sure you've set up the plugin correctly");
        getLogger().warning("  /         \\\\   Setting up the backend implementation..");
        getLogger().warning(" /     o     \\\\");
        getLogger().warning("/_____________\\\\");

        setupBackendImplementation();
    }

    private void setupBackendImplementation() {
        ChatMute muteListener = new ChatMute();

        Bukkit.getPluginManager().registerEvents(muteListener, this);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "muting:channel", muteListener);
    }

    @Override
    public void onDisable() {
        if (bootstrap == null) return;

        bootstrap.getDatabaseUtils().shutdown();
        bootstrap.getPlayerHeadImage().shutdown();
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
                bootstrap.getDiscordOAuthHandler(),
                bootstrap.getIndexHandler(),
                bootstrap.getDatabaseUtils(),
                getLogger(),
                bootstrap.getConfig()
        );

        getCommand("flexbans").setExecutor(baseCommand);
        getCommand("flexbans").setTabCompleter(baseCommand);
    }
}
