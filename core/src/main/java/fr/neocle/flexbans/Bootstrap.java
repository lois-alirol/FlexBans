package fr.neocle.flexbans;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import fr.neocle.flexbans.commands.punishments.ban.BanPlatformHandler;
import fr.neocle.flexbans.commands.punishments.ban.platforms.VelocityBan;
import fr.neocle.flexbans.commands.punishments.unban.UnbanExecutor;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.Errors.ForbiddenError;
import fr.neocle.flexbans.handlers.Errors.InternalServerError;
import fr.neocle.flexbans.handlers.Errors.NotFoundError;
import fr.neocle.flexbans.handlers.*;
import fr.neocle.flexbans.handlers.PostRequestHandlers.NewPunishmentHandler;
import fr.neocle.flexbans.handlers.PostRequestHandlers.RevokePunishmentHandler;
import fr.neocle.flexbans.handlers.Security.AuthenticationHandler;
import fr.neocle.flexbans.handlers.Security.CodeVerificationHandler;
import fr.neocle.flexbans.handlers.Security.LoginHandler;
import fr.neocle.flexbans.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.flexbans.handlers.Security.RegisterHandler;
import fr.neocle.flexbans.handlers.Security.Utils.DomainFilter;
import fr.neocle.flexbans.handlers.Security.Utils.HttpsEnforcementHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Broadcast.BroadcasterVelocity;
import fr.neocle.flexbans.utils.CommandsExecution.CommandsExecution;
import fr.neocle.flexbans.utils.CommandsExecution.CommandsExecutionBukkit;
import fr.neocle.flexbans.utils.CommandsExecution.CommandsExecutionBungee;
import fr.neocle.flexbans.utils.CommandsExecution.CommandsExecutionVelocity;
import fr.neocle.flexbans.utils.DurationCalculator;
import fr.neocle.flexbans.utils.JettyReloader;
import fr.neocle.flexbans.utils.LibsLoader;
import fr.neocle.flexbans.utils.Player.PlayerHeadImage;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import fr.neocle.flexbans.utils.ResourceLoader;
import fr.neocle.flexbans.utils.Security.CodeGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
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
    protected ScriptsHandler scriptsHandler;
    protected ForbiddenError forbiddenError;
    protected NotFoundError notFoundError;
    protected File pluginFolder;
    protected String platform;
    protected Object pluginInstance;
    protected DatabaseUtils databaseUtils;
    protected CodeVerificationHandler codeVerificationHandler;
    protected CodeGenerator codeGenerator;
    protected LoginHandler loginHandler;
    protected RegisterHandler registerHandler;
    protected DiscordOAuthHandler discordOAuthHandler;
    protected EventDispatcher eventDispatcher;
    protected FlexBansAPI api;
    protected JettyReloader jettyReloader;
    protected PlayerHeadImage playerHeadImage;
    protected BanExecutor banExecutor;
    protected UnbanExecutor unbanExecutor;
    protected BanPlatformHandler banPlatformHandler;
    protected LibsLoader libsLoader;
    protected Broadcaster broadcaster;
    protected UsernameUUIDConverters usernameUUIDConverters;

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

            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
                logger.info("Created FlexBans directory.");
            }

            ConfigManager.initialize(logger, dataFolder);
            WebhooksConfigManager.initialize(logger, dataFolder);

            initializeDatabase(config);
            initializeHandlers(platform);
            initializeAPI(eventDispatcher);
            initializeCommands();
            initializeLanguage();
        } catch (IOException | URISyntaxException | SQLException e) {
            logger.severe("Error setting up FlexBans: " + e.getMessage());
        }
    }

    private void initializeHandlers(String platform) throws URISyntaxException {
        forbiddenError = new ForbiddenError(logger);
        notFoundError = new NotFoundError(logger);

        usernameUUIDConverters = new UsernameUUIDConverters();
        playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);
        DurationCalculator durationCalculator = new DurationCalculator();

        indexHandler = new IndexHandler(usernameUUIDConverters, durationCalculator, playerHeadImage);
        playerHistoryHandler = new PlayerHistoryHandler(usernameUUIDConverters, durationCalculator, playerHeadImage);
        moderatorHistoryHandler = new ModeratorHistoryHandler(usernameUUIDConverters, durationCalculator, playerHeadImage);
        punishmentDetailsHandler = new PunishmentDetailsHandler(usernameUUIDConverters, durationCalculator, playerHeadImage, databaseUtils);
        playerHeadHandler = new PlayerHeadHandler(dataFolder, notFoundError);

        scriptsHandler = new ScriptsHandler(notFoundError);

        CommandsExecution commandsExecution;

        switch (platform.toLowerCase()) {
            case "bungee":
                commandsExecution = new CommandsExecutionBungee();
                break;
            case "spigot":
                commandsExecution = new CommandsExecutionBukkit();
                break;
            case "velocity":
                commandsExecution = new CommandsExecutionVelocity((ProxyServer) pluginInstance);
                broadcaster = new BroadcasterVelocity((ProxyServer) pluginInstance);

                banPlatformHandler = new VelocityBan((ProxyServer) pluginInstance);
                break;
            default:
                logger.severe("Unsupported platform: " + platform);
                return;
        }

        revokePunishmentHandler = new RevokePunishmentHandler(commandsExecution);
        codeGenerator = new CodeGenerator();
        codeVerificationHandler = new CodeVerificationHandler(logger, codeGenerator, databaseUtils);
        registerHandler = new RegisterHandler(logger, databaseUtils, eventDispatcher);
        loginHandler = new LoginHandler(logger, databaseUtils, eventDispatcher);
        discordOAuthHandler = new DiscordOAuthHandler(databaseUtils, forbiddenError, eventDispatcher, logger);
        newPunishmentHandler = new NewPunishmentHandler(commandsExecution, databaseUtils);

        authHandler = new AuthenticationHandler(
                indexHandler,
                playerHistoryHandler,
                moderatorHistoryHandler,
                punishmentDetailsHandler,
                playerHeadHandler,
                scriptsHandler,
                revokePunishmentHandler,
                null,
                forbiddenError,
                notFoundError,
                databaseUtils,
                codeVerificationHandler,
                registerHandler,
                loginHandler,
                discordOAuthHandler,
                eventDispatcher,
                logger
        );

        authHandler.setNewPunishmentHandler(newPunishmentHandler);

    }

    public void initializeLanguage() {
        String lang = (String) ConfigManager.getConfigValue("language");

        if ("locale".equalsIgnoreCase(lang)) {
            Locale defaultLocale = Locale.getDefault();
            lang = defaultLocale.getLanguage() + "_" + defaultLocale.getCountry();
        }

        logger.info("Loading language file: " + lang + "...");
        LanguageManager.initialize(logger, dataFolder);
        LanguageManager.loadLanguage(lang);
    }

    public void initializeCommands() {
        banExecutor = new BanExecutor(banPlatformHandler, broadcaster, usernameUUIDConverters, databaseUtils);
        unbanExecutor = new UnbanExecutor(broadcaster, usernameUUIDConverters, databaseUtils);
    }

    public void initializeAPI(EventDispatcher eventDispatcher) {
        api = FlexBansAPI.getInstance();
    }

    public void initializeDatabase(Map<String, Object> config) throws SQLException {
        String type = (String) ConfigManager.getConfigValue("database.type");
        String host = (String) ConfigManager.getConfigValue("database.hostname");
        int port = Integer.parseInt((String) ConfigManager.getConfigValue("database.port"));
        String database = (String) ConfigManager.getConfigValue("database.database");
        String username = (String) ConfigManager.getConfigValue("database.username");
        String password = (String) ConfigManager.getConfigValue("database.password");

        databaseUtils = new DatabaseUtils("./plugins/FlexBans", type, host, port, database, username, password, logger);
        databaseUtils.initialize();
    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler(config, logger);
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
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logServerStartupInfo(String address, int port, String platform, String version, boolean isWebServerEnabled) {
        String yellow = "\u001B[38;5;214m";
        String lightYellow = "\u001B[38;5;228m";
        String reset = "\u001B[0m";

        logger.info(yellow + "    ________          ____                  " + reset);
        logger.info(yellow + "   / ____/ /__  _  __/ __ )____ _____  _____" + reset);
        logger.info(yellow + "  / /_  / / _ \\| |/_/ __  / __ `/ __ \\/ ___/" + reset);
        logger.info(yellow + " / __/ / /  __/>  </ /_/ / /_/ / / / (__  ) " + reset);
        logger.info(yellow + "/_/   /_/\\___/_/|_/_____/\\__,_/_/ /_/____/  " + reset);
        logger.info(yellow + "=======================================================================" + reset);
        if (isWebServerEnabled) {
            logger.info(yellow + "Webserver is running on " + lightYellow + address + ":" + port + reset);
        }
        logger.info(yellow + "Running on platform: " + lightYellow + platform + " " + version + reset);
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

    public UnbanExecutor getUnbanExecutor() {
        return unbanExecutor;
    }
}
