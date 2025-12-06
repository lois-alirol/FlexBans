package fr.neocle.flexbans;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.command.punishment.ban.BanExecutor;
import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.command.punishment.kick.KickExecutor;
import fr.neocle.flexbans.command.punishment.kick.KickPlatformHandler;
import fr.neocle.flexbans.command.punishment.mute.MuteExecutor;
import fr.neocle.flexbans.command.punishment.mute.MutePlatformHandler;
import fr.neocle.flexbans.command.punishment.unban.UnbanExecutor;
import fr.neocle.flexbans.command.punishment.unmute.UnmuteExecutor;
import fr.neocle.flexbans.command.punishment.warning.WarningExecutor;
import fr.neocle.flexbans.command.punishment.warning.WarningPlatformHandler;
import fr.neocle.flexbans.command.server.lock.ServerLockExecutor;
import fr.neocle.flexbans.command.server.lock.ServerLockPlatformHandler;
import fr.neocle.flexbans.command.server.unlock.ServerUnlockExecutor;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.handler.web.*;
import fr.neocle.flexbans.handler.web.api.PunishmentSSEHandler;
import fr.neocle.flexbans.handler.web.error.ForbiddenError;
import fr.neocle.flexbans.handler.web.error.InternalServerError;
import fr.neocle.flexbans.handler.web.error.NotFoundError;
import fr.neocle.flexbans.handler.web.post.NewPunishmentHandler;
import fr.neocle.flexbans.handler.web.post.RevokePunishmentHandler;
import fr.neocle.flexbans.handler.web.security.AuthenticationHandler;
import fr.neocle.flexbans.handler.web.security.CodeVerificationHandler;
import fr.neocle.flexbans.handler.web.security.LoginHandler;
import fr.neocle.flexbans.handler.web.security.RegisterHandler;
import fr.neocle.flexbans.handler.web.security.component.AuthComponents;
import fr.neocle.flexbans.handler.web.security.component.DatabaseComponents;
import fr.neocle.flexbans.handler.web.security.component.ErrorHandlers;
import fr.neocle.flexbans.handler.web.security.component.HandlerRegistry;
import fr.neocle.flexbans.handler.web.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.handler.web.security.util.DomainFilter;
import fr.neocle.flexbans.handler.web.security.util.HttpsEnforcementHandler;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.internal.UpdateChecker;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.ImagesLoader;
import fr.neocle.flexbans.util.JettyReloader;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UsernameUUIDConverters;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bootstrap {
    protected Path dataFolder;
    protected Logger logger;
    protected PlatformHandlerFactory platformHandlerFactory;
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
    protected Broadcaster broadcaster;
    protected UsernameUUIDConverters usernameUUIDConverters;
    protected PunishmentSSEHandler punishmentSSEHandler;

    public void initialize(Path dataFolder, Logger logger,
                           EventDispatcher eventDispatcher,
                           DatabaseUtils databaseUtils,
                           PlatformHandlerFactory platformHandlerFactory) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.pluginFolder = new File("plugins/FlexBans");
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.platformHandlerFactory = platformHandlerFactory;

        FlexLogger.init(logger);

        try {
            initializeHandlers();
            initializeAPI();
            initializeCommands();
            initializeLanguage();

            ImagesLoader.extractImagesFromJar(new File(pluginFolder, "images"));

            new UpdateChecker(getVersion()).start();
        } catch (URISyntaxException e) {
            logger.severe("Error setting up FlexBans: " + e.getMessage());
        }
    }

    private void initializeHandlers() throws URISyntaxException {
        forbiddenError = new ForbiddenError();
        notFoundError = new NotFoundError();

        usernameUUIDConverters = new UsernameUUIDConverters();
        playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);

        boolean playerHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.player.enabled");
        boolean moderatorHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.moderator.enabled");
        boolean punishmentDetailsEnabled = ConfigManager.getBoolean("webserver.pages.details.punishment.enabled");

        indexHandler = new IndexHandler(usernameUUIDConverters, playerHeadImage, databaseUtils);

        if (playerHistoryEnabled)
            playerHistoryHandler = new PlayerHistoryHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, notFoundError);
        if (moderatorHistoryEnabled)
            moderatorHistoryHandler = new ModeratorHistoryHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, notFoundError);
        if (punishmentDetailsEnabled)
            punishmentDetailsHandler = new PunishmentDetailsHandler(usernameUUIDConverters, playerHeadImage, databaseUtils, notFoundError);

        playerHeadHandler = new PlayerHeadHandler(dataFolder, notFoundError);
        javascriptHandler = new JavascriptHandler(notFoundError);
        cssHandler = new CssHandler(notFoundError);

        CommandsExecution commandsExecution = platformHandlerFactory.createCommandsExecution();
        broadcaster = platformHandlerFactory.createBroadcaster();
        banPlatformHandler = platformHandlerFactory.createBanHandler();
        mutePlatformHandler = platformHandlerFactory.createMuteHandler();
        kickPlatformHandler = platformHandlerFactory.createKickHandler();
        warningPlatformHandler = platformHandlerFactory. createWarningHandler();
        serverLockHandler = platformHandlerFactory.createServerLockHandler();

        revokePunishmentHandler = new RevokePunishmentHandler(commandsExecution);
        codeVerificationHandler = new CodeVerificationHandler(databaseUtils);
        registerHandler = new RegisterHandler(databaseUtils, eventDispatcher);
        loginHandler = new LoginHandler(databaseUtils, eventDispatcher);
        discordOAuthHandler = new DiscordOAuthHandler(databaseUtils, forbiddenError, eventDispatcher);
        newPunishmentHandler = new NewPunishmentHandler(commandsExecution, databaseUtils);
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
                .build();
    }

    public void initializeLanguage() {
        String lang = ConfigManager.getString("language");

        if ("locale".equalsIgnoreCase(lang)) {
            Locale defaultLocale = Locale.getDefault();
            lang = defaultLocale.getLanguage() + "_" + defaultLocale.getCountry();
        }

        logger.info("Loading language file: " + lang + "...");
        LanguageManager.initialize(dataFolder);
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

    public void initializeAPI() {
        api = FlexBansAPI.getInstance();

        FlexBansAPIImpl.setBanExecutor(banExecutor);
        FlexBansAPIImpl.setMuteExecutor(muteExecutor);
        FlexBansAPIImpl.setKickExecutor(kickExecutor);
        FlexBansAPIImpl.setWarningExecutor(warningExecutor);

        FlexBansAPIImpl.setUnbanExecutor(unbanExecutor);
        FlexBansAPIImpl.setUnmuteExecutor(unmuteExecutor);

        FlexBansAPIImpl.setServerLockExecutor(serverLockExecutor);
        FlexBansAPIImpl.setServerUnlockExecutor(serverUnlockExecutor);

    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler();
        ResourceHandler resourceHandler = new ResourceHandler();
        DomainFilter domainFilter = new DomainFilter();
        HomeHandler homeHandler = new HomeHandler();

        jettyReloader = new JettyReloader(server);

        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"home.html"});
        resourceHandler.setResourceBase(getClass().getClassLoader().getResource("web").toExternalForm());

        InternalServerError errorHandler = new InternalServerError();
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
