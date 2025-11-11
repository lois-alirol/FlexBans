package fr.neocle.flexbans.handlers.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoginHandler extends AbstractHandler {
    private final Logger logger;
    private final EventDispatcher eventDispatcher;
    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final Gson gson;

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = TimeUnit.MINUTES.toMillis(15);
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();

    public LoginHandler(Logger logger, DatabaseUtils databaseUtils, EventDispatcher eventDispatcher) {
        this.logger = logger;
        this.eventDispatcher = eventDispatcher;
        this.userManager = databaseUtils.getUserManager();
        this.sessionManager = databaseUtils.getSessionManager();
        this.gson = new Gson();
    }

    private static class LoginAttempt {
        int attempts;
        long lockoutUntil;

        LoginAttempt() {
            this.attempts = 0;
            this.lockoutUntil = 0;
        }
    }

    private void cleanupOldAttempts() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            loginAttempts.entrySet().removeIf(entry ->
                    entry.getValue().lockoutUntil > 0 &&
                            currentTime > entry.getValue().lockoutUntil
            );
            lastCleanupTime = currentTime;
        }
    }

    private boolean isLockedOut(String identifier) {
        cleanupOldAttempts();
        LoginAttempt attempt = loginAttempts.get(identifier);
        if (attempt == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if (attempt.lockoutUntil > currentTime) {
            return true;
        }
        if (attempt.lockoutUntil > 0 && currentTime > attempt.lockoutUntil) {
            loginAttempts.remove(identifier);
        }
        return false;
    }

    private void recordFailedAttempt(String identifier) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(identifier, k -> new LoginAttempt());
        attempt.attempts++;

        if (attempt.attempts >= MAX_ATTEMPTS) {
            attempt.lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            logger.warning("IP address locked out due to too many failed attempts: " + identifier);
        }
    }

    private void resetAttempts(String identifier) {
        loginAttempts.remove(identifier);
    }

    private long getRemainingLockoutTime(String identifier) {
        LoginAttempt attempt = loginAttempts.get(identifier);
        if (attempt == null || attempt.lockoutUntil == 0) {
            return 0;
        }
        long remaining = attempt.lockoutUntil - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/login".equalsIgnoreCase(target)) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/security/login.html");
            if (htmlTemplate == null) {
                logger.warning("Unable to load HTML template for login page.");
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            HttpSession session = request.getSession(true);
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("session_id".equals(cookie.getName())) {
                        session.setAttribute("playerName", sessionManager.getPlayerFromSessionId(cookie.getValue()));
                        if (session.getAttribute("playerName") != null) {
                            response.sendRedirect("/index");
                            return;
                        }
                    }
                }
            }

            String serverIcon = (String) ConfigManager.getConfigValue("server-display.icon");
            String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
            String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
            String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
            String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
            String serverName = (String) ConfigManager.getConfigValue("server-display.name");

            boolean oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));

            String discordLoginButton = "";
            if (oauthEnabled) {
                discordLoginButton =
                        "            <div class=\"flex items-center my-4\">\n" +
                                "                <hr class=\"flex-grow border-[#4b5563]\">\n" +
                                "                <span class=\"px-2 text-sm text-[#a1a1aa]\">or</span>\n" +
                                "                <hr class=\"flex-grow border-[#4b5563]\">\n" +
                                "            </div>\n" +
                                "            <div class=\"mt-6\">\n" +
                                "                <a href=\"/login-with-discord\" class=\"w-full inline-flex items-center justify-center bg-[#5865F2] hover:bg-[#4752C4] text-white font-bold py-3 px-6 rounded-lg transition duration-300\">\n" +
                                "                    <i class=\"fa-brands fa-discord mr-2\"></i> Login with Discord\n" +
                                "                </a>\n" +
                                "            </div>";
            }

            String pageContent = htmlTemplate
                    .replace("{{server_name}}", serverName)
                    .replace("{{server_icon}}", serverIcon)
                    .replace("{{server_favicon}}", serverFavicon)
                    .replace("{{server_color}}", serverColor)
                    .replace("{{server_color_hover}}", serverColorDarker)
                    .replace("{{server_logo}}", serverLogo)
                    .replace("{{discord_button}}", discordLoginButton);

            response.getWriter().write(pageContent);

        } else if ("/login/submit".equalsIgnoreCase(target)) {
            response.setContentType("application/json;charset=utf-8");
            baseRequest.setHandled(true);

            String userAgent = request.getHeader("User-Agent");
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            if (isLockedOut(ipAddress)) {
                long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(getRemainingLockoutTime(ipAddress));
                long remainingMinutes = remainingSeconds / 60;
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Too many failed login attempts. Please try again in " +
                        remainingMinutes + " minute" + (remainingMinutes > 1 ? "s" : "") + ".");
                response.getWriter().write(gson.toJson(error));
                return;
            }

            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Method not allowed");
                response.getWriter().write(gson.toJson(error));
                return;
            }

            try {
                BufferedReader reader = request.getReader();
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }

                JsonObject requestData = gson.fromJson(requestBody.toString(), JsonObject.class);

                String username = requestData.has("username") ? requestData.get("username").getAsString() : "";
                String password = requestData.has("password") ? requestData.get("password").getAsString() : "";
                boolean stayLogggedIn = requestData.has("stayLoggedIn") &&
                        Boolean.parseBoolean(requestData.get("stayLoggedIn").getAsString());

                if (username.isEmpty() || password.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "All fields are required.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (!userManager.isUserRegistered(username)) {
                    recordFailedAttempt(ipAddress);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid username or password.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (!userManager.validateUserCredentials(username, password)) {
                    recordFailedAttempt(ipAddress);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid username or password.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                resetAttempts(ipAddress);

                eventDispatcher.playerLoginEvent(username, userAgent, ipAddress);

                HttpSession session = request.getSession(true);
                session.setAttribute("playerName", username);

                response.setStatus(HttpServletResponse.SC_OK);
                JsonObject success = new JsonObject();
                success.addProperty("message", "Login successful.");
                response.getWriter().write(gson.toJson(success));

                if (stayLogggedIn) {
                    Cookie sessionCookie = new Cookie("session_id", session.getId());
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setMaxAge(60 * 60 * 24 * 14);
                    sessionCookie.setPath("/");
                    response.addCookie(sessionCookie);
                    sessionManager.insertSessionData(session.getId(), username);
                }
            } catch (JsonSyntaxException e) {
                logger.severe("Error parsing JSON: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid JSON format.");
                response.getWriter().write(gson.toJson(error));
            } catch (Exception e) {
                logger.severe("Error processing login: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "An internal error occurred.");
                response.getWriter().write(gson.toJson(error));
            }
        }
    }
}