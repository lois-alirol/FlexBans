package fr.neocle.litebansweb.handlers.Security;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.handlers.*;
import fr.neocle.litebansweb.handlers.Errors.*;
import fr.neocle.litebansweb.handlers.PostRequestHandlers.*;
import fr.neocle.litebansweb.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AuthenticationHandler extends AbstractHandler {

    private final Map<String, Object> oauthConfig;
    private final Map<String, Object> loginConfig;

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
    private final CodeVerificationHandler codeVerificationHandler;
    private final RegisterHandler registerHandler;
    private final LoginHandler loginHandler;
    private final DiscordOAuthHandler discordOAuthHandler;

    private final EventDispatcher eventDispatcher;

    private final boolean oauthEnabled;
    private final boolean loginEnabled;

    public AuthenticationHandler(
            Map<String, Object> oauthConfig,
            Map<String, Object> loginConfig,
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
            EventDispatcher eventDispatcher) {

        this.oauthConfig = oauthConfig;
        this.loginConfig = loginConfig;
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
        
        this.oauthEnabled = Boolean.parseBoolean(oauthConfig.getOrDefault("enabled", "false").toString());
        this.loginEnabled = Boolean.parseBoolean(loginConfig.getOrDefault("enabled", "false").toString());
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
    
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("session_id".equals(cookie.getName())) {
                        session = request.getSession(true);
                        session.setAttribute("playerName", databaseUtils.getPlayerFromSessionId(cookie.getValue()));
                        response.sendRedirect("/index");
                        return;
                    }
                }
            }

            loginHandler.handle("/login", baseRequest, request, response);
            return;
        } else if (oauthEnabled) {
            discordOAuthHandler.initiateOAuthFlow(response);
            return;
        }
    
        forbiddenError.handle(request, response);
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
        boolean isVerified = (userId != null) ? databaseUtils.isUserVerified(userId) : databaseUtils.isPlayerVerified(playerName);
        boolean isAllowed = (userId != null) ? discordOAuthHandler.isUserAllowed(userId) : isPlayerAllowed(playerName);

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

    public void updateConfig(Map<String, Object> newConfig) {
        oauthConfig.clear();
        oauthConfig.putAll(newConfig);
    }

    public void setNewPunishmentHandler(NewPunishmentHandler newPunishmentHandler) {
        this.newPunishmentHandler = newPunishmentHandler;
    }

    @SuppressWarnings("unchecked")
    public boolean isPlayerAllowed(String playerName) {
        List<String> allowedPlayers = (List<String>) loginConfig.get("allowed-players");

        if (allowedPlayers == null || allowedPlayers.isEmpty()) {
            return false;
        }

        return allowedPlayers.contains(playerName);
    }

}