package fr.neocle.flexbans.web.routing;

import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.async.AsyncRequestHandler;
import fr.neocle.flexbans.web.handler.*;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiRouter {
    private static final FlexLogger LOGGER = FlexLogger.get(ApiRouter.class);

    private final AuthHandler authHandler;
    private final PunishmentsHandler punishmentsHandler;
    private final ConfigHandler configHandler;
    private final PlayerResourceHandler playerResourceHandler;
    private final UsersHandler usersHandler;
    private final CsrfHandler csrfHandler;
    private final PunishmentCreationHandler punishmentCreationHandler;
    private final PunishmentEditionHandler punishmentEditionHandler;
    private final PunishmentRevocationHandler punishmentRevocationHandler;
    private final AsyncRequestHandler asyncHandler;

    public ApiRouter(
            AuthHandler authHandler,
            PunishmentsHandler punishmentsHandler,
            ConfigHandler configHandler,
            PlayerResourceHandler playerResourceHandler,
            UsersHandler usersHandler,
            CsrfHandler csrfHandler,
            PunishmentCreationHandler punishmentCreationHandler,
            PunishmentEditionHandler punishmentEditionHandler,
            PunishmentRevocationHandler punishmentRevocationHandler,
            AsyncRequestHandler asyncHandler
    ) {
        this.authHandler = authHandler;
        this.punishmentsHandler = punishmentsHandler;
        this.configHandler = configHandler;
        this.playerResourceHandler = playerResourceHandler;
        this.usersHandler = usersHandler;
        this.csrfHandler = csrfHandler;
        this.punishmentCreationHandler = punishmentCreationHandler;
        this.punishmentEditionHandler = punishmentEditionHandler;
        this.punishmentRevocationHandler = punishmentRevocationHandler;
        this.asyncHandler = asyncHandler;
    }

    public ApiResponse<?> routePost(String path, JsonObject jsonBody, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (path == null) {
            return ApiResponse.notFound("Endpoint not found");
        }

        String token = RequestUtils.extractBearerToken(req);

        switch (path) {
            case "/auth/login":
                asyncHandler.handleAsync(req, resp,
                        authHandler.handleLogin(jsonBody, req, resp), "login");
                return null;

            case "/auth/register":
                asyncHandler.handleAsync(req, resp,
                        authHandler.handleRegister(jsonBody, req, resp), "register");
                return null;

            case "/auth/2fa/request":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return authHandler.handleRequest2FA(token, resp);

            case "/auth/verify/complete":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing temporary token");
                }
                return authHandler.handleCompleteVerification(token, resp);

            case "/auth/logout":
                return authHandler.handleLogout(token, req, resp);

            case "/auth/refresh":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing refresh token");
                }
                return authHandler.handleRefresh(token, resp);

            case "/punishments/revoke":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return punishmentRevocationHandler.revokePunishment(req, jsonBody);

            case "/punishments/edit":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                try {
                    return punishmentEditionHandler.editPunishment(req, jsonBody).join();
                } catch (Exception e) {
                    LOGGER.error("Error while editing punishment", e);
                    return ApiResponse.error(500, "Unable to process the request");
                }

            case "/punishments/create":
                if (token == null) {
                    return ApiResponse.unauthorized("Missing authorization token");
                }
                return punishmentCreationHandler.createPunishment(req, jsonBody);

            default:
                return ApiResponse.notFound("Endpoint not found");
        }
    }

    public ApiResponse<?> routeGet(String path, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (path == null) {
            return ApiResponse.notFound("Endpoint not found");
        }

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
            playerResourceHandler.handlePlayerHead(username, resp).join();
            return null;
        }

        if (path.startsWith("/player/skin/")) {
            String username = path.replace("/player/skin/", "").trim();
            playerResourceHandler.handlePlayerSkin(username, resp).join();
            return null;
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
}