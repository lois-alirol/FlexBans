package fr.neocle.flexbans.handlers.Security;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.Dashboard.SessionManager;
import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

public class LoginHandler extends AbstractHandler {
    private final Logger logger;
    private final EventDispatcher eventDispatcher;
    private final UserManager userManager;
    private final SessionManager sessionManager;

    public LoginHandler(Logger logger, DatabaseUtils databaseUtils, EventDispatcher eventDispatcher) {
        this.logger = logger;
        this.eventDispatcher = eventDispatcher;
        this.userManager = databaseUtils.getUserManager();
        this.sessionManager = databaseUtils.getSessionManager();
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

            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                response.getWriter().write("{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                BufferedReader reader = request.getReader();
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }

                JSONObject requestData = new JSONObject(requestBody.toString());

                String username = requestData.optString("username");
                String password = requestData.optString("password");

                boolean stayLogggedIn = Boolean.parseBoolean(requestData.optString("stayLoggedIn"));
                if (username.isEmpty() || password.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"All fields are required.\"}");
                    return;
                }

                if (!userManager.isUserRegistered(username)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid username or password.\"}");
                    return;
                }

                if (!userManager.validateUserCredentials(username, password)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid username or password.\"}");
                    return;
                }

                eventDispatcher.playerLoginEvent(username, userAgent, ipAddress);

                HttpSession session = request.getSession(true);
                session.setAttribute("playerName", username);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"message\":\"Login successful.\"}");

                if (stayLogggedIn) {
                    Cookie sessionCookie = new Cookie("session_id", session.getId());
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setMaxAge(60 * 60 * 24 * 14);
                    sessionCookie.setPath("/");
                    response.addCookie(sessionCookie);
                    sessionManager.insertSessionData(session.getId(), username);
                }
            } catch (Exception e) {
                logger.severe("Error processing login: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"An internal error occurred.\"}");
            }
        }
    }
}
