package fr.neocle.flexbans.web.handler;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.provider.PunishmentDataProvider;
import fr.neocle.flexbans.web.response.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PunishmentsHandler {
    private final PunishmentDataProvider dataProvider;

    private static final FlexLogger LOGGER = FlexLogger.get(PunishmentsHandler.class);

    public PunishmentsHandler(PunishmentDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public ApiResponse<?> getPunishments(int page, String type) {
        try {
            String normalizedType = null;
            int perPage;
            if (type == null || type.isBlank()) {
                perPage = getMaxPerPage(null);
            } else {
                normalizedType = type.trim().toUpperCase();
                if (!isValidPunishmentType(normalizedType)) {
                    return ApiResponse.badRequest("Invalid punishment type. Valid types: BAN, MUTE, WARNING, KICK");
                }
                perPage = getMaxPerPage(normalizedType);
            }

            int totalItems = normalizedType != null
                    ? dataProvider.getTotalPunishmentsCountByType(normalizedType)
                    : dataProvider.getTotalPunishmentsCount();
            int totalPages = (int) Math.ceil(totalItems / (double) perPage);

            int safePage = Math.max(1, Math.min(page, Math.max(1, totalPages)));

            List<Map<String, Object>> items = normalizedType != null
                    ? dataProvider.getPunishmentsPageByType(safePage, perPage, normalizedType)
                    : dataProvider.getPunishmentsPage(safePage, perPage);

            Map<String, Object> payload = new HashMap<>();
            payload.put("items", items);
            payload.put("page", safePage);
            payload.put("perPage", perPage);
            payload.put("totalItems", totalItems);
            payload.put("totalPages", totalPages);
            payload.put("countsByType", dataProvider.getGlobalCounts());

            return ApiResponse.success(payload);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch punishments: ", e);
            return ApiResponse.error(500, "Failed to fetch punishments");
        }
    }

    private boolean isValidPunishmentType(String type) {
        return "BAN".equals(type) || "MUTE".equals(type) || "WARNING".equals(type) || "KICK".equals(type);
    }

    public ApiResponse<?> getPunishmentDetails(String punishmentId) {
        try {
            if (punishmentId == null || punishmentId.isEmpty()) {
                return ApiResponse.badRequest("Punishment ID is required");
            }

            Object details = dataProvider.getPunishmentDetails(punishmentId);

            if (details == null) {
                return ApiResponse.notFound("Punishment not found");
            }

            return ApiResponse.success(details);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch punishment details for ID {}: ", e);
            return ApiResponse.error(500, "Failed to fetch punishment details");
        }
    }

    public int getMaxPerPage(String type) {
        if (type == null) {
            return Math.max(
                    Math.max(
                            ConfigManager.getInt("webserver.pages.punishments.bans.max-per-page"),
                            ConfigManager.getInt("webserver.pages.punishments.mutes.max-per-page")
                    ),
                    Math.max(
                            ConfigManager.getInt("webserver.pages.punishments.kicks.max-per-page"),
                            ConfigManager.getInt("webserver.pages.punishments.warnings.max-per-page")
                    )
            );
        }
        return switch (type.toLowerCase()) {
            case "ban" -> ConfigManager.getInt("webserver.pages.punishments.bans.max-per-page");
            case "mute" -> ConfigManager.getInt("webserver.pages.punishments.mutes.max-per-page");
            case "kick" -> ConfigManager.getInt("webserver.pages.punishments.kicks.max-per-page");
            case "warning" -> ConfigManager.getInt("webserver.pages.punishments.warnings.max-per-page");
            default -> 20;
        };
    }
}