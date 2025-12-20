package fr.neocle.flexbans.bukkit;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.bukkit.BukkitEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bukkit.command.BaseCommandBukkit;
import fr.neocle.flexbans.bukkit.command.TestCommand;
import fr.neocle.flexbans.bukkit.listener.ChatMute;
import fr.neocle.flexbans.bukkit.listener.DashboardEvents;
import fr.neocle.flexbans.bukkit.listener.DialogEvents;
import fr.neocle.flexbans.bukkit.listener.WhitelistEvents;
import fr.neocle.flexbans.config.ConfigManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;

public class FlexBansBukkit extends JavaPlugin {
    private Bootstrap bootstrap;

    @Override
    public void onEnable() {
        if (Bukkit.getServerConfig().isProxyEnabled()) {
            warnBackendSetup();
            return;
        }

        EventDispatcher eventDispatcher = new BukkitEventDispatcher(this);

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                getLogger(),
                eventDispatcher
        );

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), getLogger(), eventDispatcher, null, null);

        int pluginId = 23868;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        int port = ConfigManager.getInt("webserver.port");
        boolean webserverEnabled = ConfigManager.getBoolean("webserver.enabled");
        String url = ConfigManager.getString("webserver.url");

        if (webserverEnabled) {
            bootstrap.startWebServer(port);
        }

        registerCommands();
        registerListeners();

        bootstrap.logServerStartupInfo(url, port, "Spigot", getServer().getVersion(), webserverEnabled);
    }

    public void warnBackendSetup() {
        DialogEvents dialogListener = new DialogEvents(this);

        getLogger().warning("      / \\\\");
        getLogger().warning("     /   \\\\");
        getLogger().warning("    /  |  \\\\     Plugin is running behind a BungeeCord / Velocity instance! ");
        getLogger().warning("   /   |   \\\\    Make sure you've set up the plugin correctly");
        getLogger().warning("  /         \\\\   Setting up the backend implementation..");
        getLogger().warning(" /     o     \\\\");
        getLogger().warning("/_____________\\\\");

        setupBackendImplementation(dialogListener);

        getCommand("test").setExecutor(new TestCommand());
        getServer().getPluginManager().registerEvents(new TestCommand(), this);
        getServer().getPluginManager().registerEvents(dialogListener, this);
    }

    private void setupBackendImplementation(DialogEvents dialogListener) {
        ChatMute muteListener = new ChatMute(this);

        getServer().getPluginManager().registerEvents(muteListener, this);
        getServer().getPluginManager().registerEvents(dialogListener, this);

        if (!getServer().getMessenger().isIncomingChannelRegistered(this, "muting:channel")) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "muting:channel", muteListener);
        }

        if (!getServer().getMessenger().isIncomingChannelRegistered(this, "muting:response")) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "muting:response", muteListener);
        }

        if (!getServer().getMessenger().isOutgoingChannelRegistered(this, "muting:query")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "muting:query");
        }

        if (!getServer().getMessenger().isIncomingChannelRegistered(this, "flexbans:dialogs")) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "flexbans:dialogs", dialogListener);
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
                bootstrap.getDatabaseUtils()
        );

        getCommand("flexbans").setExecutor(baseCommand);
        getCommand("flexbans").setTabCompleter(baseCommand);
    }

    private void registerListeners() {
        getLogger().info("Registering listeners...");
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(new WhitelistEvents(), this);
        pluginManager.registerEvents(new DashboardEvents(), this);
    }
}
