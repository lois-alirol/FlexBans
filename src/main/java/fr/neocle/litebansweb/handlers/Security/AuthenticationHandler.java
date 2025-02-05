package fr.neocle.litebansweb.handlers.Security;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import fr.neocle.litebansweb.handlers.*;
import fr.neocle.litebansweb.handlers.Errors.*;
import fr.neocle.litebansweb.handlers.PostRequestHandlers.*;
import fr.neocle.litebansweb.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private final RevokePunishmentHandler revokePunishmentHandler;
    private NewPunishmentHandler newPunishmentHandler;

    private final ForbiddenError forbiddenError;
    private final NotFoundError notFoundError;

    private final DatabaseUtils databaseUtils;
    private final CodeVerificationHandler codeVerificationHandler;
    private final RegisterHandler registerHandler;
    private final LoginHandler loginHandler;
    private final DiscordOAuthHandler discordOAuthHandler;

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
            RevokePunishmentHandler revokePunishmentHandler,
            NewPunishmentHandler newPunishmentHandler,
            ForbiddenError forbiddenError,
            NotFoundError notFoundError,
            DatabaseUtils databaseUtils,
            CodeVerificationHandler codeVerificationHandler,
            RegisterHandler registerHandler,
            LoginHandler loginHandler,
            DiscordOAuthHandler discordOAuthHandler) {

        this.oauthConfig = oauthConfig;
        this.loginConfig = loginConfig;
        this.indexHandler = indexHandler;
        this.playerHistoryHandler = playerHistoryHandler;
        this.moderatorHistoryHandler = moderatorHistoryHandler;
        this.punishmentDetailsHandler = punishmentDetailsHandler;
        this.playerHeadHandler = playerHeadHandler;
        this.revokePunishmentHandler = revokePunishmentHandler;
        this.newPunishmentHandler = newPunishmentHandler;
        this.forbiddenError = forbiddenError;
        this.notFoundError = notFoundError;
        this.databaseUtils = databaseUtils;
        this.codeVerificationHandler = codeVerificationHandler;
        this.registerHandler = registerHandler;
        this.loginHandler = loginHandler;
        this.discordOAuthHandler = discordOAuthHandler;
        
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
                handleLoginWithDiscord(response);
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

    private void handleLoginWithDiscord(HttpServletResponse response) throws IOException {
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
        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");
    
        if (userId != null) {
            if (!databaseUtils.isUserVerified(userId)) {
                response.sendRedirect("/code-verification");
                return;
            }
    
            if (discordOAuthHandler.isUserAllowed(userId)) {
                response.sendRedirect("/index?type=bans");
                return;
            }
        }
    
        if (playerName != null) {
            if (!databaseUtils.isPlayerVerified(playerName)) {
                response.sendRedirect("/code-verification");
                return;
            }
    
            if (isPlayerAllowed(playerName)) {
                response.sendRedirect("/index?type=bans");
                return;
            }
        }
    
        if (userId == null && playerName == null) {
            if (loginEnabled) {
                loginHandler.handle("/login", baseRequest, request, response);
                return;
            } else if (oauthEnabled) {
                discordOAuthHandler.initiateOAuthFlow(response);
                return;
            }
        }
        forbiddenError.handle(request, response);
    }    

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().invalidate();
        response.sendRedirect("/");
    }

    private void handleProtectedPage(HttpServletRequest request, HttpServletResponse response, Request baseRequest) throws IOException {
        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");
    
        if ((oauthEnabled || loginEnabled) 
            && (((userId == null || !discordOAuthHandler.isUserAllowed(userId))) 
            && ((playerName == null || !isPlayerAllowed(playerName))))) {
    
            forbiddenError.handle(request, response);
            return;
        }
    
        if (userId != null && !databaseUtils.isUserVerified(userId)) {
            response.sendRedirect("/code-verification");
            return;
        }
        if (playerName != null && !databaseUtils.isPlayerVerified(playerName)) {
            response.sendRedirect("/code-verification");
            return;
        }
    
        String uri = request.getRequestURI();
        boolean pageHandled = false;
    
        if (uri.startsWith("/player/")) {
            playerHistoryHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if (uri.startsWith("/moderator/")) {
            moderatorHistoryHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if (uri.startsWith("/details/")) {
            punishmentDetailsHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if (uri.startsWith("/player-head/")) {
            playerHeadHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if ((oauthEnabled || loginEnabled) && uri.startsWith("/revoke-punishment")) {
            revokePunishmentHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if ((oauthEnabled || loginEnabled) && (uri.startsWith("/new-punishment") || uri.startsWith("/new-punishment/submit"))) {
            newPunishmentHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if ((oauthEnabled || loginEnabled) && ("/code-verification".equals(uri) || "/check-verification".equals(uri))) {
            codeVerificationHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        } else if ("/index".equals(uri) || uri.startsWith("/index")) {
            indexHandler.handle(uri, baseRequest, request, response);
            pageHandled = true;
        }
    
        if (!pageHandled) {
            notFoundError.handle(request, response);
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
        List<String> allowedPlayers = (List<String>) loginConfig.get("allowed_players");

        if (allowedPlayers == null || allowedPlayers.isEmpty()) {
            return false;
        }

        return allowedPlayers.contains(playerName);
    }

}