package fr.neocle.flexbans.handler.web.post;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.HooksUtils;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class RevokePunishmentHandler extends AbstractHandler {
    private final CommandsExecution commandsExecution;
    private final Gson gson;

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();

    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\- ]{1,255}$");

    public RevokePunishmentHandler(CommandsExecution commandsExecution) {
        this.commandsExecution = commandsExecution;
        this.gson = new Gson();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/revoke-punishment".equalsIgnoreCase(target)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().write("Error: Only POST method is allowed.");
            return;
        }

        baseRequest.setHandled(true);

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            FlexLogger.warn("Error reading request body: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error: Invalid request payload.");
            return;
        }

        try {
            JsonObject jsonPayload = gson.fromJson(stringBuilder.toString(), JsonObject.class);

            String identity = jsonPayload.has("identity") ? jsonPayload.get("identity").getAsString() : null;
            String removalReason = jsonPayload.has("removalReason") ? sanitizeInput(jsonPayload.get("removalReason").getAsString()) : null;
            String punishmentType = jsonPayload.has("punishmentType") ? sanitizeInput(jsonPayload.get("punishmentType").getAsString()) : null;
            String punishmentId = jsonPayload.has("punishmentId") ? sanitizeInput(jsonPayload.get("punishmentId").getAsString()) : null;

            if (identity == null || removalReason == null || punishmentType == null || punishmentId == null) {
                FlexLogger.info(identity + " " + removalReason + " " + punishmentType + " " + punishmentId);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Error: Invalid input.");
                return;
            }

            String command = buildCommand(punishmentType, punishmentId, removalReason, identity);
            if (command == null) {
                FlexLogger.warn("Invalid punishment type: " + punishmentType);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Error: Invalid punishment type.");
                return;
            }

            if (usingFlexBans) {
                FlexLogger.info("FlexBans system: Command would have been executed: " + command);
            } else {
                commandsExecution.executeCommand(command);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("message", "Punishment revoked successfully!");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(jsonResponse));

        } catch (JsonSyntaxException e) {
            FlexLogger.error("Error parsing JSON: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error: Invalid JSON format.");
        } catch (Exception e) {
            FlexLogger.error("Error processing request: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error: Unable to process the request.");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null || !SAFE_TEXT_PATTERN.matcher(input).matches()) {
            return null;
        }
        return input.trim();
    }

    private String buildCommand(String punishmentType, String punishmentId, String removalReason, String identity) {
        switch (punishmentType.toLowerCase()) {
            case "ban":
                return String.format("litebans:unban %s \"%s\" --sender=%s", punishmentId, removalReason, identity);
            case "mute":
                return String.format("litebans:unmute %s \"%s\" --sender=%s", punishmentId, removalReason, identity);
            case "warn":
                return String.format("litebans:unwarn %s \"%s\" --sender=%s", punishmentId, removalReason, identity);
            default:
                return null;
        }
    }
}