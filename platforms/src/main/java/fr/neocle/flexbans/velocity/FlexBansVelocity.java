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
import fr.neocle.flexbans.velocity.commands.BanCommand;
import fr.neocle.flexbans.velocity.commands.BaseCommandVelocity;
import fr.neocle.flexbans.velocity.listener.DashboardEvents;
import fr.neocle.flexbans.velocity.listener.PlayerEvents;
import fr.neocle.flexbans.velocity.listener.WhitelistEvents;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.Map;
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

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                logger,
                eventDispatcher
        );

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), logger, "velocity", eventDispatcher, proxyServer);
        bootstrap.startWebServer(getPortFromConfig());

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);

        registerCommands();
        registerListeners();

        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "Velocity", proxyServer.getVersion().getVersion(), getWebServerConfig());
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
                logger,
                bootstrap.getConfig()
        ));

        commandManager.register("ban", new BanCommand(bootstrap.getBanExecutor(), proxyServer));
    }

    private void registerListeners() {
        logger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();

        eventManager.register(this, new WhitelistEvents(bootstrap.getWebhooksConfig(), logger));
        eventManager.register(this, new DashboardEvents(bootstrap.getWebhooksConfig(), logger));
        eventManager.register(this, new PlayerEvents(bootstrap, proxyServer));
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

    private boolean getWebServerConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Boolean.parseBoolean(String.valueOf(webserverConfig.getOrDefault("enabled", true)));
    }
}
