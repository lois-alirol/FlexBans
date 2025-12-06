package fr.neocle.flexbans.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.velocity.VelocityEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.common.database.DatabaseInitializer;
import fr.neocle.flexbans.common.listener.PlayerEventHandler;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.config.WebhooksConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.HooksUtils;
import fr.neocle.flexbans.util.IpUtils;
import fr.neocle.flexbans.util.LibsLoader;
import fr.neocle.flexbans.velocity.command.*;
import fr.neocle.flexbans.velocity.command.adapter.VelocityPlatform;
import fr.neocle.flexbans.velocity.command.lookup.AltCommand;
import fr.neocle.flexbans.velocity.command.lookup.HistoryCommand;
import fr.neocle.flexbans.velocity.command.punishment.*;
import fr.neocle.flexbans.velocity.command.server.ServerLockCommand;
import fr.neocle.flexbans.velocity.command.server.ServerUnlockCommand;
import fr.neocle.flexbans.velocity.handler.VelocityHandlerFactory;
import fr.neocle.flexbans.velocity.listener.*;
import litebans.api.Events;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.Logger;

public class FlexBansVelocity {
    private final ProxyServer proxyServer;
    private final Metrics.Factory metricsFactory;
    private final Logger logger;
    private Bootstrap bootstrap;

    public static final MinecraftChannelIdentifier MUTE_QUERY_CHANNEL = MinecraftChannelIdentifier.from("muting:query");
    public static final MinecraftChannelIdentifier MUTE_RESPONSE_CHANNEL = MinecraftChannelIdentifier.from("muting:response");

    @Inject
    public FlexBansVelocity(ProxyServer proxyServer, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.metricsFactory = metricsFactory;
        this.logger = Logger.getLogger("FlexBans");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException, SQLException {
        FlexLogger.init(logger);

        Path dataFolder = Paths.get("plugins", "FlexBans");
        if (!Files.exists(dataFolder)) {
            Files.createDirectories(dataFolder);
            FlexLogger.info("Created FlexBans directory.");
        }

        ConfigManager.initialize(dataFolder);
        WebhooksConfigManager.initialize(dataFolder);

        String ip = IpUtils.getPublicIP();
        String licenseKey = ConfigManager.getString("license-key");
        boolean isLicenseValid = LicenseChecker.isLicenseValid(licenseKey, ip);

        if (!isLicenseValid) {
            FlexLogger.error("      / \\\\");
            FlexLogger.error("     /   \\\\");
            FlexLogger.error("    /  |  \\\\     Your license key is invalid!");
            FlexLogger.error("   /   |   \\\\    Make sure you followed setup instructions correctly");
            FlexLogger.error("  /         \\\\   You must join Neocle Resources discord server to get your key");
            FlexLogger.error(" /     o     \\\\");
            FlexLogger.error("/_____________\\\\");
            return;
        }

        EventDispatcher eventDispatcher = new VelocityEventDispatcher(proxyServer);

        int pluginId = 23869;
        @SuppressWarnings("unused")
        Metrics metrics = metricsFactory.make(this, pluginId);
        metrics.addCustomChart(new SimplePie("language", () -> ConfigManager.getString("language")));
        metrics.addCustomChart(new SimplePie("https_usage", () -> String.valueOf(ConfigManager.getBoolean("webserver.https"))));

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                logger,
                eventDispatcher
        );

        LibsLoader libsLoader = new LibsLoader();
        libsLoader.ensureSQLiteAvailable();
        libsLoader.ensureMySQLAvailable();

        DatabaseInitializer dbInitializer = new DatabaseInitializer();
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
        FlexLogger.info("Shutting down schedulers...");
        bootstrap.getDatabaseUtils().shutdown();
        bootstrap.getPlayerHeadImage().shutdown();

        FlexLogger.info("FlexBans disabled successfully!");
    }

    private void registerCommands() {
        FlexLogger.info("Registering commands...");
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
                bootstrap.getVersion()
        ));

        commandManager.register("ban", new BanCommand(bootstrap.getBanExecutor(), proxyServer), "ipban", "banip", "ban-ip", "ip-ban", "flexbans:ban", "flexbans:ipban", "flexbans:banip", "flexbans:ban-ip", "flexbans:ip-ban");
        commandManager.register("mute", new MuteCommand(bootstrap.getMuteExecutor(), proxyServer), "flexbans:mute");
        commandManager.register("kick", new KickCommand(bootstrap.getKickExecutor(), proxyServer), "flexbans:kick");
        commandManager.register("warning",  new WarningCommand(bootstrap.getWarningExecutor(), proxyServer), "warn", "flexbans:warning", "flexbans:warn");
        commandManager.register("unban", new UnbanCommand(bootstrap.getUnbanExecutor(), proxyServer), "flexbans:unban");
        commandManager.register("unmute", new UnmuteCommand(bootstrap.getUnmuteExecutor(), proxyServer), "flexbans:unmute");
        commandManager.register("serverlock", new ServerLockCommand(bootstrap.getServerLockExecutor(), proxyServer), "flexbans:serverlock");
        commandManager.register("serverunlock", new ServerUnlockCommand(bootstrap.getServerUnlockExecutor(), proxyServer), "flexbans:serverunlock");
        commandManager.register("alt", new AltCommand(proxyServer, bootstrap.getDatabaseUtils()), "flexbans:alt");
        commandManager.register("history", new HistoryCommand(proxyServer, bootstrap.getDatabaseUtils(), bootstrap.getDatabaseUtils().getDatabaseConnectionManager()), "flexbans:history");
    }

    private void registerListeners() {
        FlexLogger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();

        VelocityPlatform platformAdapter = new VelocityPlatform(proxyServer);
        PlayerEventHandler commonHandler = new PlayerEventHandler(bootstrap, platformAdapter);

        eventManager.register(this, new WhitelistEvents());
        eventManager.register(this, new DashboardEvents());
        eventManager.register(this, new PlayerEvents(commonHandler, platformAdapter));

        if (HooksUtils.usingLiteBansSystem()) {
            LiteBansEvents listener = new LiteBansEvents(bootstrap.getDatabaseUtils());
            Events.get().register(listener);
        } else if (HooksUtils.usingFlexBansSystem()) {
            eventManager.register(this, new FlexBansEvents(bootstrap.getDatabaseUtils(), bootstrap.getPunishmentSSEHandler()));
        }
    }

    private void registerChannels() {
        proxyServer.getChannelRegistrar().register(MUTE_QUERY_CHANNEL);
        proxyServer.getChannelRegistrar().register(MUTE_RESPONSE_CHANNEL);
    }
}
