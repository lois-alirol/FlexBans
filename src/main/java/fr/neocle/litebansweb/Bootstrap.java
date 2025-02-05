package fr.neocle.litebansweb;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.PlayerHistoryHandler;
import fr.neocle.litebansweb.handlers.ModeratorHistoryHandler;
import fr.neocle.litebansweb.handlers.PunishmentDetailsHandler;
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
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecutionSpigot;
import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecutionVelocity;
import fr.neocle.litebansweb.utils.Player.PlayerHeadImage;
import fr.neocle.litebansweb.utils.Player.UsernameUUIDConverters;
import fr.neocle.litebansweb.utils.Security.CodeGenerator;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.session.SessionHandler;
import com.velocitypowered.api.proxy.ProxyServer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.logging.Logger;

public class Bootstrap {
    protected Path dataFolder;
    protected Map<String, Object> config;
    protected Logger logger;
    protected AuthenticationHandler authHandler;
    protected IndexHandler indexHandler;
    protected PlayerHistoryHandler playerHistoryHandler;
    protected ModeratorHistoryHandler moderatorHistoryHandler;
    protected PunishmentDetailsHandler punishmentDetailsHandler;
    protected RevokePunishmentHandler revokePunishmentHandler;
    protected NewPunishmentHandler newPunishmentHandler;
    protected PlayerHeadHandler playerHeadHandler;
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

    public void initialize(Path dataFolder, Logger logger, String platform, ProxyServer proxyServer) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.pluginFolder = new File("plugins/LitebansWeb");
        this.platform = platform;
        this.proxyServer = proxyServer;

        try {
            databaseUtils = new DatabaseUtils("./plugins/LitebansWeb", logger);
            databaseUtils.initializeConnection();

            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
                logger.info("Created LitebansWeb directory.");
            }

            Path configFilePath = dataFolder.resolve("config.yml");
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

            config = ResourceLoader.loadConfig(dataFolder, logger);
            if (config == null) {
                logger.severe("Failed to load the config. Web server not started.");
                return;
            }
            
            initializeHandlers(platform);
        } catch (IOException e) {
            logger.severe("Error setting up LitebansWeb: " + e.getMessage());
        }
    }

    private void initializeHandlers(String platform) {
        UsernameUUIDConverters usernameUUIDConverters = new UsernameUUIDConverters();
        PlayerHeadImage playerHeadImage = new PlayerHeadImage(usernameUUIDConverters, pluginFolder);
        DurationCalculator durationCalculator = new DurationCalculator();

        indexHandler = new IndexHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        playerHistoryHandler = new PlayerHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        moderatorHistoryHandler = new ModeratorHistoryHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage);
        punishmentDetailsHandler = new PunishmentDetailsHandler(config, usernameUUIDConverters, durationCalculator, playerHeadImage, databaseUtils);
        playerHeadHandler = new PlayerHeadHandler(dataFolder);

        CommandsExecution commandsExecution;

        if (platform.equalsIgnoreCase("bungee")) {
            commandsExecution = new CommandsExecutionBungee();
        } else if (platform.equalsIgnoreCase("spigot")) {
            commandsExecution = new CommandsExecutionSpigot();
        } else if (platform.equalsIgnoreCase("velocity")) {
            commandsExecution = new CommandsExecutionVelocity(proxyServer);
        } else {
            logger.severe("Unsupported platform: " + platform);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> oauthConfig = (Map<String, Object>) config.get("discord_oauth");

        @SuppressWarnings("unchecked")
        Map<String, Object> loginConfig = (Map<String, Object>) config.get("password_login");

        revokePunishmentHandler = new RevokePunishmentHandler(commandsExecution);
        forbiddenError = new ForbiddenError(config, logger);
        notFoundError = new NotFoundError(config, logger);
        codeGenerator = new CodeGenerator();
        codeVerificationHandler = new CodeVerificationHandler(logger, config, codeGenerator, databaseUtils);
        registerHandler = new RegisterHandler(logger, config, databaseUtils);
        loginHandler = new LoginHandler(logger, config,  databaseUtils);
        discordOAuthHandler = new DiscordOAuthHandler(oauthConfig, databaseUtils);

        authHandler = new AuthenticationHandler(
            oauthConfig,
            loginConfig,
            indexHandler,
            playerHistoryHandler,
            moderatorHistoryHandler,
            punishmentDetailsHandler,
            playerHeadHandler,
            revokePunishmentHandler,
            null,
            forbiddenError,
            notFoundError,
            databaseUtils,
            codeVerificationHandler,
            registerHandler,
            loginHandler,
            discordOAuthHandler
        );
        
        newPunishmentHandler = new NewPunishmentHandler(config, commandsExecution, databaseUtils);
        authHandler.setNewPunishmentHandler(newPunishmentHandler);
        
    }

    public void startWebServer(int port) {
        Server server = new Server(port);

        AbstractHandler securityHandler = new HttpsEnforcementHandler(config, logger);
        ResourceHandler resourceHandler = new ResourceHandler();
        DomainFilter domainFilter = new DomainFilter(config, logger);
        
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"home.html"});
        resourceHandler.setResourceBase(getClass().getClassLoader().getResource("web").toExternalForm());

        InternalServerError errorHandler = new InternalServerError(this.config, this.logger);
        server.setErrorHandler(errorHandler);

        AbstractHandler homeHandler = new AbstractHandler() {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                if ("/home.html".equals(target) || "/".equals(target)) {
                    String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/home.html");
                    if (htmlTemplate == null) {
                        response.getWriter().write("Error: Unable to load HTML template.");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server_display_settings");
                    String pageContent = htmlTemplate
                            .replace("{{server_name}}", String.valueOf(serverDisplaySettings.getOrDefault("name", "Example")))
                            .replace("{{server_description}}", String.valueOf(serverDisplaySettings.getOrDefault("description", "ExampleServer")))
                            .replace("{{server_icon}}", String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png")))
                            .replace("{{server_favicon}}", String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png")))
                            .replace("{{server_color}}", String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7")))
                            .replace("{{server_color_hover}}", String.valueOf(serverDisplaySettings.getOrDefault("darker_color", "#207dd2")))
                            .replace("{{server_logo}}", String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png")));

                    response.setContentType("text/html;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(pageContent);
                    baseRequest.setHandled(true);
                }
            }
        };

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

    public void logServerStartupInfo(int port, String platform, String version) {
        String yellow = "\u001B[33m";
        String reset = "\u001B[0m";

        System.out.println(yellow + "    __    _ __       __                    _       __     __  " + reset);
        System.out.println(yellow + "   / /   (_) /____  / /_  ____ _____  ____| |     / /__  / /_ " + reset);
        System.out.println(yellow + "  / /   / / __/ _ \\/ __ \\/ __ `/ __ \\/ ___/ | /| / / _ \\/ __ \\" + reset);
        System.out.println(yellow + " / /___/ / /_/  __/ /_/ / /_/ / / / (__  )| |/ |/ /  __/ /_/ /" + reset);
        System.out.println(yellow + "/_____/_/\\__/\\___/_.___/\\__,_/_/ /_/____/ |__/|__/\\___/_.___/ " + reset);
        System.out.println(yellow + "=======================================================================" + reset);
        System.out.println(yellow + "LitebansWeb is configured to run on port " + port + reset);
        System.out.println(yellow + "Running on platform: " + platform + " " + version + reset);
        System.out.println(yellow + "=======================================================================" + reset);
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public Map<String, Object> getConfig() {
        return config;
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

}
