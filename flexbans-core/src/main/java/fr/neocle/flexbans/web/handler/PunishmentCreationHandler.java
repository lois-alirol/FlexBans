package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonObject;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;

public class PunishmentCreationHandler {
    private static FlexLogger LOGGER = FlexLogger.get(PunishmentCreationHandler.class);

    public ApiResponse<?> createPunishment(HttpServletRequest req, JsonObject jsonBody) {
        if (jsonBody == null) {
            return ApiResponse.badRequest("Request body is required");
        }

        String token = RequestUtils.extractBearerToken(req);
        if (token == null) {
            return ApiResponse.unauthorized("Missing authorization token");
        }
        if (!TokenManager.get().isValidToken(token, "access")) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        String executor = TokenManager.get().getUsernameFromToken(token);
        if (executor == null || executor.isEmpty()) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        try {
            String target = RequestUtils.getRequiredString(jsonBody, "target", 1, 32, "target");
            String punishmentType = RequestUtils.getRequiredString(jsonBody, "punishmentType", 3, 16, "punishmentType");
            String reason = RequestUtils.getRequiredString(jsonBody, "reason", 1, 256, "reason");

            boolean silent = jsonBody.has("silent") && jsonBody.get("silent").getAsBoolean();
            boolean permanent = jsonBody.has("permanent") && jsonBody.get("permanent").getAsBoolean();
            String duration = jsonBody.has("duration") && !jsonBody.get("duration").isJsonNull()
                    ? jsonBody.get("duration").getAsString()
                    : "";

            if (!isValidPunishmentType(punishmentType)) {
                return ApiResponse.badRequest("Invalid punishment type");
            }

            if (!"kick".equalsIgnoreCase(punishmentType) && !permanent) {
                if (duration == null || duration.isBlank()) {
                    return ApiResponse.badRequest("Duration is required unless punishment is permanent or type is kick");
                }
            } else {
                duration = "";
            }

            FlexBansAPI api = FlexBansAPI.getInstance();

            switch (punishmentType.toLowerCase()) {
                case "ban":
                    api.getBanExecutor().executeBan(
                            target,
                            executor,
                            duration,
                            reason,
                            "Global",
                            "Proxy",
                            silent,
                            false,
                            msg -> {}
                    );
                    break;

                case "mute":
                    api.getMuteExecutor().executeMute(
                            target,
                            executor,
                            duration,
                            reason,
                            "Global",
                            "Proxy",
                            silent,
                            false,
                            msg -> {}
                    );
                    break;

                case "warn":
                    api.getWarningExecutor().executeWarning(
                            target,
                            executor,
                            reason,
                            "Global",
                            "Proxy",
                            silent,
                            false,
                            msg -> {}
                    );
                    break;

                case "kick":
                    api.getKickExecutor().executeKick(
                            target,
                            executor,
                            reason,
                            "Proxy",
                            silent,
                            false,
                            msg -> {}
                    );
                    break;

                default:
                    return ApiResponse.badRequest("Unsupported punishment type");
            }


            JsonObject data = new JsonObject();
            data.addProperty("executor", executor);
            data.addProperty("target", target);
            data.addProperty("punishmentType", punishmentType);
            data.addProperty("silent", silent);
            data.addProperty("reason", reason);
            data.addProperty("duration", duration);
            data.addProperty("permanent", permanent);

            return new ApiResponse<>(201, "Punishment applied successfully", data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error creating punishment: ", e);
            return ApiResponse.error(500, "Unable to process the request");
        }
    }

    private boolean isValidPunishmentType(String type) {
        return "ban".equalsIgnoreCase(type) ||
                "mute".equalsIgnoreCase(type) ||
                "warn".equalsIgnoreCase(type) ||
                "kick".equalsIgnoreCase(type);
    }
}