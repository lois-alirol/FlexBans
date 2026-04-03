package fr.neocle.flexbans;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.command.punishment.BanExecutorImpl;
import fr.neocle.flexbans.api.platform.handler.BanPlatformHandler;
import fr.neocle.flexbans.command.punishment.KickExecutorImpl;
import fr.neocle.flexbans.api.platform.handler.KickPlatformHandler;
import fr.neocle.flexbans.command.punishment.MuteExecutorImpl;
import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.command.punishment.UnbanExecutorImpl;
import fr.neocle.flexbans.command.punishment.UnmuteExecutorImpl;
import fr.neocle.flexbans.command.punishment.WarningExecutorImpl;
import fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler;
import fr.neocle.flexbans.command.server.ServerLockExecutorImpl;
import fr.neocle.flexbans.api.platform.handler.ServerLockPlatformHandler;
import fr.neocle.flexbans.command.server.ServerUnlockExecutorImpl;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.internal.LicenseChecker;
import fr.neocle.flexbans.internal.UpdateChecker;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.logger.JettyLoggerBridge;
import fr.neocle.flexbans.util.loader.FaviconConverter;
import fr.neocle.flexbans.util.loader.JettyReloader;
import fr.neocle.flexbans.util.loader.PublicFolderLoader;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.ApiServlet;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.socket.VerifyWebSocketServlet;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Logger;

public class Bootstrap {
    protected Path dataFolder;
    protected Logger logger;
    protected PlatformHandlerFactory platformHandlerFactory;
    protected DatabaseUtils databaseUtils;
    protected EventDispatcher eventDispatcher;
    protected FlexBansAPI api;
    protected JettyReloader jettyReloader;
    protected PlayerHeadImage playerHeadImage;
    protected BanExecutorImpl banExecutorImpl;
    protected MuteExecutorImpl muteExecutorImpl;
    protected KickExecutorImpl kickExecutorImpl;
    protected WarningExecutorImpl warningExecutorImpl;
    protected UnbanExecutorImpl unbanExecutorImpl;
    protected UnmuteExecutorImpl unmuteExecutorImpl;
    protected ServerLockExecutorImpl serverLockExecutorImpl;
    protected ServerUnlockExecutorImpl serverUnlockExecutorImpl;
    protected BanPlatformHandler banPlatformHandler;
    protected MutePlatformHandler mutePlatformHandler;
    protected KickPlatformHandler kickPlatformHandler;
    protected WarningPlatformHandler warningPlatformHandler;
    protected ServerLockPlatformHandler serverLockHandler;
    protected Broadcaster broadcaster;

    private Server webServer;
    private ApiServlet apiServlet;

    private static final FlexLogger LOGGER = FlexLogger.get(Bootstrap.class);

    public void initialize(Path dataFolder, Logger logger,
                           EventDispatcher eventDispatcher,
                           DatabaseUtils databaseUtils,
                           PlatformHandlerFactory platformHandlerFactory) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.platformHandlerFactory = platformHandlerFactory;

        FlexLogger.init(logger);

        try {
            initializeHandlers();
            initializeCommands();
            initializeAPI();
            initializeLanguage();

            File publicFolder = dataFolder.resolve("public").toFile();
            PublicFolderLoader.extractPublicFolder(publicFolder);

            new TokenManager(dataFolder);
            new UpdateChecker(getVersion()).start();
        } catch (URISyntaxException e) {
            logger.severe("Error setting up FlexBans: " + e.getMessage());
        }
    }

    private void initializeHandlers() throws URISyntaxException {
        UuidUsernameResolver.initialize(databaseUtils.getProfilesManager());

        playerHeadImage = new PlayerHeadImage(dataFolder.toFile());

        broadcaster = platformHandlerFactory.createBroadcaster();
        banPlatformHandler = platformHandlerFactory.createBanHandler();
        mutePlatformHandler = platformHandlerFactory.createMuteHandler();
        kickPlatformHandler = platformHandlerFactory.createKickHandler();
        warningPlatformHandler = platformHandlerFactory.createWarningHandler();
        serverLockHandler = platformHandlerFactory.createServerLockHandler();
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
        banExecutorImpl = new BanExecutorImpl(banPlatformHandler, broadcaster, databaseUtils, eventDispatcher);
        muteExecutorImpl = new MuteExecutorImpl(mutePlatformHandler, broadcaster, databaseUtils, eventDispatcher);
        kickExecutorImpl = new KickExecutorImpl(kickPlatformHandler, broadcaster, databaseUtils, eventDispatcher);
        warningExecutorImpl = new WarningExecutorImpl(warningPlatformHandler, broadcaster, databaseUtils, eventDispatcher);

        unbanExecutorImpl = new UnbanExecutorImpl(broadcaster, databaseUtils, eventDispatcher);
        unmuteExecutorImpl = new UnmuteExecutorImpl(broadcaster, databaseUtils, eventDispatcher);

        serverLockExecutorImpl = new ServerLockExecutorImpl(serverLockHandler, broadcaster, databaseUtils, eventDispatcher);
        serverUnlockExecutorImpl = new ServerUnlockExecutorImpl(broadcaster, databaseUtils, eventDispatcher);
    }

    public void initializeAPI() {
        api = FlexBansAPI.getInstance();

        FlexBansAPIImpl.setBanExecutor(banExecutorImpl);
        FlexBansAPIImpl.setMuteExecutor(muteExecutorImpl);
        FlexBansAPIImpl.setKickExecutor(kickExecutorImpl);
        FlexBansAPIImpl.setWarningExecutor(warningExecutorImpl);

        FlexBansAPIImpl.setUnbanExecutor(unbanExecutorImpl);
        FlexBansAPIImpl.setUnmuteExecutor(unmuteExecutorImpl);

        FlexBansAPIImpl.setServerLockExecutor(serverLockExecutorImpl);
        FlexBansAPIImpl.setServerUnlockExecutor(serverUnlockExecutorImpl);

    }

    private void createFavicon() throws Exception {
        String faviconUrl = ConfigManager.getString("server-display.favicon");

        boolean isValidUrl;
        try {
            URI uri = new URI(faviconUrl);
            isValidUrl = uri.getScheme() != null && uri.getHost() != null;
        } catch (Exception e) {
            isValidUrl = false;
        }

        if (!isValidUrl) {
            return;
        }

        FaviconConverter.convertUrlToFavicon(
                faviconUrl, dataFolder.resolve("public").toFile()
        );
    }

    public void startWebServer(int port) {
        Log.setLog(new JettyLoggerBridge("Web"));
        Thread.currentThread().setContextClassLoader(Bootstrap.class.getClassLoader());

        webServer = new Server(port);

        File publicFolder = dataFolder.resolve("public").toFile();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        apiServlet = new ApiServlet(databaseUtils, playerHeadImage, dataFolder);
        context.addServlet(new ServletHolder(apiServlet), "/api/*");
        context.addServlet(new ServletHolder(new VerifyWebSocketServlet()), "/api/ws/verify");

        Resource publicRes = Resource.newResource(publicFolder);
        Resource webRes = Resource.newResource(Bootstrap.class.getClassLoader().getResource("web"));
        context.setBaseResource(new ResourceCollection(publicRes, webRes));

        ServletHolder staticFiles = new ServletHolder("default", DefaultServlet.class);
        staticFiles.setInitParameter("dirAllowed", "false");
        context.addServlet(staticFiles, "/");

        RewriteHandler rewrite = new RewriteHandler();
        rewrite.setRewriteRequestURI(true);
        rewrite.setRewritePathInfo(false);

        RewriteRegexRule spaRule = new RewriteRegexRule();
        spaRule.setRegex("^/(?!api/|api$|.*\\..*).*$");
        spaRule.setReplacement("/index.html");
        rewrite.addRule(spaRule);

        rewrite.setHandler(context);
        webServer.setHandler(rewrite);

        try {
            createFavicon();
            webServer.start();
            new Thread(() -> {
                try {
                    webServer.join();
                } catch (InterruptedException e) {
                    logger.severe("Server thread interrupted: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error("Error starting web server: ", e);
        }
    }

    public void logServerStartupInfo(String address, int port, String platform, String version, boolean isWebServerEnabled) {
        String yellow = "\u001B[38;5;214m";
        String lightYellow = "\u001B[38;5;228m";
        String reset = "\u001B[0m";

        String buyerId = LicenseChecker.getDiscordId(ConfigManager.getString("license-key"));
        String buyerName = null;

        logger.info(yellow + "----------===============☰☰☰☰☰☰☰☰☰☰☰===============----------" + reset);
        logger.info(yellow + "            ___ _             ___                          " + reset);
        logger.info(yellow + "           / __\\ | _____  __ / __\\ __ _ _ __  ___          " + reset);
        logger.info(yellow + "          / _\\ | |/ _ \\ \\/ //__\\/// _` | '_ \\/ __|         " + reset);
        logger.info(yellow + "         / /   | |  __/>  </ \\/  \\ (_| | | | \\__ \\         " + reset);
        logger.info(yellow + "         \\/    |_|\\___/_/\\_\\_____/\\__,_|_| |_|___/         " + reset);
        logger.info(" ");

        if (isWebServerEnabled) {
            logger.info(yellow + " > Dashboard listening to port: " + lightYellow + port + reset);
            logger.info(yellow + " > Public dashboard address: " + lightYellow + address + reset);
        }

        logger.info(yellow + " > Platform: " + lightYellow + platform + " " + version + reset);
        logger.info(yellow + " > Developer: " + lightYellow + "Neocle" + reset);
        logger.info(yellow + " > Licensed to: " + lightYellow + "@" + buyerName + reset);
        logger.info(yellow + "----------===============☰☰☰☰☰☰☰☰☰☰☰===============----------" + reset);
    }

    public boolean shutdown() {
        try {
            if (webServer != null && webServer.isRunning()) {
                webServer.stop();
                webServer.join();
                webServer = null;
                LOGGER.info("Web server stopped successfully.");
            }

            TaskScheduler.get().shutdown();

            return true;
        } catch (Exception e) {
            LOGGER.error("Error while stopping web server or schedulers: ", e);
            return false;
        }
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public Logger getLogger() {
        return logger;
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

    public BanExecutorImpl getBanExecutor() {
        return banExecutorImpl;
    }

    public MuteExecutorImpl getMuteExecutor() {
        return muteExecutorImpl;
    }

    public KickExecutorImpl getKickExecutor() {
        return kickExecutorImpl;
    }

    public WarningExecutorImpl getWarningExecutor() { return warningExecutorImpl; }

    public UnbanExecutorImpl getUnbanExecutor() {
        return unbanExecutorImpl;
    }

    public UnmuteExecutorImpl getUnmuteExecutor() { return unmuteExecutorImpl; }

    public ServerLockExecutorImpl getServerLockExecutor() {
        return serverLockExecutorImpl;
    }

    public ServerUnlockExecutorImpl getServerUnlockExecutor() {
        return serverUnlockExecutorImpl;
    }

    public String getVersion() {
        return "0.9.9-SNAPSHOT";
    }
}
