package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.permissions.FlexBansPermissionLookup;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.response.ApiResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerResourceHandler {
    private static final FlexLogger LOGGER = FlexLogger.get(PlayerResourceHandler.class);
    private final Path cacheFolder;

    private final PlayerHeadImage playerHeadImage;
    private final UuidUsernameResolver resolver;

    public PlayerResourceHandler(PlayerHeadImage playerHeadImage, Path dataFolder) {
        this.playerHeadImage = playerHeadImage;
        this.cacheFolder = dataFolder.resolve("cache");

        this.resolver = UuidUsernameResolver.get();

    }

    public CompletableFuture<ApiResponse<JsonObject>> handlePlayerDetailsAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = resolver.usernameToUuid(username);
            if (uuid == null) {
                LOGGER.debug("Could not resolve UUID for player: {}", username);
            }
            return uuid;
        }).thenCompose(uuid -> {
            if (uuid == null) {
                return CompletableFuture.completedFuture(ApiResponse.<JsonObject>notFound("Player not found: " + username));
            }

            CompletableFuture<String> prefixFuture = FlexBansPermissionLookup.getUserPrefix(uuid);
            CompletableFuture<Set<String>> permsFuture = FlexBansPermissionLookup.getFlexBansPermissions(uuid)
                    .exceptionally(e -> {
                        LOGGER.error("Permission lookup failed for {}", username, e);
                        return Set.of();
                    });

            return prefixFuture.thenCombine(permsFuture, (prefix, permissions) -> {
                JsonObject details = new JsonObject();
                details.addProperty("username", username);
                details.addProperty("uuid", uuid.toString());
                details.addProperty("prefix", prefix);

                JsonArray permsArray = new JsonArray();
                permissions.forEach(permsArray::add);
                details.add("permissions", permsArray);

                return ApiResponse.success(details);
            });
        }).exceptionally(e -> {
            LOGGER.error("Unexpected error in player details for {}", username, e);
            return ApiResponse.error(500, "Internal server error");
        });
    }

    public CompletableFuture<Void> handlePlayerHead(String username, HttpServletResponse resp) {
        if (isInvalidUsername(username)) {
            sendError(resp, 400, "Invalid username");
            return CompletableFuture.completedFuture(null);
        }

        return playerHeadImage.getPlayerHeadUrl(username, 32)
                .thenAccept(urlPath -> {
                    Path file = cacheFolder.resolve("heads").resolve(username + ".png");
                    streamImageResponse(file, resp);
                })
                .exceptionally(e -> {
                    LOGGER.error("Failed to provide head for {}", username, e);
                    sendError(resp, 500, "Image processing error");
                    return null;
                });
    }

    public CompletableFuture<Void> handlePlayerSkin(String username, HttpServletResponse resp) {
        if (isInvalidUsername(username)) {
            sendError(resp, 400, "Invalid username");
            return CompletableFuture.completedFuture(null);
        }

        return playerHeadImage.getPlayerSkinUrl(username)
                .thenAccept(urlPath -> {
                    Path file = cacheFolder.resolve("skins").resolve(username + ".png");
                    streamImageResponse(file, resp);
                })
                .exceptionally(e -> {
                    LOGGER.error("Failed to provide skin for {}", username, e);
                    sendError(resp, 500, "Image processing error");
                    return null;
                });
    }

    private void streamImageResponse(Path path, HttpServletResponse resp) {
        try {
            if (!Files.exists(path)) {
                LOGGER.warn("Image file expected but not found at: {}", path.toAbsolutePath());
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not generated");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "public, max-age=86400");

            Files.copy(path, resp.getOutputStream());
            resp.flushBuffer();
        } catch (IOException e) {
            LOGGER.error("Failed to stream image: {}", path.getFileName(), e);
        }
    }

    private boolean isInvalidUsername(String username) {
        return username == null || username.isEmpty() || username.length() > 32;
    }

    private void sendError(HttpServletResponse resp, int statusCode, String message) {
        try {
            if (!resp.isCommitted()) {
                resp.sendError(statusCode, message);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to send error response: {}", message);
        }
    }
}