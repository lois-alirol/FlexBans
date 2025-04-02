package fr.neocle.flexbans.handlers.Security;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class RegisterHandler extends AbstractHandler {
    private final Logger logger;
    private final EventDispatcher eventDispatcher;
    private final UserManager userManager;

    public RegisterHandler(Logger logger, DatabaseUtils databaseUtils, EventDispatcher eventDispatcher) {
        this.logger = logger;
        this.eventDispatcher = eventDispatcher;
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/register".equalsIgnoreCase(target)) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            if (request.getSession().getAttribute("playerName") != null) {
                response.sendRedirect("/index");
                return;
            }

            String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/security/register.html");
            if (htmlTemplate == null) {
                logger.warning("Unable to load HTML template for login page.");
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            String serverIcon = (String) ConfigManager.getConfigValue("server-display.icon");
            String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
            String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
            String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
            String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
            String serverName = (String) ConfigManager.getConfigValue("server-display.name");

            String pageContent = htmlTemplate
                    .replace("{{server_name}}", serverName)
                    .replace("{{server_icon}}", serverIcon)
                    .replace("{{server_favicon}}", serverFavicon)
                    .replace("{{server_color}}", serverColor)
                    .replace("{{server_color_hover}}", serverColorDarker)
                    .replace("{{server_logo}}", serverLogo);

            response.getWriter().write(pageContent);

        } else if ("/register/submit".equalsIgnoreCase(target)) {
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
                String body = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
                JSONObject requestData = new JSONObject(body);

                String username = requestData.optString("username");
                String password = requestData.optString("password");
                String confirmPassword = requestData.optString("confirmPassword");

                if (username == null || username.isEmpty() ||
                        password == null || password.isEmpty() ||
                        confirmPassword == null || confirmPassword.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"All fields are required.\"}");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"Passwords do not match.\"}");
                    return;
                }

                if (password.length() < 8) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"Password must be at least 8 characters long.\"}");
                    return;
                }

                if (userManager.isUserRegistered(username)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\":\"User is already registered.\"}");
                    return;
                }

                if (!isPlayerAllowed(username)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"You are not allowed to register.\"}");
                    return;
                }

                userManager.registerUser(username, password);

                if (userManager.isUserRegistered(username)) {
                    request.getSession().setAttribute("playerName", username);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"message\":\"Registration successful.\"}");

                    eventDispatcher.playerRegisterEvent(username, userAgent, ipAddress);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"error\":\"Registration failed. Please try again later.\"}");
                }
            } catch (Exception e) {
                logger.severe("Error processing registration: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"An internal error occurred.\"}");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean isPlayerAllowed(String playerName) {
        Object rawValue = ConfigManager.getConfigValue("password-auth.allowed-players");

        if (rawValue instanceof List<?> list) {
            List<String> allowedPlayers = list.stream()
                    .map(Object::toString)
                    .toList();
            return allowedPlayers.contains(playerName);
        }

        return false;
    }
}
