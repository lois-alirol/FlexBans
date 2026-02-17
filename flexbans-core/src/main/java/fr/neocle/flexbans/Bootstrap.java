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
import fr.neocle.flexbans.util.ImagesLoader;
import fr.neocle.flexbans.util.JettyReloader;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.ApiServlet;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
    protected File pluginFolder;
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
    protected CommandsExecution commandsExecution;

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
            initializeCommands();
            initializeAPI();
            initializeLanguage();

            ImagesLoader.extractImagesFromJar(new File(pluginFolder, "images"));

            new UpdateChecker(getVersion()).start();
        } catch (URISyntaxException e) {
            logger.severe("Error setting up FlexBans: " + e.getMessage());
        }
    }

    private void initializeHandlers() throws URISyntaxException {
        UuidUsernameResolver.initialize(databaseUtils.getProfilesManager());

        playerHeadImage = new PlayerHeadImage(pluginFolder);

        commandsExecution = platformHandlerFactory.createCommandsExecution();
        broadcaster = platformHandlerFactory.createBroadcaster();
        banPlatformHandler = platformHandlerFactory.createBanHandler();
        mutePlatformHandler = platformHandlerFactory.createMuteHandler();
        kickPlatformHandler = platformHandlerFactory.createKickHandler();
        warningPlatformHandler = platformHandlerFactory. createWarningHandler();
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

    public void startWebServer(int port) {
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        ApiServlet apiServlet = new ApiServlet(databaseUtils, playerHeadImage, commandsExecution);
        context.addServlet(new ServletHolder(apiServlet), "/api/*");

        context.setResourceBase(
                Bootstrap.class.getClassLoader().getResource("web").toExternalForm()
        );

        ServletHolder staticServlet = new ServletHolder("static", DefaultServlet.class);
        staticServlet.setInitParameter("dirAllowed", "false");
        context.addServlet(staticServlet, "/");

        RewriteHandler rewrite = new RewriteHandler();
        rewrite.setRewriteRequestURI(true);
        rewrite.setRewritePathInfo(false);

        RewriteRegexRule spaRule = new RewriteRegexRule();
        spaRule.setRegex("^/(?!api|static|.*\\..*).*$");
        spaRule.setReplacement("/index.html");
        rewrite.addRule(spaRule);

        rewrite.setHandler(context);

        server.setHandler(rewrite);

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
        String buyerName = null;

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
