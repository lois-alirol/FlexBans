package fr.neocle.flexbans;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import fr.neocle.flexbans.commands.punishments.ban.BanPlatformHandler;
import fr.neocle.flexbans.commands.punishments.ban.platforms.VelocityBan;
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

            Path configFilePath = dataFolder.resolve("config.yml");
            Path webhooksConfigFilePath = dataFolder.resolve("webhooks.yml");

            if (!Files.exists(configFilePath)) {
                try (InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (defaultConfig != null) {
                        Files.copy(defaultConfig, configFilePath);
                        logger.info("Created default config.yml.");
                    } else {
                        logger.severe("Default config.yml not found in plugin resources.");
                    }
                }
            }

            if (!Files.exists(webhooksConfigFilePath)) {
                try (InputStream defaultWebHooksConfig = getClass().getClassLoader().getResourceAsStream("webhooks.yml")) {
                    if (defaultWebHooksConfig != null) {
                        Files.copy(defaultWebHooksConfig, webhooksConfigFilePath);
                        logger.info("Created default webhooks.yml.");
                    } else {
                        logger.severe("Default webhooks.yml not found in plugin resources.");
                    }
                }
            }

            config = ResourceLoader.loadConfig(dataFolder, logger);
            if (config == null) {
                logger.severe("Failed to load the config. Web server not started.");
                return;
            }

            webhooksConfig = ResourceLoader.loadWebhooksConfig(dataFolder, logger);
            if (webhooksConfig == null) {
                logger.severe("Failed to load webhooks config. Those will not work.");
                return;
            }

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
        forbiddenError = new ForbiddenError(config, logger);
        notFoundError = new NotFoundError(config, logger);

        usernameUUIDConverters = new UsernameUUIDConverters();
        playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);
        DurationCalculator durationCalculator = new DurationCalculator();

        indexHandler = new IndexHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        playerHistoryHandler = new PlayerHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        moderatorHistoryHandler = new ModeratorHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        punishmentDetailsHandler = new PunishmentDetailsHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage, databaseUtils);
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

        @SuppressWarnings("unchecked")
        Map<String, Object> oauthConfig = (Map<String, Object>) config.get("discord-oauth");

        @SuppressWarnings("unchecked")
        Map<String, Object> loginConfig = (Map<String, Object>) config.get("password-auth");

        revokePunishmentHandler = new RevokePunishmentHandler(commandsExecution);
        codeGenerator = new CodeGenerator();
        codeVerificationHandler = new CodeVerificationHandler(logger, config, codeGenerator, databaseUtils);
        registerHandler = new RegisterHandler(logger, config, databaseUtils, eventDispatcher);
        loginHandler = new LoginHandler(logger, config, databaseUtils, eventDispatcher);
        discordOAuthHandler = new DiscordOAuthHandler(oauthConfig, databaseUtils, forbiddenError, eventDispatcher, logger);
        newPunishmentHandler = new NewPunishmentHandler(config, commandsExecution, databaseUtils);

        authHandler = new AuthenticationHandler(
                oauthConfig,
                loginConfig,
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
        String lang = (String) config.get("language");

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
    }

    public void initializeAPI(EventDispatcher eventDispatcher) {
        api = FlexBansAPI.getInstance();
    }

    public void initializeDatabase(Map<String, Object> config) throws SQLException {
        @SuppressWarnings("unchecked")
        Map<String, Object> databaseConfig = (Map<String, Object>) config.get("database");

        String type = (String) databaseConfig.getOrDefault("type", "h2");
        String host = (String) databaseConfig.getOrDefault("hostname", "localhost");
        Integer port = databaseConfig.containsKey("port") ? (Integer) databaseConfig.get("port") : 3306;
        String database = (String) databaseConfig.getOrDefault("database", "default_db");
        String username = (String) databaseConfig.getOrDefault("username", "root");
        String password = (String) databaseConfig.getOrDefault("password", "");

        databaseUtils = new DatabaseUtils("./plugins/FlexBans", type, host, port, database, username, password, logger);
        databaseUtils.initialize();
    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler(config, logger);
        ResourceHandler resourceHandler = new ResourceHandler();
        DomainFilter domainFilter = new DomainFilter(config, logger);
        HomeHandler homeHandler = new HomeHandler(config);

        jettyReloader = new JettyReloader(server, logger);

        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"home.html"});
        resourceHandler.setResourceBase(getClass().getClassLoader().getResource("web").toExternalForm());

        InternalServerError errorHandler = new InternalServerError(this.config, this.logger);
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
        ;
        logger.info(yellow + "Running on platform: " + lightYellow + platform + " " + version + reset);
        logger.info(yellow + "=======================================================================" + reset);
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public Map<String, Object> getWebhooksConfig() {
        return webhooksConfig;
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
}
