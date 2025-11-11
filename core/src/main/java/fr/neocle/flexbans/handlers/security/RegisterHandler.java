package fr.neocle.flexbans.handlers.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class RegisterHandler extends AbstractHandler {
    private final Logger logger;
    private final EventDispatcher eventDispatcher;
    private final UserManager userManager;
    private final Gson gson;

    public RegisterHandler(Logger logger, DatabaseUtils databaseUtils, EventDispatcher eventDispatcher) {
        this.logger = logger;
        this.eventDispatcher = eventDispatcher;
        this.userManager = databaseUtils.getUserManager();
        this.gson = new Gson();
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
                JsonObject error = new JsonObject();
                error.addProperty("error", "Method not allowed");
                response.getWriter().write(gson.toJson(error));
                return;
            }

            try {
                String body = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
                JsonObject requestData = gson.fromJson(body, JsonObject.class);

                String username = requestData.has("username") ? requestData.get("username").getAsString() : "";
                String password = requestData.has("password") ? requestData.get("password").getAsString() : "";
                String confirmPassword = requestData.has("confirmPassword") ? requestData.get("confirmPassword").getAsString() : "";

                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "All fields are required.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Passwords do not match.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (password.length() < 8) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Password must be at least 8 characters long.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (userManager.isUserRegistered(username)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "User is already registered.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                if (!isPlayerAllowed(username)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "You are not allowed to register.");
                    response.getWriter().write(gson.toJson(error));
                    return;
                }

                userManager.registerUser(username, password);

                if (userManager.isUserRegistered(username)) {
                    request.getSession().setAttribute("playerName", username);
                    response.setStatus(HttpServletResponse.SC_OK);
                    JsonObject success = new JsonObject();
                    success.addProperty("message", "Registration successful.");
                    response.getWriter().write(gson.toJson(success));

                    eventDispatcher.playerRegisterEvent(username, userAgent, ipAddress);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Registration failed. Please try again later.");
                    response.getWriter().write(gson.toJson(error));
                }
            } catch (JsonSyntaxException e) {
                logger.severe("Error parsing JSON: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid JSON format.");
                response.getWriter().write(gson.toJson(error));
            } catch (Exception e) {
                logger.severe("Error processing registration: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "An internal error occurred.");
                response.getWriter().write(gson.toJson(error));
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