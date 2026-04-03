package fr.neocle.flexbans.bukkit;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.bukkit.BukkitEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bukkit.command.BaseCommandBukkit;
import fr.neocle.flexbans.bukkit.command.backend.BanCommand;
import fr.neocle.flexbans.bukkit.listener.ChatMute;
import fr.neocle.flexbans.bukkit.listener.DashboardEvents;
import fr.neocle.flexbans.bukkit.listener.DialogEvents;
import fr.neocle.flexbans.bukkit.listener.WhitelistEvents;
import fr.neocle.flexbans.common.messaging.Channel;
import fr.neocle.flexbans.config.ConfigManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.nio.file.Paths;

public class FlexBansBukkit extends JavaPlugin {
    private Bootstrap bootstrap;

    @Override
    public void onEnable() {
        if (Bukkit.getServerConfig().isProxyEnabled()) {
            initBackendSetup();
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

    public void initBackendSetup() {
        getLogger().warning("Plugin is running behind a BungeeCord / Velocity instance!");
        getLogger().warning("Make sure you've set up the plugin correctly");
        getLogger().warning("Setting up the backend implementation..");

        setupBackendImplementation();
    }

    private void setupBackendImplementation() {
        saveResource("backend-config.yml", false);

        File configFile = new File(getDataFolder(), "backend-config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            BanCommand.register(event.registrar(), config);
        });

        ChatMute muteListener = new ChatMute(this);
        DialogEvents dialogListener = new DialogEvents(this);

        PluginManager pm = getServer().getPluginManager();
        Messenger messenger = getServer().getMessenger();

        pm.registerEvents(muteListener, this);
        pm.registerEvents(dialogListener, this);

        registerIncoming(messenger, Channel.MUTED, muteListener);
        registerIncoming(messenger, Channel.MUTED_RESPONSE, muteListener);

        registerOutgoing(messenger, Channel.MUTED_QUERY);

        registerIncoming(messenger, Channel.DIALOGS, dialogListener);
        registerOutgoing(messenger, Channel.BAN);
        registerOutgoing(messenger, Channel.MUTE);
        registerOutgoing(messenger, Channel.KICK);
        registerOutgoing(messenger, Channel.WARNING);
    }

    @Override
    public void onDisable() {
        if (bootstrap == null) return;

        getLogger().info("Shutting down schedulers...");

        getLogger().info("FlexBans disabled successfully!");
    }

    private void registerCommands() {
        BaseCommandBukkit baseCommand = new BaseCommandBukkit(
                bootstrap.getDataFolder(),
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

    private void registerIncoming(Messenger messenger, String channel, PluginMessageListener listener) {
        if (!messenger.isIncomingChannelRegistered(this, channel)) {
            messenger.registerIncomingPluginChannel(this, channel, listener);
        }
    }

    private void registerOutgoing(Messenger messenger, String channel) {
        if (!messenger.isOutgoingChannelRegistered(this, channel)) {
            messenger.registerOutgoingPluginChannel(this, channel);
        }
    }
}
