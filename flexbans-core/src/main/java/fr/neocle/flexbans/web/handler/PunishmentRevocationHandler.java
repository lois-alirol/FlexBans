package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonObject;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;

public class PunishmentRevocationHandler {
    public ApiResponse<?> revokePunishment(HttpServletRequest req, JsonObject jsonBody) {
        if (jsonBody == null) {
            return ApiResponse.badRequest("Request body is required");
        }

        String token = RequestUtils.extractBearerToken(req);
        if (token == null) {
            return ApiResponse.unauthorized("Missing authorization token");
        }
        if (!TokenManager.isValidToken(token, "access")) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        String executor = TokenManager.getUsernameFromToken(token);
        if (executor == null || executor.isEmpty()) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        try {
            int punishmentId = Integer.parseInt(RequestUtils.getRequiredString(jsonBody, "punishmentId", 1, 256, "punishmentId"));
            String punishmentType = RequestUtils.getRequiredString(jsonBody, "punishmentType", 3, 16, "punishmentType");
            String reason = RequestUtils.getRequiredString(jsonBody, "reason", 1, 256, "reason");

            boolean silent = jsonBody.has("silent") && jsonBody.get("silent").getAsBoolean();

            if (!isValidPunishmentType(punishmentType)) {
                return ApiResponse.badRequest("Invalid punishment type");
            }

            FlexBansAPI api = FlexBansAPI.getInstance();

            switch (punishmentType.toLowerCase()) {
                case "ban":
                    api.getUnbanExecutor().executeUnban(
                            punishmentId,
                            executor,
                            "Global",
                            reason,
                            silent,
                            msg -> {}
                    );
                    break;

                case "mute":
                    api.getUnmuteExecutor().executeUnmute(
                            "punishmentId",
                            executor,
                            reason,
                            "Global",
                            silent,
                            msg -> {}
                    );
                    break;

                case "warn":
                    ApiResponse.badRequest("This type of punishment cannot be revoked (not implemented yet)");
                    break;

                case "kick":
                    ApiResponse.badRequest("This type of punishment cannot be revoked");
                    break;

                default:
                    return ApiResponse.badRequest("Unsupported punishment type");
            }


            JsonObject data = new JsonObject();
            data.addProperty("executor", executor);
            data.addProperty("punishmentId", punishmentId);
            data.addProperty("punishmentType", punishmentType);
            data.addProperty("silent", silent);
            data.addProperty("reason", reason);

            return new ApiResponse<>(201, "Punishment revoked successfully", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            FlexLogger.error("Error revoking punishment: " + e.getMessage());
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