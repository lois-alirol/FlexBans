package fr.neocle.flexbans.handlers.security;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.handlers.security.components.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class AuthenticationHandler extends AbstractHandler {
    private final HandlerRegistry handlerRegistry;
    private final ErrorHandlers errorHandlers;
    private final DatabaseComponents databaseComponents;
    private final AuthComponents authComponents;
    private final EventDispatcher eventDispatcher;
    private final Logger logger;
    private final AuthConfig authConfig;

    private AuthenticationHandler(Builder builder) {
        this.handlerRegistry = builder.handlerRegistry;
        this.errorHandlers = builder.errorHandlers;
        this.databaseComponents = builder.databaseComponents;
        this.authComponents = builder.authComponents;
        this.eventDispatcher = builder.eventDispatcher;
        this.logger = builder.logger;
        this.authConfig = new AuthConfig();
    }

    public static class Builder {
        private HandlerRegistry handlerRegistry;
        private ErrorHandlers errorHandlers;
        private DatabaseComponents databaseComponents;
        private AuthComponents authComponents;
        private EventDispatcher eventDispatcher;
        private Logger logger;

        public Builder handlerRegistry(HandlerRegistry handlerRegistry) {
            this.handlerRegistry = handlerRegistry;
            return this;
        }

        public Builder errorHandlers(ErrorHandlers errorHandlers) {
            this.errorHandlers = errorHandlers;
            return this;
        }

        public Builder databaseComponents(DatabaseComponents databaseComponents) {
            this.databaseComponents = databaseComponents;
            return this;
        }

        public Builder authComponents(AuthComponents authComponents) {
            this.authComponents = authComponents;
            return this;
        }

        public Builder eventDispatcher(EventDispatcher eventDispatcher) {
            this.eventDispatcher = eventDispatcher;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public AuthenticationHandler build() {
            return new AuthenticationHandler(this);
        }
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
                if (authConfig.isLoginEnabled()) {
                    authComponents.getLoginHandler().handle(uri, baseRequest, request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                break;
            case "/login-with-discord":
                handleLoginWithDiscord(request, response);
                break;
            case "/callback":
                if (authConfig.isOauthEnabled()) {
                    authComponents.getDiscordOAuthHandler().handleOAuthCallback(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
            case "/logout":
                handleLogout(request, response);
                break;
            case "/register":
                if (authConfig.isLoginEnabled()) {
                    authComponents.getRegisterHandler().handle(uri, baseRequest, request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
                break;
            case "/register/submit":
                if (authConfig.isLoginEnabled()) {
                    authComponents.getRegisterHandler().handle(uri, baseRequest, request, response);
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
        if (authConfig.isOauthEnabled() && authConfig.isLoginEnabled()) {
            authComponents.getDiscordOAuthHandler().initiateOAuthFlow(response);
        } else {
            response.sendRedirect(authConfig.isLoginEnabled() ? "/login" : "/");
        }
    }

    private void handleRootPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("Welcome to the root page!");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void handleRootOrLogin(String target, HttpServletRequest request, Request baseRequest, HttpServletResponse response) throws IOException {
        if (authConfig.isLoginEnabled()) {
            HttpSession session = request.getSession(false);

            if (session != null && session.getAttribute("playerName") != null) {
                response.sendRedirect("/index");
                return;
            }

            authComponents.getLoginHandler().handle("/login", baseRequest, request, response);
            return;
        } else if (authConfig.isOauthEnabled()) {
            authComponents.getDiscordOAuthHandler().initiateOAuthFlow(response);
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
                    if (databaseComponents.getSessionManager().getPlayerFromSessionId(cookie.getValue()) != null &&
                            databaseComponents.getSessionManager().getPlayerFromSessionId(cookie.getValue()).equalsIgnoreCase(playerName)) {
                        isPersistentSession = true;
                    }
                }
            }
        }

        if (isPersistentSession) {
            databaseComponents.getSessionManager().deleteSessionByPlayerName(playerName);
        }

        request.getSession().invalidate();
        response.sendRedirect("/");
    }

    private void handleProtectedPage(HttpServletRequest request, HttpServletResponse response, Request baseRequest) throws IOException {
        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");

        String uri = request.getRequestURI();

        if ((authConfig.isOauthEnabled() || authConfig.isLoginEnabled()) &&
                ("/code-verification".equals(uri) || "/check-verification".equals(uri))) {
            if (userId != null || playerName != null) {
                authComponents.getCodeVerificationHandler().handle(uri, baseRequest, request, response);
                return;
            }
            errorHandlers.getForbiddenError().handle(request, response);
            return;
        }

        String identifier = (userId != null) ? userId : playerName;
        boolean isVerified = (userId != null) ?
                databaseComponents.getUserManager().isUserVerified(userId) :
                databaseComponents.getUserManager().isPlayerVerified(playerName);
        boolean isAllowed = (userId != null) ?
                authComponents.getDiscordOAuthHandler().isUserAllowed(userId) :
                isPlayerAllowed(playerName);

        if (!authConfig.isOauthEnabled() && !authConfig.isLoginEnabled()) {
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
            if (authConfig.isLoginEnabled()) {
                authComponents.getLoginHandler().handle("/login", baseRequest, request, response);
                return;
            } else if (authConfig.isOauthEnabled()) {
                authComponents.getDiscordOAuthHandler().initiateOAuthFlow(response);
                return;
            }
        }
    }

    public void handlePage(HttpServletRequest request, HttpServletResponse response, Request baseRequest) throws IOException {
        boolean pageHandled = false;
        String uri = request.getRequestURI();

        boolean playerHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.player.enabled");
        boolean moderatorHistoryEnabled = ConfigManager.getBoolean("webserver.pages.details.moderator.enabled");
        boolean punishmentDetailsEnabled = ConfigManager.getBoolean("webserver.pages.details.punishment.enabled");

        if (uri.startsWith("/player/") && playerHistoryEnabled) {
            handlerRegistry.getPlayerHistoryHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/moderator/") && moderatorHistoryEnabled) {
            handlerRegistry.getModeratorHistoryHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/details/") && punishmentDetailsEnabled) {
            handlerRegistry.getPunishmentDetailsHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/player-head/")) {
            handlerRegistry.getPlayerHeadHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/js/")) {
            handlerRegistry.getJavascriptHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if (uri.startsWith("/css/")) {
            handlerRegistry.getCssHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ((authConfig.isOauthEnabled() || authConfig.isLoginEnabled()) && uri.startsWith("/revoke-punishment")) {
            handlerRegistry.getRevokePunishmentHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ((authConfig.isOauthEnabled() || authConfig.isLoginEnabled()) &&
                (uri.startsWith("/new-punishment") || uri.startsWith("/new-punishment/submit"))) {
            handlerRegistry.getNewPunishmentHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;

        } else if ("/index".equals(uri) || uri.startsWith("/index")) {
            handlerRegistry.getIndexHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;
        } else if ("/live/punishments".equals(uri) || uri.startsWith("/live/punishments")) {
            handlerRegistry.getPunishmentSSEHandler().handle(uri, baseRequest, request, response);
            pageHandled = true;
            return;
        }

        if (!pageHandled) {
            errorHandlers.getNotFoundError().handle(request, response);
            return;
        }
    }

    public void updateConfig() {
    }

    public boolean isPlayerAllowed(String playerName) {
        List<String> allowedPlayers = ConfigManager.getList("password-auth.allowed-players");
        return allowedPlayers.contains(playerName);
    }
}