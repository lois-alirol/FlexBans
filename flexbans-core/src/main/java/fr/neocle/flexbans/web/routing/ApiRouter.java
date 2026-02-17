package fr.neocle.flexbans.web.routing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.async.AsyncRequestHandler;
import fr.neocle.flexbans.web.handler.*;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestUtils;
import fr.neocle.flexbans.web.util.ResponseUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiRouter {
    private final AuthHandler authHandler;
    private final PunishmentsHandler punishmentsHandler;
    private final ConfigHandler configHandler;
    private final PlayerResourceHandler playerResourceHandler;
    private final UsersHandler usersHandler;
    private final CsrfHandler csrfHandler;
    private final PunishmentCreationHandler punishmentCreationHandler;
    private final AsyncRequestHandler asyncHandler;

    public ApiRouter(
            AuthHandler authHandler,
            PunishmentsHandler punishmentsHandler,
            ConfigHandler configHandler,
            PlayerResourceHandler playerResourceHandler,
            UsersHandler usersHandler,
            CsrfHandler csrfHandler,
            PunishmentCreationHandler punishmentCreationHandler,
            AsyncRequestHandler asyncHandler
    ) {
        this.authHandler = authHandler;
        this.punishmentsHandler = punishmentsHandler;
        this.configHandler = configHandler;
        this.playerResourceHandler = playerResourceHandler;
        this.usersHandler = usersHandler;
        this.csrfHandler = csrfHandler;
        this.punishmentCreationHandler = punishmentCreationHandler;
        this.asyncHandler = asyncHandler;
    }

    public void routePost(String path, JsonObject jsonBody, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (path == null) {
            return;
        }

        switch (path) {
            case "/auth/login":
                asyncHandler.handleAsync(req, resp,
                        authHandler.handleLogin(jsonBody, req, resp), "login");
                break;

            case "/auth/register":
                asyncHandler.handleAsync(req, resp,
                        authHandler.handleRegister(jsonBody, req, resp), "register");
                break;

            case "/auth/2fa/request":
                handleSync2FARequest(req, resp);
                break;

            case "/auth/verify-2fa":
                asyncHandler.handleAsync(req, resp,
                        authHandler.handleVerify2FA(jsonBody, req, resp), "verify-2fa");
                break;

            case "/auth/logout":
                handleSyncLogout(req, resp);
                break;

            case "/auth/refresh":
                handleSyncRefresh(req, resp);
                break;

            case "/punishments/revoke":
                handleSyncRevokePunishment(req, resp);
                break;

            case "/punishments/create":
                handleSyncCreatePunishment(req, resp, jsonBody);
                break;

            default:
        }
    }

    public ApiResponse<?> routeGet(String path, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (path == null) {
            return ApiResponse.notFound("Endpoint not found");
        }

        FlexLogger.info("GET request to path: " + path);

        switch (path) {
            case "/auth/csrf-token":
                return csrfHandler.getCsrfToken(req, resp);

            case "/auth/me":
                handleAsyncGetMe(req, resp);
                return null;
            case "/config":
                return configHandler.getConfig();

            case "/punishments":
                int page = RequestUtils.getPageParameter(req);
                String typeParam = req.getParameter("type");
                return punishmentsHandler.getPunishments(page, typeParam);

            case "/users":
                handleAsyncAllUsers(req, resp);
                return null;

            default:
                return handleDynamicRoutes(path, req, resp);
        }
    }

    public void routePatch(String path, HttpServletRequest req, HttpServletResponse resp) {
        if (path == null) {
            return;
        }

        if (path.startsWith("/user/") && path.endsWith("/permissions")) {
            String username = extractUsernameFromPath(path, "/user/", "/permissions");
            if (username.isEmpty()) {
                return;
            }

            asyncHandler.handleAsync(req, resp,
                    usersHandler.handleUpdatePermissionsAsync(req, username), "updatePermissions");
        }
    }


    private ApiResponse<?> handleDynamicRoutes(String path, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (path.startsWith("/player/head/")) {
            String username = path.replace("/player/head/", "").trim();
            return playerResourceHandler.handlePlayerHead(username, resp);
        }
        if (path.startsWith("/player/skin/")) {
            String username = path.replace("/player/skin/", "").trim();
            return playerResourceHandler.handlePlayerSkin(username, resp);
        }
        if (path.startsWith("/player/")) {
            String username = extractUsernameFromPath(path, "/player/");
            if (!username.isEmpty()) {
                handleAsyncPlayerDetails(req, resp, username);
                return null;
            }
        }
        if (path.startsWith("/punishments/")) {
            String punishmentId = path.replace("/punishments/", "");
            return punishmentsHandler.getPunishmentDetails(punishmentId);
        }

        return ApiResponse.notFound("Endpoint not found");
    }

    private String extractUsernameFromPath(String path, String prefix) {
        String username = path.replace(prefix, "").trim();
        if (username.contains("/")) {
            return username.substring(0, username.indexOf('/'));
        } else {
            return username;
        }
    }

    private String extractUsernameFromPath(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return "";
        }

        String middle = path.substring(prefix.length(), path.length() - suffix.length());
        return middle.trim();
    }

    private void handleSync2FARequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = RequestUtils.extractBearerToken(req);
        if (token == null) {
            return; // Will be handled as unauthorized
        }
        // Response handled by servlet
    }

    private void handleAsyncPlayerDetails(HttpServletRequest req, HttpServletResponse resp, String username) {
        asyncHandler.handleAsync(req, resp,
                playerResourceHandler.handlePlayerDetailsAsync(username), "playerDetails");
    }

    private void handleAsyncAllUsers(HttpServletRequest req, HttpServletResponse resp) {
        asyncHandler.handleAsync(req, resp,
                usersHandler.handleAllUsersAsync(req), "users");
    }

    private void handleAsyncGetMe(HttpServletRequest req, HttpServletResponse resp) {
        String token = RequestUtils.extractBearerToken(req);
        if (token != null) {
            asyncHandler.handleAsync(req, resp,
                    authHandler.handleGetMe(token, req), "getMe");
        }
    }

    private void handleSyncLogout(HttpServletRequest req, HttpServletResponse resp) {
        String logoutToken = RequestUtils.extractBearerToken(req);
        // Response handled by servlet
    }

    private void handleSyncRefresh(HttpServletRequest req, HttpServletResponse resp) {
        String refreshToken = RequestUtils.extractBearerToken(req);
        if (refreshToken == null) {
            return; // Will be handled as unauthorized
        }
        // Response handled by servlet
    }

    private void handleSyncRevokePunishment(HttpServletRequest req, HttpServletResponse resp) {
        String revokeToken = RequestUtils.extractBearerToken(req);
        if (revokeToken == null) {
            return; // Will be handled as unauthorized
        }
        // Response handled by servlet
    }

    private void handleSyncCreatePunishment(HttpServletRequest req, HttpServletResponse resp, JsonObject jsonBody) {
        // Response handled by servlet
    }
}