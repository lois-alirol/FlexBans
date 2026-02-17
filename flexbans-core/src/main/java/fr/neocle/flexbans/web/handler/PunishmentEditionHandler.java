package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class PunishmentEditionHandler {

    private final PunishmentsManager punishmentsManager;

    public PunishmentEditionHandler(DatabaseUtils databaseUtils) {
        this.punishmentsManager = databaseUtils.getPunishmentsManager();
    }

    public ApiResponse<?> editPunishment(HttpServletRequest req, JsonObject jsonBody) {
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

        String executorName = TokenManager.getUsernameFromToken(token);
        if (executorName == null || executorName.isEmpty()) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        UUID executorUuid = UuidUsernameResolver.get().usernameToUuid(executorName);
        if (executorUuid == null) {
            return ApiResponse.unauthorized("Invalid or expired token");
        }

        try {
            long punishmentId = Long.parseLong(RequestUtils.getRequiredString(jsonBody, "punishmentId", 1, 256, "punishmentId"));
            String newReason = RequestUtils.getRequiredString(jsonBody, "reason", 1, 256, "reason");
            long newDuration = Long.parseLong(RequestUtils.getRequiredString(jsonBody, "duration", 1, 256, "duration"));

            boolean updated = punishmentsManager.updatePunishment(
                    punishmentId,
                    executorUuid,
                    executorName,
                    newReason,
                    newDuration
            );

            if (!updated) {
                return ApiResponse.notFound("Punishment not found or not active");
            }

            JsonObject data = new JsonObject();
            data.addProperty("punishmentId", punishmentId);
            data.addProperty("updatedBy", executorName);
            data.addProperty("reason", newReason);
            data.addProperty("duration", newDuration);

            return new ApiResponse<>(200, "Punishment updated successfully", data);

        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            FlexLogger.error("Error editing punishment: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error(500, "Unable to process the request");
        }
    }
}
