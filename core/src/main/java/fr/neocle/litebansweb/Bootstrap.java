package fr.neocle.litebansweb;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.handlers.HomeHandler;
import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.PlayerHistoryHandler;
import fr.neocle.litebansweb.handlers.ModeratorHistoryHandler;
import fr.neocle.litebansweb.handlers.PunishmentDetailsHandler;
import fr.neocle.litebansweb.handlers.ScriptsHandler;
import fr.neocle.litebansweb.handlers.PlayerHeadHandler;
import fr.neocle.litebansweb.handlers.Errors.ForbiddenError;
import fr.neocle.litebansweb.handlers.Errors.InternalServerError;
import fr.neocle.litebansweb.handlers.Errors.NotFoundError;
import fr.neocle.litebansweb.handlers.PostRequestHandlers.NewPunishmentHandler;
import fr.neocle.litebansweb.handlers.PostRequestHandlers.RevokePunishmentHandler;
import fr.neocle.litebansweb.handlers.Security.CodeVerificationHandler;
import fr.neocle.litebansweb.handlers.Security.LoginHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.handlers.Security.RegisterHandler;
import fr.neocle.litebansweb.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.litebansweb.handlers.Security.Utils.DomainFilter;
import fr.neocle.litebansweb.handlers.Security.Utils.HttpsEnforcementHandler;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.utils.ResourceLoader;
import fr.neocle.litebansweb.utils.DurationCalculator;
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecution;
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecutionBungee;
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecutionBukkit;
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecutionVelocity;
import fr.neocle.litebansweb.utils.Player.PlayerHeadImage;
import fr.neocle.litebansweb.utils.Player.UsernameUUIDConverters;
import fr.neocle.litebansweb.utils.Security.CodeGenerator;
import fr.neocle.litebansweb.locale.LanguageManager;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.session.SessionHandler;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
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
    protected ProxyServer proxyServer;
    protected DatabaseUtils databaseUtils;
    protected CodeVerificationHandler codeVerificationHandler;
    protected CodeGenerator codeGenerator;
    protected LoginHandler loginHandler;
    protected RegisterHandler registerHandler;
    protected DiscordOAuthHandler discordOAuthHandler;
    protected EventDispatcher eventDispatcher;
    protected LitebansWebAPI api;

    public void initialize(Path dataFolder, Logger logger, String platform, ProxyServer proxyServer, EventDispatcher eventDispatcher) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.pluginFolder = new File("plugins/LitebansWeb");
        this.platform = platform;
        this.proxyServer = proxyServer;
        this.eventDispatcher = eventDispatcher;
        
        try {
            databaseUtils = new DatabaseUtils("./plugins/LitebansWeb", logger);
            databaseUtils.initializeConnection();

            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
                logger.info("Created LitebansWeb directory.");
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

            initializeHandlers(platform);
            initializeAPI(eventDispatcher);
            initializeLanguage();
        } catch (IOException | URISyntaxException e) {
            logger.severe("Error setting up LitebansWeb: " + e.getMessage());
        }
    }

    private void initializeHandlers(String platform) throws URISyntaxException {
        forbiddenError = new ForbiddenError(config, logger);
        notFoundError = new NotFoundError(config, logger);

        UsernameUUIDConverters usernameUUIDConverters = new UsernameUUIDConverters();
        PlayerHeadImage playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);
        DurationCalculator durationCalculator = new DurationCalculator();

        indexHandler = new IndexHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        playerHistoryHandler = new PlayerHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        moderatorHistoryHandler = new ModeratorHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        punishmentDetailsHandler = new PunishmentDetailsHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage, databaseUtils);
        playerHeadHandler = new PlayerHeadHandler(dataFolder, notFoundError);

        scriptsHandler = new ScriptsHandler(notFoundError);

        CommandsExecution commandsExecution;

        if (platform.equalsIgnoreCase("bungee")) {
            commandsExecution = new CommandsExecutionBungee();
        } else if (platform.equalsIgnoreCase("spigot")) {
            commandsExecution = new CommandsExecutionBukkit();
        } else if (platform.equalsIgnoreCase("velocity")) {
            commandsExecution = new CommandsExecutionVelocity(proxyServer);
        } else {
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
        loginHandler = new LoginHandler(logger, config,  databaseUtils, eventDispatcher);
        discordOAuthHandler = new DiscordOAuthHandler(oauthConfig, databaseUtils, eventDispatcher);
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
            eventDispatcher
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

    public void initializeAPI(EventDispatcher eventDispatcher) {
        api = LitebansWebAPI.getInstance();
    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler(config, logger);
        ResourceHandler resourceHandler = new ResourceHandler();
        DomainFilter domainFilter = new DomainFilter(config, logger);
        HomeHandler homeHandler = new HomeHandler(config);

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
        sessionHandler.getSessionCookieConfig().setSecure(true);
        server.setHandler(sessionHandler);

        try {
            server.start();
            logger.info("Web server started on port " + port);
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

    public void logServerStartupInfo(String address, int port, String platform, String version) {
        String yellow = "\u001B[38;5;214m";
        String lightYellow = "\u001B[38;5;228m";
        String reset = "\u001B[0m";

        logger.info(yellow + "    __    _ __       __                    _       __     __  " + reset);
        logger.info(yellow + "   / /   (_) /____  / /_  ____ _____  ____| |     / /__  / /_ " + reset);
        logger.info(yellow + "  / /   / / __/ _ \\/ __ \\/ __ `/ __ \\/ ___/ | /| / / _ \\/ __ \\" + reset);
        logger.info(yellow + " / /___/ / /_/  __/ /_/ / /_/ / / / (__  )| |/ |/ /  __/ /_/ /" + reset);
        logger.info(yellow + "/_____/_/\\__/\\___/_.___/\\__,_/_/ /_/____/ |__/|__/\\___/_.___/ " + reset);
        logger.info(yellow + "=======================================================================" + reset);
        logger.info(yellow + "Webserver is running on " + lightYellow + address + ":" + port + reset);
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

    public IndexHandler getIndexHandler() {
        return indexHandler;
    }

    public LitebansWebAPI getAPI() {
        return api;
    }
}
