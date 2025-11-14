package fr.neocle.flexbans;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import fr.neocle.flexbans.commands.punishments.ban.BanPlatformHandler;
import fr.neocle.flexbans.commands.punishments.ban.platforms.BukkitBan;
import fr.neocle.flexbans.commands.punishments.ban.platforms.BungeeBan;
import fr.neocle.flexbans.commands.punishments.ban.platforms.VelocityBan;
import fr.neocle.flexbans.commands.punishments.kick.KickExecutor;
import fr.neocle.flexbans.commands.punishments.kick.KickPlatformHandler;
import fr.neocle.flexbans.commands.punishments.kick.platforms.BukkitKick;
import fr.neocle.flexbans.commands.punishments.kick.platforms.BungeeKick;
import fr.neocle.flexbans.commands.punishments.kick.platforms.VelocityKick;
import fr.neocle.flexbans.commands.punishments.mute.MuteExecutor;
import fr.neocle.flexbans.commands.punishments.mute.MutePlatformHandler;
import fr.neocle.flexbans.commands.punishments.mute.platforms.BukkitMute;
import fr.neocle.flexbans.commands.punishments.mute.platforms.BungeeMute;
import fr.neocle.flexbans.commands.punishments.mute.platforms.VelocityMute;
import fr.neocle.flexbans.commands.punishments.unban.UnbanExecutor;
import fr.neocle.flexbans.commands.punishments.unmute.UnmuteExecutor;
import fr.neocle.flexbans.commands.punishments.warning.WarningExecutor;
import fr.neocle.flexbans.commands.punishments.warning.WarningPlatformHandler;
import fr.neocle.flexbans.commands.punishments.warning.platforms.VelocityWarning;
import fr.neocle.flexbans.commands.server.lock.ServerLockExecutor;
import fr.neocle.flexbans.commands.server.lock.ServerLockPlatformHandler;
import fr.neocle.flexbans.commands.server.lock.platforms.VelocityServerLock;
import fr.neocle.flexbans.commands.server.unlock.ServerUnlockExecutor;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.*;
import fr.neocle.flexbans.handlers.api.PunishmentSSEHandler;
import fr.neocle.flexbans.handlers.errors.ForbiddenError;
import fr.neocle.flexbans.handlers.errors.InternalServerError;
import fr.neocle.flexbans.handlers.errors.NotFoundError;
import fr.neocle.flexbans.handlers.post.NewPunishmentHandler;
import fr.neocle.flexbans.handlers.post.RevokePunishmentHandler;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.handlers.security.CodeVerificationHandler;
import fr.neocle.flexbans.handlers.security.LoginHandler;
import fr.neocle.flexbans.handlers.security.RegisterHandler;
import fr.neocle.flexbans.handlers.security.components.AuthComponents;
import fr.neocle.flexbans.handlers.security.components.DatabaseComponents;
import fr.neocle.flexbans.handlers.security.components.ErrorHandlers;
import fr.neocle.flexbans.handlers.security.components.HandlerRegistry;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.handlers.security.utils.DomainFilter;
import fr.neocle.flexbans.handlers.security.utils.HttpsEnforcementHandler;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.internal.UpdateChecker;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.JettyReloader;
import fr.neocle.flexbans.utils.LibsLoader;
import fr.neocle.flexbans.utils.broadcast.Broadcaster;
import fr.neocle.flexbans.utils.broadcast.BroadcasterBukkit;
import fr.neocle.flexbans.utils.broadcast.BroadcasterBungee;
import fr.neocle.flexbans.utils.broadcast.BroadcasterVelocity;
import fr.neocle.flexbans.utils.commandsexecution.CommandsExecution;
import fr.neocle.flexbans.utils.commandsexecution.CommandsExecutionBukkit;
import fr.neocle.flexbans.utils.commandsexecution.CommandsExecutionBungee;
import fr.neocle.flexbans.utils.commandsexecution.CommandsExecutionVelocity;
import fr.neocle.flexbans.utils.player.PlayerHeadImage;
import fr.neocle.flexbans.utils.player.UsernameUUIDConverters;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bootstrap {
    protected Path dataFolder;
    protected Map<String, Object> config;
    protected Map<String, Object> webhooksConfig;
    protected Logger logger;
    protected AuthenticationHandler authHandler;
    protected IndexHandler indexHandler;
    protected PlayerHistoryHandler playerHistoryHandler;
    protected ModeratorHistoryHandler moderatorHistoryHandler;
    protected PunishmentDetailsHandler punishmentDetailsHandler;
    protected RevokePunishmentHandler revokePunishmentHandler;
    protected NewPunishmentHandler newPunishmentHandler;
    protected PlayerHeadHandler playerHeadHandler;
    protected JavascriptHandler javascriptHandler;
    protected CssHandler cssHandler;
    protected ForbiddenError forbiddenError;
    protected NotFoundError notFoundError;
    protected File pluginFolder;
    protected String platform;
    protected Object pluginInstance;
    protected DatabaseUtils databaseUtils;
    protected CodeVerificationHandler codeVerificationHandler;
    protected LoginHandler loginHandler;
    protected RegisterHandler registerHandler;
    protected DiscordOAuthHandler discordOAuthHandler;
    protected EventDispatcher eventDispatcher;
    protected FlexBansAPI api;
    protected JettyReloader jettyReloader;
    protected PlayerHeadImage playerHeadImage;
    protected BanExecutor banExecutor;
    protected MuteExecutor muteExecutor;
    protected KickExecutor kickExecutor;
    protected WarningExecutor warningExecutor;
    protected UnbanExecutor unbanExecutor;
    protected UnmuteExecutor unmuteExecutor;
    protected ServerLockExecutor serverLockExecutor;
    protected ServerUnlockExecutor serverUnlockExecutor;
    protected BanPlatformHandler banPlatformHandler;
    protected MutePlatformHandler mutePlatformHandler;
    protected KickPlatformHandler kickPlatformHandler;
    protected WarningPlatformHandler warningPlatformHandler;
    protected ServerLockPlatformHandler serverLockHandler;
    protected LibsLoader libsLoader;
    protected Broadcaster broadcaster;
    protected UsernameUUIDConverters usernameUUIDConverters;
    protected PunishmentSSEHandler punishmentSSEHandler;

    public void initialize(Path dataFolder, Logger logger, String platform, EventDispatcher eventDispatcher, Object pluginInstance) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.pluginFolder = new File("plugins/FlexBans");
        this.platform = platform;
        this.pluginInstance = pluginInstance;
        this.eventDispatcher = eventDispatcher;

        try {
            libsLoader = new LibsLoader(logger);
            libsLoader.ensureSQLiteAvailable();
            libsLoader.ensureMySQLAvailable();

            ConfigManager.initialize(logger, dataFolder);

            initializeDatabase(config);
            initializeHandlers(platform);
            initializeAPI(eventDispatcher);
            initializeCommands();
            initializeLanguage();

            new UpdateChecker(getVersion(), logger).start();
        } catch (URISyntaxException | SQLException e) {
            logger.severe("Error setting up FlexBans: " + e.getMessage());
        }
    }

    private void initializeHandlers(String platform) throws URISyntaxException {
        forbiddenError = new ForbiddenError(logger);
        notFoundError = new NotFoundError(logger);

        usernameUUIDConverters = new UsernameUUIDConverters();
        playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);

        boolean playerHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.player.enabled");
        boolean moderatorHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.moderator.enabled");
        boolean punishmentDetailsEnabled = ConfigManager.getBoolean("webserver.pages.details.punishment.enabled");

        indexHandler = new IndexHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, logger);

        if (playerHistoryEnabled)
            playerHistoryHandler = new PlayerHistoryHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, logger, notFoundError);
        if (moderatorHistoryEnabled)
            moderatorHistoryHandler = new ModeratorHistoryHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, logger, notFoundError);
        if (punishmentDetailsEnabled)
            punishmentDetailsHandler = new PunishmentDetailsHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, logger, notFoundError);

        playerHeadHandler = new PlayerHeadHandler(dataFolder, notFoundError);
        javascriptHandler = new JavascriptHandler(notFoundError);
        cssHandler = new CssHandler(notFoundError);

        CommandsExecution commandsExecution;

        switch (platform.toLowerCase()) {
            case "bungee":
                commandsExecution = new CommandsExecutionBungee();
                broadcaster = new BroadcasterBungee((net.md_5.bungee.api.ProxyServer) pluginInstance);
                banPlatformHandler = new BungeeBan((net.md_5.bungee.api.ProxyServer) pluginInstance);
                mutePlatformHandler = new BungeeMute((net.md_5.bungee.api.ProxyServer) pluginInstance);
                kickPlatformHandler = new BungeeKick((net.md_5.bungee.api.ProxyServer) pluginInstance);
                warningPlatformHandler = null;
                break;

            case "spigot":
                commandsExecution = new CommandsExecutionBukkit((JavaPlugin) pluginInstance);
                broadcaster = new BroadcasterBukkit();
                banPlatformHandler = new BukkitBan();
                mutePlatformHandler = new BukkitMute();
                kickPlatformHandler = new BukkitKick();
                warningPlatformHandler = null;
                break;

            case "velocity":
                commandsExecution = new CommandsExecutionVelocity((ProxyServer) pluginInstance);
                broadcaster = new BroadcasterVelocity((ProxyServer) pluginInstance);
                banPlatformHandler = new VelocityBan((ProxyServer) pluginInstance);
                mutePlatformHandler = new VelocityMute((ProxyServer) pluginInstance);
                kickPlatformHandler = new VelocityKick((ProxyServer) pluginInstance);
                warningPlatformHandler = new VelocityWarning((ProxyServer) pluginInstance);

                serverLockHandler = new VelocityServerLock((ProxyServer) pluginInstance);
                break;
            default:
                logger.severe("Unsupported platform: " + platform);
                return;
        }

        revokePunishmentHandler = new RevokePunishmentHandler(commandsExecution, logger);
        codeVerificationHandler = new CodeVerificationHandler(logger, databaseUtils);
        registerHandler = new RegisterHandler(logger, databaseUtils, eventDispatcher);
        loginHandler = new LoginHandler(logger, databaseUtils, eventDispatcher);
        discordOAuthHandler = new DiscordOAuthHandler(databaseUtils, forbiddenError, eventDispatcher, logger);
        newPunishmentHandler = new NewPunishmentHandler(commandsExecution, databaseUtils, logger);
        punishmentSSEHandler = new PunishmentSSEHandler();

        HandlerRegistry handlerRegistry = new HandlerRegistry(
                indexHandler,
                playerHistoryHandler,
                moderatorHistoryHandler,
                punishmentDetailsHandler,
                playerHeadHandler,
                javascriptHandler,
                cssHandler,
                revokePunishmentHandler,
                newPunishmentHandler,
                punishmentSSEHandler
        );

        ErrorHandlers errorHandlers = new ErrorHandlers(
                forbiddenError,
                notFoundError
        );

        DatabaseComponents databaseComponents = new DatabaseComponents(
                databaseUtils
        );

        AuthComponents authComponents = new AuthComponents(
                codeVerificationHandler,
                registerHandler,
                loginHandler,
                discordOAuthHandler
        );

        authHandler = new AuthenticationHandler.Builder()
                .handlerRegistry(handlerRegistry)
                .errorHandlers(errorHandlers)
                .databaseComponents(databaseComponents)
                .authComponents(authComponents)
                .eventDispatcher(eventDispatcher)
                .logger(logger)
                .build();
    }

    public void initializeLanguage() {
        String lang = ConfigManager.getString("language");

        if ("locale".equalsIgnoreCase(lang)) {
            Locale defaultLocale = Locale.getDefault();
            lang = defaultLocale.getLanguage() + "_" + defaultLocale.getCountry();
        }

        logger.info("Loading language file: " + lang + "...");
        LanguageManager.initialize(logger, dataFolder);
        LanguageManager.loadLanguage(lang);
    }

    public void initializeCommands() {
        banExecutor = new BanExecutor(banPlatformHandler, broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher, punishmentSSEHandler);
        muteExecutor = new MuteExecutor(mutePlatformHandler, broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher);
        kickExecutor = new KickExecutor(kickPlatformHandler, broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher);
        warningExecutor = new WarningExecutor(warningPlatformHandler, broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher, punishmentSSEHandler);

        unbanExecutor = new UnbanExecutor(broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher,  punishmentSSEHandler);
        unmuteExecutor = new UnmuteExecutor(broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher,  punishmentSSEHandler);

        serverLockExecutor = new ServerLockExecutor(serverLockHandler, broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher);
        serverUnlockExecutor = new ServerUnlockExecutor(broadcaster, usernameUUIDConverters, databaseUtils, eventDispatcher);
    }

    public void initializeAPI(EventDispatcher eventDispatcher) {
        api = FlexBansAPI.getInstance();
    }

    public void initializeDatabase(Map<String, Object> config) throws SQLException {
        String type = ConfigManager.getString("database.type");
        String host = ConfigManager.getString("database.hostname");

        int port = ConfigManager.getInt("database.port");
        String database = ConfigManager.getString("database.database");
        String username = ConfigManager.getString("database.username");
        String password = ConfigManager.getString("database.password");

        databaseUtils = new DatabaseUtils("./plugins/FlexBans", type, host, port, database, username, password, logger);
        databaseUtils.initialize();
    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler(logger);
        ResourceHandler resourceHandler = new ResourceHandler();
        DomainFilter domainFilter = new DomainFilter(logger);
        HomeHandler homeHandler = new HomeHandler();

        jettyReloader = new JettyReloader(server, logger);

        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"home.html"});
        resourceHandler.setResourceBase(getClass().getClassLoader().getResource("web").toExternalForm());

        InternalServerError errorHandler = new InternalServerError(logger);
        server.setErrorHandler(errorHandler);

        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(securityHandler);
        handlerList.addHandler(domainFilter);
        handlerList.addHandler(homeHandler);
        handlerList.addHandler(playerHeadHandler);
        handlerList.addHandler(authHandler);
        handlerList.addHandler(resourceHandler);
        handlerList.addHandler(new DefaultHandler());

        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setHandler(handlerList);
        sessionHandler.getSessionCookieConfig().setHttpOnly(true);
        sessionHandler.getSessionCookieConfig().setSecure(false);
        server.setHandler(sessionHandler);

        try {
            server.start();
            new Thread(() -> {
                try {
                    server.join();
                } catch (InterruptedException e) {
                    logger.severe("Error while joining server thread: " + e.getMessage());
                    logger.log(Level.SEVERE, "Detailed exception information", e);
                }
            }).start();
        } catch (Exception e) {
            logger.severe("Failed to start web server on port " + port + ": " + e.getMessage());
            logger.log(Level.SEVERE, "Detailed exception information", e);
        }
    }

    public void logServerStartupInfo(String address, int port, String platform, String version, boolean isWebServerEnabled) {
        String yellow = "\u001B[38;5;214m";
        String lightYellow = "\u001B[38;5;228m";
        String reset = "\u001B[0m";

        String buyerId = LicenseChecker.getDiscordId(ConfigManager.getString("license-key"));
        String buyerName = discordOAuthHandler.getUsernameFromId(buyerId);

        logger.info(yellow + "    ________          ____                  " + reset);
        logger.info(yellow + "   / ____/ /__  _  __/ __ )____ _____  _____" + reset);
        logger.info(yellow + "  / /_  / / _ \\| |/_/ __  / __ `/ __ \\/ ___/" + reset);
        logger.info(yellow + " / __/ / /  __/>  </ /_/ / /_/ / / / (__  ) " + reset);
        logger.info(yellow + "/_/   /_/\\___/_/|_/_____/\\__,_/_/ /_/____/  " + reset);
        logger.info(yellow + "=======================================================================" + reset);
        if (isWebServerEnabled) {
            logger.info(yellow + "Webserver is running on " + lightYellow + address + ":" + port + reset);
        }
        logger.info(yellow + "Platform: " + lightYellow + platform + " " + version + reset);
        logger.info(yellow + "Developer: " + lightYellow + "Neocle" + reset);
        logger.info(yellow + "Licensed to: " + lightYellow + "@" + buyerName + reset);
        logger.info(yellow + "=======================================================================" + reset);
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public Logger getLogger() {
        return logger;
    }

    public AuthenticationHandler getAuthenticatorHandler() {
        return authHandler;
    }

    public DiscordOAuthHandler getDiscordOAuthHandler() {
        return discordOAuthHandler;
    }

    public IndexHandler getIndexHandler() {
        return indexHandler;
    }

    public JettyReloader getJettyReloader() {
        return jettyReloader;
    }

    public FlexBansAPI getAPI() {
        return api;
    }

    public DatabaseUtils getDatabaseUtils() {
        return databaseUtils;
    }

    public PlayerHeadImage getPlayerHeadImage() {
        return playerHeadImage;
    }

    public BanExecutor getBanExecutor() {
        return banExecutor;
    }

    public MuteExecutor getMuteExecutor() {
        return muteExecutor;
    }

    public KickExecutor getKickExecutor() {
        return kickExecutor;
    }

    public WarningExecutor getWarningExecutor() { return warningExecutor; }

    public UnbanExecutor getUnbanExecutor() {
        return unbanExecutor;
    }

    public UnmuteExecutor getUnmuteExecutor() { return unmuteExecutor; }

    public ServerLockExecutor getServerLockExecutor() {
        return serverLockExecutor;
    }

    public ServerUnlockExecutor getServerUnlockExecutor() {
        return serverUnlockExecutor;
    }

    public PunishmentSSEHandler getPunishmentSSEHandler() {
        return punishmentSSEHandler;
    }

    public String getVersion() {
        return "0.9.9-SNAPSHOT";
    }
}
