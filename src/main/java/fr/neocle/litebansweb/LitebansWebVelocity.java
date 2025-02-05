package fr.neocle.litebansweb;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import fr.neocle.litebansweb.commands.BaseCommandVelocity;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import org.bstats.velocity.Metrics;

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
        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), logger, "velocity", proxyServer);

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getPortFromConfig(), "Velocity", proxyServer.getVersion().getVersion());
    }

    private int getPortFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Integer.parseInt(String.valueOf(webserverConfig.getOrDefault("port", "8080")));
    }

    private void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("litebansweb").aliases("lbw", "lw").build(), new BaseCommandVelocity(
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", logger),
            logger
        ));
    }
}
