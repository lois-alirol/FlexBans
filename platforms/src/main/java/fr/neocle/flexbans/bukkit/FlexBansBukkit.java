package fr.neocle.flexbans.bukkit;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.bukkit.BukkitEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bukkit.commands.BaseCommandBukkit;
import fr.neocle.flexbans.bukkit.listener.ChatMute;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.bukkit.listener.DashboardEvents;
import fr.neocle.flexbans.bukkit.listener.WhitelistEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;

public class FlexBansBukkit extends JavaPlugin {
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

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), getLogger(), "spigot", eventDispatcher, this);

        FlexBansAPIImpl.setBanExecutor(bootstrap.getBanExecutor());
        FlexBansAPIImpl.setKickExecutor(bootstrap.getKickExecutor());
        FlexBansAPIImpl.setUnbanExecutor(bootstrap.getUnbanExecutor());

        int pluginId = 23868;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        int port = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.port"));
        boolean webserverEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.enabled"));
        String url = (String) ConfigManager.getConfigValue("webserver.url");

        if (webserverEnabled) {
            bootstrap.startWebServer(port);
        }

        registerCommands();
        registerListeners();

        bootstrap.logServerStartupInfo(url, port, "Spigot", getServer().getVersion(), webserverEnabled);
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
        ChatMute muteListener = new ChatMute(this);

        getServer().getPluginManager().registerEvents(muteListener, this);

        if (!getServer().getMessenger().isIncomingChannelRegistered(this, "muting:channel")) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "muting:channel", muteListener);
        }

        if (!getServer().getMessenger().isIncomingChannelRegistered(this, "muting:response")) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "muting:response", muteListener);
        }

        if (!getServer().getMessenger().isOutgoingChannelRegistered(this, "muting:query")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "muting:query");
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap == null) return;

        getLogger().info("Shutting down schedulers...");
        bootstrap.getDatabaseUtils().shutdown();
        bootstrap.getPlayerHeadImage().shutdown();

        getLogger().info("FlexBans disabled successfully!");
    }

    private void registerCommands() {
        BaseCommandBukkit baseCommand = new BaseCommandBukkit(
                bootstrap.getAPI(),
                bootstrap.getDataFolder(),
                bootstrap.getAuthenticatorHandler(),
                bootstrap.getDiscordOAuthHandler(),
                bootstrap.getIndexHandler(),
                bootstrap.getDatabaseUtils(),
                getLogger()
        );

        getCommand("flexbans").setExecutor(baseCommand);
        getCommand("flexbans").setTabCompleter(baseCommand);
    }

    private void registerListeners() {
        getLogger().info("Registering listeners...");
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(new WhitelistEvents(getLogger()), this);
        pluginManager.registerEvents(new DashboardEvents(getLogger()), this);
    }
}
