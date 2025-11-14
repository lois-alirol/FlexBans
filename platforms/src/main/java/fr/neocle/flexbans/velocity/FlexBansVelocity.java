package fr.neocle.flexbans.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.velocity.VelocityEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.utils.HooksUtils;
import fr.neocle.flexbans.utils.IpUtils;
import fr.neocle.flexbans.velocity.commands.*;
import fr.neocle.flexbans.velocity.listener.*;
import litebans.api.Events;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        FlexLogger.init(logger);

        Path dataFolder = Paths.get("plugins", "FlexBans");
        if (!Files.exists(dataFolder)) {
            Files.createDirectories(dataFolder);
            FlexLogger.info("Created FlexBans directory.");
        }

        ConfigManager.initialize(logger, dataFolder);
        WebhooksConfigManager.initialize(logger, dataFolder);

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
        metrics.addCustomChart(new Metrics.SimplePie("language", () -> ConfigManager.getString("language")));
        metrics.addCustomChart(new Metrics.SimplePie("https_usage", () -> String.valueOf(ConfigManager.getBoolean("webserver.https"))));

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                logger,
                eventDispatcher
        );

        bootstrap = new Bootstrap();
        bootstrap.initialize(dataFolder, logger, "velocity", eventDispatcher, proxyServer);

        FlexBansAPIImpl.setBanExecutor(bootstrap.getBanExecutor());
        FlexBansAPIImpl.setMuteExecutor(bootstrap.getMuteExecutor());
        FlexBansAPIImpl.setKickExecutor(bootstrap.getKickExecutor());

        FlexBansAPIImpl.setUnbanExecutor(bootstrap.getUnbanExecutor());
        FlexBansAPIImpl.setUnmuteExecutor(bootstrap.getUnmuteExecutor());

        FlexBansAPIImpl.setServerLockExecutor(bootstrap.getServerLockExecutor());
        FlexBansAPIImpl.setServerUnlockExecutor(bootstrap.getServerUnlockExecutor());

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
                bootstrap.getVersion(),
                logger
        ));

        commandManager.register("ban", new BanCommand(bootstrap.getBanExecutor(), proxyServer), "flexbans:ban");
        commandManager.register("mute", new MuteCommand(bootstrap.getMuteExecutor(), proxyServer), "flexbans:mute");
        commandManager.register("kick", new KickCommand(bootstrap.getKickExecutor(), proxyServer), "flexbans:kick");
        commandManager.register("warning",  new WarningCommand(bootstrap.getWarningExecutor(), proxyServer), "warn", "flexbans:warning", "flexbans:warn");
        commandManager.register("unban", new UnbanCommand(bootstrap.getUnbanExecutor(), proxyServer), "flexbans:unban");
        commandManager.register("unmute", new UnmuteCommand(bootstrap.getUnmuteExecutor(), proxyServer), "flexbans:unmute");
        commandManager.register("serverlock", new ServerLockCommand(bootstrap.getServerLockExecutor(), proxyServer), "flexbans:serverlock");
        commandManager.register("serverunlock", new ServerUnlockCommand(bootstrap.getServerUnlockExecutor(), proxyServer), "flexbans:serverunlock");
        commandManager.register("alt", new AltCommand(proxyServer, bootstrap.getDatabaseUtils()), "flexbans:alt");
    }

    private void registerListeners() {
        FlexLogger.info("Registering listeners...");
        EventManager eventManager = proxyServer.getEventManager();

        eventManager.register(this, new WhitelistEvents(logger));
        eventManager.register(this, new DashboardEvents(logger));
        eventManager.register(this, new PlayerEvents(bootstrap, proxyServer));

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
