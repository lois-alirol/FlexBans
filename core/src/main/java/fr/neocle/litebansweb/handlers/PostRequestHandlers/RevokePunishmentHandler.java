package fr.neocle.litebansweb.handlers.PostRequestHandlers;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.JSONObject;

import fr.neocle.litebansweb.utils.CommandsExecution.CommandsExecution;

public class RevokePunishmentHandler extends AbstractHandler {
    private final Logger logger = Logger.getLogger("RevokePunishmentHandler");
    private final CommandsExecution commandsExecution;

    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\- ]{1,255}$");

    public RevokePunishmentHandler(CommandsExecution commandsExecution) {
        this.commandsExecution = commandsExecution;
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
            logger.warning("Error reading request body: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Error: Invalid request payload.");
            return;
        }

        try {
            JSONObject jsonPayload = new JSONObject(stringBuilder.toString());
            String identity = jsonPayload.optString("identity");
            String removalReason = sanitizeInput(jsonPayload.optString("removalReason"));
            String punishmentType = sanitizeInput(jsonPayload.optString("punishmentType"));
            String punishmentId = sanitizeInput(jsonPayload.optString("punishmentId"));

            if (identity == null || removalReason == null || punishmentType == null || punishmentId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Error: Invalid input.");
                return;
            }

            String command = buildCommand(punishmentType, punishmentId, removalReason, identity);
            if (command == null) {
                logger.warning("Invalid punishment type: " + punishmentType);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Error: Invalid punishment type.");
                return;
            }

            commandsExecution.executeCommand(command);

            response.setStatus(HttpServletResponse.SC_OK);
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", "Punishment revoked successfully!");
            response.setContentType("application/json");
            response.getWriter().write(jsonResponse.toString());

        } catch (Exception e) {
            logger.severe("Error processing request: " + e.getMessage());
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
