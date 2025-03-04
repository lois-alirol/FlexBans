package fr.neocle.litebansweb.velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import fr.neocle.litebansweb.velocity.commands.BaseCommandVelocity;
import fr.neocle.litebansweb.velocity.listener.DashboardEvents;
import fr.neocle.litebansweb.velocity.listener.WhitelistEvents;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.Bootstrap;
import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.api.events.velocity.VelocityEventDispatcher;
import fr.neocle.litebansweb.api.impl.LitebansWebAPIImpl;

import javax.inject.Inject;

import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

@Plugin(id = "litebansweb", name = "Litebans Web", version = "1.0-SNAPSHOT", authors = {"Neocle"})
public class LitebansWebVelocity {
    private final ProxyServer proxyServer;
    private final Metrics.Factory metricsFactory;
    private final Logger logger;
    private Bootstrap bootstrap;

    @Inject
    public LitebansWebVelocity(ProxyServer proxyServer, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.metricsFactory = metricsFactory;
        this.logger = Logger.getLogger("LitebansWeb");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        EventDispatcher eventDispatcher = new VelocityEventDispatcher(proxyServer);

        LitebansWebAPIImpl.initialize(
            Paths.get("plugins", "LitebansWeb", "config.yml"), 
            logger, 
            eventDispatcher
        );

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), logger, "velocity", proxyServer, eventDispatcher);

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);

        registerCommands();
        registerListeners();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "Velocity", proxyServer.getVersion().getVersion());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        return;
    }

    private void registerCommands() {
        logger.info("Registering commands...");
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("litebansweb").aliases("lbw", "lw").build(), new BaseCommandVelocity(
            bootstrap.getAPI(),
            proxyServer,
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", logger),
            logger
        ));
    }

    private void registerListeners() {
        logger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();
        
        eventManager.register(this, new WhitelistEvents(bootstrap.getWebhooksConfig(), logger));
        eventManager.register(this, new DashboardEvents(bootstrap.getWebhooksConfig(), logger));
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
}
