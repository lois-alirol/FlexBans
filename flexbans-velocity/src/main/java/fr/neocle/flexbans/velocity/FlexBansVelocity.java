package fr.neocle.flexbans.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.velocity.VelocityEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.common.database.DatabaseInitializer;
import fr.neocle.flexbans.common.listener.PlayerEventHandler;
import fr.neocle.flexbans.common.messaging.Channel;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.config.WebhooksConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.HooksUtils;
import fr.neocle.flexbans.util.IpUtils;
import fr.neocle.flexbans.util.loader.LibsLoader;
import fr.neocle.flexbans.velocity.command.*;
import fr.neocle.flexbans.velocity.command.adapter.VelocityPlatform;
import fr.neocle.flexbans.velocity.command.lookup.AltCommand;
import fr.neocle.flexbans.velocity.command.lookup.HistoryCommand;
import fr.neocle.flexbans.velocity.command.lookup.ModeratorHistoryCommand;
import fr.neocle.flexbans.velocity.command.punishment.*;
import fr.neocle.flexbans.velocity.command.server.ServerLockCommand;
import fr.neocle.flexbans.velocity.command.server.ServerUnlockCommand;
import fr.neocle.flexbans.velocity.handler.VelocityHandlerFactory;
import fr.neocle.flexbans.velocity.listener.*;
import fr.neocle.flexbans.velocity.util.LiteBansHook;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Logger;

public class FlexBansVelocity {
    private final ProxyServer proxyServer;
    private final Metrics.Factory metricsFactory;
    private final Logger logger;
    private final Path dataFolder;
    private Bootstrap bootstrap;

    private static final MinecraftChannelIdentifier MUTE_QUERY_CHANNEL = MinecraftChannelIdentifier.from(Channel.MUTED_QUERY);
    private static final MinecraftChannelIdentifier MUTE_RESPONSE_CHANNEL = MinecraftChannelIdentifier.from(Channel.MUTED_RESPONSE);
    private static final MinecraftChannelIdentifier BAN_CHANNEL = MinecraftChannelIdentifier.from(Channel.BAN);
    private static final MinecraftChannelIdentifier MUTE_CHANNEL = MinecraftChannelIdentifier.from(Channel.MUTE);
    private static final MinecraftChannelIdentifier KICK_CHANNEL = MinecraftChannelIdentifier.from(Channel.KICK);
    private static final MinecraftChannelIdentifier WARNING_CHANNEL = MinecraftChannelIdentifier.from(Channel.WARNING);

    @Inject
    public FlexBansVelocity(ProxyServer proxyServer, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataFolder) {
        this.proxyServer = proxyServer;
        this.logger = Logger.getLogger("FlexBans");
        this.metricsFactory = metricsFactory;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException, SQLException {
        FlexLogger.init(logger);

        if (!Files.exists(dataFolder)) {
            Files.createDirectories(dataFolder);
            logger.info("Created FlexBans directory.");
        }

        ConfigManager.initialize(dataFolder);
        WebhooksConfigManager.initialize(dataFolder);

        String ip = IpUtils.getPublicIP();
        String licenseKey = ConfigManager.getString("license-key");
        boolean isLicenseValid = LicenseChecker.isLicenseValid(licenseKey, ip);

        if (!isLicenseValid) {
            logger.severe(".---------------------------------------.");
            logger.severe("|   / \\\\         W A R N I N G          |");
            logger.severe("|  / | \\\\                               |");
            logger.severe("| /  o  \\\\   Invalid license key!       |");
            logger.severe("| ‾‾‾‾‾‾‾‾                              |");
            logger.severe("|   Join Neocle Resources discord to    |");
            logger.severe("|   to obtain a valid license key       |");
            logger.severe("'---------------------------------------'");

            return;
        }

        EventDispatcher eventDispatcher = new VelocityEventDispatcher(proxyServer);

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);
        metrics.addCustomChart(new SimplePie("language", () -> ConfigManager.getString("language")));
        metrics.addCustomChart(new SimplePie("https_usage", () -> String.valueOf(ConfigManager.getBoolean("webserver.https"))));

        FlexBansAPIImpl.initialize(
                dataFolder.resolve("config.yml"),
                logger,
                eventDispatcher
        );

        String type = ConfigManager.getString("database.type");

        LibsLoader libsLoader = new LibsLoader();
        libsLoader.loadDriver(type);

        DatabaseInitializer dbInitializer = new DatabaseInitializer(dataFolder);
        DatabaseUtils dbUtils = dbInitializer.initialize();

        PlatformHandlerFactory handlerFactory = new VelocityHandlerFactory(proxyServer, dbUtils);

        bootstrap = new Bootstrap();
        bootstrap.initialize(dataFolder, logger, eventDispatcher, dbUtils, handlerFactory);

        int port = ConfigManager.getInt("webserver.port");
        boolean webserverEnabled = ConfigManager.getBoolean("webserver.enabled");
        String url = ConfigManager.getString("webserver.url");

        if (webserverEnabled) {
            bootstrap.startWebServer(port);
        }

        registerCommands();
        registerListeners();
        registerChannels();

        bootstrap.logServerStartupInfo(url, port, "Velocity", proxyServer.getVersion().getVersion(), webserverEnabled);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down...");
        bootstrap.shutdown();

        logger.info("FlexBans disabled successfully!");
    }

    private void registerCommands() {
        logger.info("Registering commands...");
        CommandManager commandManager = proxyServer.getCommandManager();

        BaseCommandVelocity.register(
                proxyServer,
                bootstrap.getDataFolder(),
                bootstrap.getJettyReloader(),
                bootstrap.getDatabaseUtils(),
                bootstrap.getVersion()
        );

        BanCommand.register(
                commandManager,
                bootstrap.getBanExecutor(),
                proxyServer
        );

        MuteCommand.register(
                commandManager,
                bootstrap.getMuteExecutor(),
                proxyServer
        );

        KickCommand.register(
                commandManager,
                bootstrap.getKickExecutor(),
                proxyServer
        );

        WarningCommand.register(
                commandManager,
                bootstrap.getWarningExecutor(),
                proxyServer
        );

        UnbanCommand.register(
                commandManager,
                bootstrap.getUnbanExecutor(),
                proxyServer
        );

        UnmuteCommand.register(
                commandManager,
                bootstrap.getUnmuteExecutor(),
                proxyServer
        );

        ServerLockCommand.register(
                commandManager,
                bootstrap.getServerLockExecutor(),
                proxyServer
        );

        ServerUnlockCommand.register(
                commandManager,
                bootstrap.getServerUnlockExecutor(),
                proxyServer
        );

        AltCommand.register(commandManager, proxyServer, bootstrap.getDatabaseUtils());
        HistoryCommand.register(commandManager, proxyServer, bootstrap.getDatabaseUtils(),
                                bootstrap.getDatabaseUtils().getDatabaseConnectionManager());
        ModeratorHistoryCommand.register(commandManager, proxyServer, bootstrap.getDatabaseUtils(),
                                         bootstrap.getDatabaseUtils().getDatabaseConnectionManager());

    }

    private void registerListeners() {
        logger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();

        VelocityPlatform platformAdapter = new VelocityPlatform(proxyServer);
        PlayerEventHandler commonHandler = new PlayerEventHandler(bootstrap, platformAdapter);

        eventManager.register(this, new WhitelistEvents());
        eventManager.register(this, new DashboardEvents());
        eventManager.register(this, new BackendRequests(proxyServer));
        eventManager.register(this, new PlayerEvents(commonHandler, platformAdapter));

        if (HooksUtils.usingLiteBansSystem()) {
            try {
                LiteBansHook.register(bootstrap.getDatabaseUtils());
            } catch (NoClassDefFoundError e) {
                logger.warning("LiteBans was detected but API is unreachable.");
            }
        } else if (HooksUtils.usingFlexBansSystem()) {
            eventManager.register(this, new FlexBansEvents(bootstrap.getDatabaseUtils()));
        }
    }

    private void registerChannels() {
        proxyServer.getChannelRegistrar().register(MUTE_QUERY_CHANNEL);
        proxyServer.getChannelRegistrar().register(MUTE_RESPONSE_CHANNEL);

        proxyServer.getChannelRegistrar().register(BAN_CHANNEL);
        proxyServer.getChannelRegistrar().register(MUTE_CHANNEL);
        proxyServer.getChannelRegistrar().register(KICK_CHANNEL);
        proxyServer.getChannelRegistrar().register(WARNING_CHANNEL);
    }
}
