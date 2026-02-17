package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.FlexBansPermissionLookup;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.response.ApiResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerResourceHandler {
    private final PlayerHeadImage playerHeadImage;
    private final UuidUsernameResolver resolver;

    public PlayerResourceHandler(PlayerHeadImage playerHeadImage) {
        this.playerHeadImage = playerHeadImage;
        this.resolver = UuidUsernameResolver.get();
    }

    public CompletableFuture<ApiResponse<JsonObject>> handlePlayerDetailsAsync(String username) {
        try {
            UUID uuid = resolver.usernameToUuid(username);
            if (uuid == null) {
                return CompletableFuture.completedFuture(ApiResponse.<JsonObject>notFound("Player UUID not found for username: " + username));
            }

            CompletableFuture<String> prefixFuture = FlexBansPermissionLookup.getUserPrefix(uuid);

            CompletableFuture<Set<String>> permissionsFuture = FlexBansPermissionLookup.getFlexBansPermissions(uuid)
                    .exceptionally(e -> {
                        FlexLogger.error("Error fetching permissions for user " + username + ": " + e.getMessage());
                        return Set.of();
                    });

            return prefixFuture.thenCombine(permissionsFuture, (prefix, permissions) -> {
                JsonObject details = new JsonObject();
                details.addProperty("username", username);
                details.addProperty("uuid", uuid.toString());
                details.addProperty("prefix", prefix);
                details.add("permissions", permissionsToJsonArray(permissions));

                return ApiResponse.<JsonObject>success(details);
            }).exceptionally(e -> {
                FlexLogger.error("Error combining player details for user " + username + ": " + e.getMessage());
                return ApiResponse.<JsonObject>error(500, "Internal server error");
            });

        } catch (Exception e) {
            FlexLogger.error("Unexpected error during handlePlayerDetailsAsync for user " + username + ": " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.<JsonObject>error(500, "Internal server error"));
        }
    }

    private JsonArray permissionsToJsonArray(Set<String> permissions) {
        JsonArray jsonArray = new JsonArray();
        for (String permission : permissions) {
            jsonArray.add(permission);
        }
        return jsonArray;
    }

    public ApiResponse<?> handlePlayerHead(String username, HttpServletResponse resp) throws IOException {
        try {
            if (username.isEmpty() || username.length() > 32) {
                return ApiResponse.badRequest("Invalid username");
            }

            playerHeadImage.getPlayerHeadUrl(username, "32");

            File playerHeadFile = new File("./plugins/FlexBans/cache/heads/" + username + ".png");
            FlexLogger.log(String.valueOf(playerHeadFile));

            if (playerHeadFile.exists() && playerHeadFile.isFile()) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("image/png");
                resp.setHeader("Cache-Control", "public, max-age=86400");

                try (FileInputStream fis = new FileInputStream(playerHeadFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        resp.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }

                resp.flushBuffer();
                return null;
            } else {
                return ApiResponse.notFound("Player head not found");
            }
        } catch (Exception e) {
            FlexLogger.error("Error fetching player head: " + e.getMessage());
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public ApiResponse<?> handlePlayerSkin(String username, HttpServletResponse resp) throws IOException {
        try {
            if (username.isEmpty() || username.length() > 32) {
                return ApiResponse.badRequest("Invalid username");
            }

            playerHeadImage.getPlayerSkinUrl(username);

            File playerSkinFile = new File("./plugins/FlexBans/cache/skins/" + username + ".png");
            FlexLogger.log("Fetching player skin: " + playerSkinFile);

            if (playerSkinFile.exists() && playerSkinFile.isFile()) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("image/png");
                resp.setHeader("Cache-Control", "public, max-age=86400");

                try (FileInputStream fis = new FileInputStream(playerSkinFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        resp.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }

                resp.flushBuffer();
                return null;
            } else {
                return ApiResponse.notFound("Player skin not found");
            }
        } catch (Exception e) {
            FlexLogger.error("Error fetching player skin: " + e.getMessage());
            return ApiResponse.error(500, "Internal server error");
        }
    }
}