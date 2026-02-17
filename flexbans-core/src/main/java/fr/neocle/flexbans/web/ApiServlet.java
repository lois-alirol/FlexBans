package fr.neocle.flexbans.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
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

@WebServlet(asyncSupported = true)
public class ApiServlet extends HttpServlet {
    private final Gson gson;
    private final ApiRouter router;
    private final SecurityHeadersManager securityManager;
    private final CsrfValidator csrfValidator;
    private final SessionManager sessionManager;
    private final AuthHandler authHandler;
    private final PunishmentsHandler punishmentsHandler;
    private final PunishmentCreationHandler punishmentCreationHandler;
    private final PunishmentEditionHandler punishmentEditionHandler;
    private final PunishmentRevocationHandler punishmentRevocationHandler;

    public ApiServlet(DatabaseUtils databaseUtils, PlayerHeadImage playerHeadImage,
                      CommandsExecution commandsExecution) {
        this.gson = new Gson();

        PunishmentDataProvider punishmentDataProvider = PunishmentDataProvider.getInstance(databaseUtils);
        punishmentDataProvider.initialize();
        ServerConfigProvider serverConfigProvider = ServerConfigProvider.getInstance();

        this.securityManager = new SecurityHeadersManager();
        this.sessionManager = new SessionManager();
        this.csrfValidator = new CsrfValidator(sessionManager);
        this.authHandler = new AuthHandler(databaseUtils);
        this.punishmentsHandler = new PunishmentsHandler(punishmentDataProvider);

        ConfigHandler configHandler = new ConfigHandler(serverConfigProvider);
        PlayerResourceHandler playerResourceHandler = new PlayerResourceHandler(playerHeadImage);
        UsersHandler usersHandler = new UsersHandler(databaseUtils.getUserManager());
        CsrfHandler csrfHandler = new CsrfHandler(sessionManager);

        this.punishmentEditionHandler = new PunishmentEditionHandler(databaseUtils);
        this.punishmentCreationHandler = new PunishmentCreationHandler();
        this.punishmentRevocationHandler = new PunishmentRevocationHandler();

        AsyncRequestHandler asyncHandler = new AsyncRequestHandler(securityManager, gson);

        this.router = new ApiRouter(
                authHandler,
                punishmentsHandler,
                configHandler,
                playerResourceHandler,
                usersHandler,
                csrfHandler,
                punishmentCreationHandler,
                asyncHandler
        );

        FlexLogger.info("API Servlet initialized successfully");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FlexLogger.log("REQUESTING POST AT : " + req.getPathInfo());
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

        ApiResponse<?> response = handleSyncPostRoutes(path, req, jsonBody);
        if (response != null) {
            ResponseUtils.sendJson(resp, response, gson);
            return;
        }

        router.routePost(path, jsonBody, req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FlexLogger.log("REQUESTING GET AT : " + req.getPathInfo());

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
            FlexLogger.log("REQUESTING PATCH AT : " + req.getPathInfo());
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

    private ApiResponse<?> handleSyncPostRoutes(String path, HttpServletRequest req, JsonObject jsonBody) {
        if (path == null) {
            return ApiResponse.notFound("Endpoint not found");
        }

        String token = RequestUtils.extractBearerToken(req);

        switch (path) {
            case "/auth/2fa/request":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return authHandler.handleRequest2FA(token, null);

            case "/auth/logout":
                return authHandler.handleLogout(token, req, null);

            case "/auth/refresh":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing refresh token");
                }
                return authHandler.handleRefresh(token, null);

            case "/punishments/revoke":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return punishmentRevocationHandler.revokePunishment(req, jsonBody);

            case "/punishments/edit":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return punishmentEditionHandler.editPunishment(req, jsonBody);

            case "/punishments/create":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return punishmentCreationHandler.createPunishment(req, jsonBody);

            default:
                return null;
        }
    }

    private ApiResponse<?> handleSyncPatchRoutes(String path, HttpServletRequest req, JsonObject jsonBody) {
        if (path == null) {
            return ApiResponse.notFound("Endpoint not found");
        }

        String token = RequestUtils.extractBearerToken(req);

        switch (path) {
            default:
                return null;
        }
    }
}