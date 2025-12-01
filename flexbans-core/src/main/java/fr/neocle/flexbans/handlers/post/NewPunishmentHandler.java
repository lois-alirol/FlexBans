package fr.neocle.flexbans.handlers.post;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.utils.ResourceLoader;
import fr.neocle.flexbans.utils.commandsexecution.CommandsExecution;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

public class NewPunishmentHandler extends AbstractHandler {
    private Logger logger;
    private final CommandsExecution commandsExecution;
    private final DatabaseUtils databaseUtils;
    private final Gson gson;

    public NewPunishmentHandler(CommandsExecution commandsExecution, DatabaseUtils databaseUtils, Logger logger) {
        this.commandsExecution = commandsExecution;
        this.databaseUtils = databaseUtils;
        this.logger = logger;
        this.gson = new Gson();
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

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/post/new_punishment.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for new punishment page.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }

        String serverIcon = ConfigManager.getString("server-display.icon");
        String serverFavicon = ConfigManager.getString("server-display.favicon");
        String serverLogo = ConfigManager.getString("server-display.logo");
        String serverColor = ConfigManager.getString("server-display.color");
        String serverColorDarker = ConfigManager.getString("server-display.darker-color");

        String userId = (String) request.getSession().getAttribute("userId");
        String playerName = (String) request.getSession().getAttribute("playerName");
        String identifier = "";

        if (userId == null) {
            identifier = (String) request.getSession().getAttribute("playerName");
        } else if (playerName == null) {
            identifier = databaseUtils.getUserManager().getUsernameFromDiscordId(userId);
        } else if (playerName != null && userId != null) {
            identifier = playerName;
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
            JsonObject jsonPayload = gson.fromJson(payload, JsonObject.class);
            String command = constructPunishmentCommand(jsonPayload);
            if (command == null) {
                respondWithError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid punishment type.");
                return;
            }

            commandsExecution.executeCommand(command);

            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("message", "Punishment applied successfully!");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (JsonSyntaxException e) {
            logger.severe("Error parsing JSON: " + e.getMessage());
            respondWithError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format.");
        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
            respondWithError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process the request.");
        }
    }

    private String constructPunishmentCommand(JsonObject payload) {
        String player = payload.has("target") ? payload.get("target").getAsString() : "";
        String identity = payload.has("identity") ? payload.get("identity").getAsString() : "";
        String punishmentType = payload.has("punishmentType") ? payload.get("punishmentType").getAsString() : "";
        String silent = payload.has("silent") ? payload.get("silent").getAsString() : "false";
        String reason = payload.has("reason") ? payload.get("reason").getAsString() : "";
        String duration = payload.has("duration") ? payload.get("duration").getAsString() : "";
        String permanent = payload.has("permanent") ? payload.get("permanent").getAsString() : "false";

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
        JsonObject error = new JsonObject();
        error.addProperty("error", errorMessage);
        response.getWriter().write(gson.toJson(error));
    }
}