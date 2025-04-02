package fr.neocle.flexbans.handlers.Security;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.Dashboard.SessionManager;
import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.Errors.ForbiddenError;
import fr.neocle.flexbans.handlers.Errors.NotFoundError;
import fr.neocle.flexbans.handlers.*;
import fr.neocle.flexbans.handlers.PostRequestHandlers.NewPunishmentHandler;
import fr.neocle.flexbans.handlers.PostRequestHandlers.RevokePunishmentHandler;
import fr.neocle.flexbans.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AuthenticationHandler extends AbstractHandler {
    private final IndexHandler indexHandler;
    private final PlayerHistoryHandler playerHistoryHandler;
    private final ModeratorHistoryHandler moderatorHistoryHandler;
    private final PunishmentDetailsHandler punishmentDetailsHandler;
    private final PlayerHeadHandler playerHeadHandler;
    private final ScriptsHandler scriptsHandler;
    private final RevokePunishmentHandler revokePunishmentHandler;
    private NewPunishmentHandler newPunishmentHandler;

    private final ForbiddenError forbiddenError;
    private final NotFoundError notFoundError;

    private final DatabaseUtils databaseUtils;
    private final SessionManager sessionManager;
    private final UserManager userManager;
    private final CodeVerificationHandler codeVerificationHandler;
    private final RegisterHandler registerHandler;
    private final LoginHandler loginHandler;
    private final DiscordOAuthHandler discordOAuthHandler;

    private final EventDispatcher eventDispatcher;

    private final Logger logger;

    private final boolean oauthEnabled;
    private final boolean loginEnabled;

    public AuthenticationHandler(
            IndexHandler indexHandler,
            PlayerHistoryHandler playerHistoryHandler,
            ModeratorHistoryHandler moderatorHistoryHandler,
            PunishmentDetailsHandler punishmentDetailsHandler,
            PlayerHeadHandler playerHeadHandler,
            ScriptsHandler scriptsHandler,
            RevokePunishmentHandler revokePunishmentHandler,
            NewPunishmentHandler newPunishmentHandler,
            ForbiddenError forbiddenError,
            NotFoundError notFoundError,
            DatabaseUtils databaseUtils,
            CodeVerificationHandler codeVerificationHandler,
            RegisterHandler registerHandler,
            LoginHandler loginHandler,
            DiscordOAuthHandler discordOAuthHandler,
            EventDispatcher eventDispatcher,
            Logger logger) {

        this.indexHandler = indexHandler;
        this.playerHistoryHandler = playerHistoryHandler;
        this.moderatorHistoryHandler = moderatorHistoryHandler;
        this.punishmentDetailsHandler = punishmentDetailsHandler;
        this.playerHeadHandler = playerHeadHandler;
        this.scriptsHandler = scriptsHandler;
        this.revokePunishmentHandler = revokePunishmentHandler;
        this.newPunishmentHandler = newPunishmentHandler;
        this.forbiddenError = forbiddenError;
        this.notFoundError = notFoundError;
        this.databaseUtils = databaseUtils;
        this.codeVerificationHandler = codeVerificationHandler;
        this.registerHandler = registerHandler;
        this.loginHandler = loginHandler;
        this.discordOAuthHandler = discordOAuthHandler;
        this.eventDispatcher = eventDispatcher;
        this.logger = logger;

        this.oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));
        this.loginEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("password-auth.enabled"));

        this.sessionManager = databaseUtils.getSessionManager();
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        switch (uri) {
            case "/":
                handleRootPage(request, response);
                break;
            case "/login":
                handleRootOrLogin(target, request, baseRequest, response);
                break;
            case "/login/submit":
                if (loginEnabled) {
                    loginHandler.handle(uri, baseRequest, request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                break;
            case "/login-with-discord":
                handleLoginWithDiscord(request, response);
                break;
            case "/callback":
                if (oauthEnabled) {
                    discordOAuthHandler.handleOAuthCallback(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
            case "/logout":
                handleLogout(request, response);
                break;
            case "/register":
                if (loginEnabled) {
                    registerHandler.handle(uri, baseRequest, request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                break;
            case "/register/submit":
                if (loginEnabled) {
                    registerHandler.handle(uri, baseRequest, request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                break;
            default:
                handleProtectedPage(request, response, baseRequest);
        }
        baseRequest.setHandled(true);
    }

    private void handleLoginWithDiscord(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (oauthEnabled && loginEnabled) {
            discordOAuthHandler.initiateOAuthFlow(response);
        } else {
            response.sendRedirect(loginEnabled ? "/login" : "/");
        }
    }

    private void handleRootPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("Welcome to the root page!");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void handleRootOrLogin(String target, HttpServletRequest request, Request baseRequest, HttpServletResponse response) throws IOException {
        if (loginEnabled) {
            HttpSession session = request.getSession(false);

            if (session != null && session.getAttribute("playerName") != null) {
                response.sendRedirect("/index");
                return;
            }

            loginHandler.handle("/login", baseRequest, request, response);
            return;
        } else if (oauthEnabled) {
            discordOAuthHandler.initiateOAuthFlow(response);
            return;
        }

        response.sendRedirect("/index");
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        eventDispatcher.logoutEvent(playerName, userId, userAgent, ipAddress);

        boolean isPersistentSession = false;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("session_id".equals(cookie.getName())) {
                    if (sessionManager.getPlayerFromSessionId(cookie.getValue()) != null && sessionManager.getPlayerFromSessionId(cookie.getValue()).equalsIgnoreCase(playerName)) {
                        isPersistentSession = true;
                    }
                }
            }
        }

        if (isPersistentSession) {
            sessionManager.deleteSessionByPlayerName(playerName);
        }

        request.getSession().invalidate();
        response.sendRedirect("/");
    }

    private void handleProtectedPage(HttpServletRequest request, HttpServletResponse response, Request baseRequest) throws IOException {
        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");

        String uri = request.getRequestURI();

        if ((oauthEnabled || loginEnabled) && ("/code-verification".equals(uri) || "/check-verification".equals(uri))) {
            if (userId != null || playerName != null) {
                codeVerificationHandler.handle(uri, baseRequest, request, response);
                return;
            }
            forbiddenError.handle(request, response);
            return;
        }

        String identifier = (userId != null) ? userId : playerName;
        boolean isVerified = (userId != null) ? userManager.isUserVerified(userId) : userManager.isPlayerVerified(playerName);
        boolean isAllowed = (userId != null) ? discordOAuthHandler.isUserAllowed(userId) : isPlayerAllowed(playerName);

        if (!oauthEnabled && !loginEnabled) {
            handlePage(request, response, baseRequest);
            return;
        }

        if (identifier != null) {
            if (!isVerified) {
                response.sendRedirect("/code-verification");
                return;
            } else if (isAllowed) {
                handlePage(request, response, baseRequest);
                return;
            }
        } else {
            if (loginEnabled) {
                loginHandler.handle("/login", baseRequest, request, response);
                return;
            } else if (oauthEnabled) {
                discordOAuthHandler.initiateOAuthFlow(response);
                return;
            }
        }
    }

    public void handlePage(HttpServletRequest request, HttpServletResponse response, Request baseRequest) throws IOException {
        boolean pageHandled = false;
        String uri = request.getRequestURI();

        if (uri.startsWith("/player/")) {
            playerHistoryHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/moderator/")) {
            moderatorHistoryHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/details/")) {
            punishmentDetailsHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/player-head/")) {
            playerHeadHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/js/")) {
            scriptsHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ((oauthEnabled || loginEnabled) && uri.startsWith("/revoke-punishment")) {
            revokePunishmentHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ((oauthEnabled || loginEnabled) && (uri.startsWith("/new-punishment") || uri.startsWith("/new-punishment/submit"))) {
            newPunishmentHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ("/index".equals(uri) || uri.startsWith("/index")) {
            indexHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;
        }

        if (!pageHandled) {
            notFoundError.handle(request, response);
            return;
        }
    }

    public void updateConfig() {
        // TO DO
    }

    public void setNewPunishmentHandler(NewPunishmentHandler newPunishmentHandler) {
        this.newPunishmentHandler = newPunishmentHandler;
    }

    @SuppressWarnings("unchecked")
    public boolean isPlayerAllowed(String playerName) {
        Object rawValue = ConfigManager.getConfigValue("password-auth.allowed-players");

        logger.info(rawValue.toString());

        if (rawValue instanceof List<?> list) {
            List<String> allowedPlayers = list.stream()
                    .map(Object::toString)
                    .toList();
            return allowedPlayers.contains(playerName);
        }

        return false;
    }
}