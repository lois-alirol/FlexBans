package fr.neocle.flexbans.handlers.PostRequestHandlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.utils.CommandsExecution.CommandsExecution;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class NewPunishmentHandler extends AbstractHandler {
    private Logger logger = Logger.getLogger("FlexBans");
    private final CommandsExecution commandsExecution;
    private final DatabaseUtils databaseUtils;

    public NewPunishmentHandler(CommandsExecution commandsExecution, DatabaseUtils databaseUtils) {
        this.commandsExecution = commandsExecution;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/new-punishment".equalsIgnoreCase(target)) {
            handleNewPunishmentPage(response, baseRequest, request);
        } else if ("/new-punishment/submit".equalsIgnoreCase(target)) {
            handlePunishmentSubmission(response, baseRequest, request);
        }
    }

    private void handleNewPunishmentPage(HttpServletResponse response, Request baseRequest, HttpServletRequest request) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/postActions/new_punishment.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for new punishment page.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }

        String serverIcon = (String) ConfigManager.getConfigValue("server-display.icon");
        String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
        String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
        String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
        String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");

        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");
        String identifier = "";

        if (userId == null) {
            identifier = (String) request.getSession().getAttribute("playerName");
        } else if (playerName == null) {
            identifier = databaseUtils.getUserManager().getUsernameFromDiscordId(userId);
        }

        String pageContent = htmlTemplate
                .replace("{{executor_name}}", identifier)
                .replace("{{server_icon}}", serverIcon)
                .replace("{{server_favicon}}", serverFavicon)
                .replace("{{server_color}}", serverColor)
                .replace("{{server_color_hover}}", serverColorDarker)
                .replace("{{server_logo}}", serverLogo);

        response.getWriter().write(pageContent);
    }

    private void handlePunishmentSubmission(HttpServletResponse response, Request baseRequest, HttpServletRequest request) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            respondWithError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only POST method is allowed.");
            return;
        }

        baseRequest.setHandled(true);

        String payload = readRequestBody(request);
        if (payload == null) {
            respondWithError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request payload.");
            return;
        }

        try {
            JSONObject jsonPayload = new JSONObject(payload);
            String command = constructPunishmentCommand(jsonPayload);
            if (command == null) {
                respondWithError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid punishment type.");
                return;
            }

            commandsExecution.executeCommand(command);

            JSONObject jsonResponse = new JSONObject().put("message", "Punishment applied successfully!");
            response.setContentType("application/json");
            response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            respondWithError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process the request.");
        }
    }

    private String constructPunishmentCommand(JSONObject payload) {
        String player = payload.optString("target");
        String identity = payload.optString("identity");
        String punishmentType = payload.optString("punishmentType");
        String silent = payload.optString("silent");
        String reason = payload.optString("reason");
        String duration = payload.optString("duration");
        String permanent = payload.optString("permanent");

        if (!isValidPunishmentType(punishmentType)) return null;

        String baseCommand = "litebans:" + punishmentType.toLowerCase() + " " + player + " " + reason;
        String durationPart = (!"kick".equalsIgnoreCase(punishmentType) && !Boolean.parseBoolean(permanent)) ? " " + duration : "";
        String silentPart = Boolean.parseBoolean(silent) ? " -s" : "";
        String senderPart = " --sender=" + identity;

        return baseCommand + durationPart + silentPart + senderPart;
    }

    private boolean isValidPunishmentType(String type) {
        return "ban".equalsIgnoreCase(type) || "mute".equalsIgnoreCase(type) ||
                "warn".equalsIgnoreCase(type) || "kick".equalsIgnoreCase(type);
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            logger.severe("Error reading request body: " + e.getMessage());
            return null;
        }
        return stringBuilder.toString();
    }

    private void respondWithError(HttpServletResponse response, int statusCode, String errorMessage) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.getWriter().write(new JSONObject().put("error", errorMessage).toString());
    }
}
