package fr.neocle.flexbans.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.velocity.VelocityEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.velocity.commands.BanCommand;
import fr.neocle.flexbans.velocity.commands.BaseCommandVelocity;
import fr.neocle.flexbans.velocity.commands.UnbanCommand;
import fr.neocle.flexbans.velocity.listener.DashboardEvents;
import fr.neocle.flexbans.velocity.listener.PlayerEvents;
import fr.neocle.flexbans.velocity.listener.WhitelistEvents;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.logging.Logger;

@Plugin(id = "flexbans", name = "FlexBans", version = "1.0-SNAPSHOT", authors = {"Neocle"})
public class FlexBansVelocity {
    private final ProxyServer proxyServer;
    private final Metrics.Factory metricsFactory;
    private final Logger logger;
    private Bootstrap bootstrap;

    @Inject
    public FlexBansVelocity(ProxyServer proxyServer, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.metricsFactory = metricsFactory;
        this.logger = Logger.getLogger("FlexBans");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        EventDispatcher eventDispatcher = new VelocityEventDispatcher(proxyServer);

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                logger,
                eventDispatcher
        );

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), logger, "velocity", eventDispatcher, proxyServer);

        int port = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.port"));
        boolean webserverEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.enabled"));
        String url = (String) ConfigManager.getConfigValue("webserver.url");

        if (webserverEnabled) {
            bootstrap.startWebServer(port);
        }

        registerCommands();
        registerListeners();

        bootstrap.logServerStartupInfo(url, port, "Velocity", proxyServer.getVersion().getVersion(), webserverEnabled);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down schedulers...");
        bootstrap.getDatabaseUtils().shutdown();
        bootstrap.getPlayerHeadImage().shutdown();

        logger.info("FlexBans disabled successfully!");
    }

    private void registerCommands() {
        logger.info("Registering commands...");
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("flexbans").aliases("fb").build(), new BaseCommandVelocity(
                bootstrap.getAPI(),
                proxyServer,
                bootstrap.getDataFolder(),
                bootstrap.getAuthenticatorHandler(),
                bootstrap.getDiscordOAuthHandler(),
                bootstrap.getIndexHandler(),
                bootstrap.getJettyReloader(),
                bootstrap.getDatabaseUtils(),
                logger
        ));

        commandManager.register("ban", new BanCommand(bootstrap.getBanExecutor(), proxyServer));
        commandManager.register("unban", new UnbanCommand(bootstrap.getUnbanExecutor()));
    }

    private void registerListeners() {
        logger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();

        eventManager.register(this, new WhitelistEvents(logger));
        eventManager.register(this, new DashboardEvents(logger));
        eventManager.register(this, new PlayerEvents(bootstrap, proxyServer));
    }
}
