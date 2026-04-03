package fr.neocle.flexbans.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.web.async.AsyncRequestHandler;
import fr.neocle.flexbans.web.handler.*;
import fr.neocle.flexbans.web.provider.PunishmentDataProvider;
import fr.neocle.flexbans.web.provider.ServerConfigProvider;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.routing.ApiRouter;
import fr.neocle.flexbans.web.security.CsrfValidator;
import fr.neocle.flexbans.web.security.SecurityHeadersManager;
import fr.neocle.flexbans.web.security.SessionManager;
import fr.neocle.flexbans.web.util.RequestUtils;
import fr.neocle.flexbans.web.util.ResponseUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;

@WebServlet(asyncSupported = true)
public class ApiServlet extends HttpServlet {
    private final Gson gson;
    private final ApiRouter router;
    private final SecurityHeadersManager securityManager;
    private final CsrfValidator csrfValidator;
    private final SessionManager sessionManager;

    private static final FlexLogger LOGGER = FlexLogger.get(ApiServlet.class);

    public ApiServlet(
            DatabaseUtils databaseUtils,
            PlayerHeadImage playerHeadImage,
            Path dataFolder
    ) {
        this.gson = new Gson();

        PunishmentDataProvider punishmentDataProvider = PunishmentDataProvider.getInstance(databaseUtils);
        punishmentDataProvider.initialize();
        ServerConfigProvider serverConfigProvider = ServerConfigProvider.getInstance();

        this.securityManager = new SecurityHeadersManager();
        this.sessionManager = new SessionManager();
        this.csrfValidator = new CsrfValidator(sessionManager);

        AuthHandler authHandler = new AuthHandler(databaseUtils);
        PunishmentsHandler punishmentsHandler = new PunishmentsHandler(punishmentDataProvider);
        ConfigHandler configHandler = new ConfigHandler(serverConfigProvider);
        PlayerResourceHandler playerResourceHandler = new PlayerResourceHandler(playerHeadImage, dataFolder);
        UsersHandler usersHandler = new UsersHandler(databaseUtils.getUserManager());
        CsrfHandler csrfHandler = new CsrfHandler(sessionManager);

        PunishmentEditionHandler punishmentEditionHandler = new PunishmentEditionHandler(databaseUtils);
        PunishmentCreationHandler punishmentCreationHandler = new PunishmentCreationHandler();
        PunishmentRevocationHandler punishmentRevocationHandler = new PunishmentRevocationHandler();

        AsyncRequestHandler asyncHandler = new AsyncRequestHandler(securityManager, gson);

        this.router = new ApiRouter(
                authHandler,
                punishmentsHandler,
                configHandler,
                playerResourceHandler,
                usersHandler,
                csrfHandler,
                punishmentCreationHandler,
                punishmentEditionHandler,
                punishmentRevocationHandler,
                asyncHandler
        );

        LOGGER.info("Initialized successfully");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        securityManager.setHeaders(resp, req);

        if (!csrfValidator.verify(req)) {
            ResponseUtils.sendJson(resp, ApiResponse.forbidden("CSRF token invalid or missing"), gson);
            return;
        }

        String path = req.getPathInfo();
        JsonObject jsonBody;

        try {
            jsonBody = RequestUtils.parseJsonBody(req);
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendJson(resp, ApiResponse.badRequest(e.getMessage()), gson);
            return;
        }

        ApiResponse<?> response = router.routePost(path, jsonBody, req, resp);
        if (response != null) {
            ResponseUtils.sendJson(resp, response, gson);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        securityManager.setHeaders(resp, req);

        String path = req.getPathInfo();
        ApiResponse<?> response = router.routeGet(path, req, resp);

        if (response != null) {
            ResponseUtils.sendJson(resp, response, gson);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
            return;
        }

        super.service(req, resp);
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        securityManager.setHeaders(resp, req);

        if (!csrfValidator.verify(req)) {
            ResponseUtils.sendJson(resp, ApiResponse.forbidden("CSRF token invalid or missing"), gson);
            return;
        }

        String path = req.getPathInfo();

        router.routePatch(path, req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        securityManager.setHeaders(resp, req);
        resp.setStatus(200);
    }
}