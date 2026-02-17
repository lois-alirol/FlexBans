package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.database.dashboard.UserManager.UserInfo;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.FlexBansPermissionLookup;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UsersHandler {
    private final UserManager userManager;
    private final UuidUsernameResolver resolver;

    private static final String PERM_PREFIX = "flexbans.";
    private static final Set<String> PROTECTED_PERMISSIONS = Set.of(
            "flexbans.web.admin",
            "flexbans.*"
    );

    public UsersHandler(UserManager userManager) {
        this.userManager = userManager;
        this.resolver = UuidUsernameResolver.get();
    }

    public CompletableFuture<ApiResponse<JsonArray>> handleAllUsersAsync(HttpServletRequest req) {
        try {
            String token = RequestUtils.extractBearerToken(req);
            if (token == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Missing authorization token"));
            }
            if (!TokenManager.isValidToken(token, "access")) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            String executor = TokenManager.getUsernameFromToken(token);
            if (executor == null || executor.isEmpty()) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            // -----------------------
            // Permission check
            // -----------------------
            UUID executorUuid = resolver.usernameToUuid(executor) == null
                    ? null
                    : resolver.usernameToUuid(executor);

            if (executorUuid == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("User not linked to Minecraft account"));
            }

            CompletableFuture<Set<String>> permsFuture = FlexBansPermissionLookup.getFlexBansPermissions(executorUuid)
                    .exceptionally(e -> {
                        FlexLogger.error("Error fetching permissions for executor " + executor + ": " + e.getMessage());
                        return Set.of();
                    });

            return permsFuture.thenCompose(perms -> {
                if (!perms.contains("flexbans.web.admin")) {
                    return CompletableFuture.completedFuture(ApiResponse.unauthorized("You don't have permission"));
                }

                // -----------------------
                // Actual users list
                // -----------------------
                List<UserInfo> users = userManager.getAllUsers();

                List<CompletableFuture<JsonObject>> futures = users.stream()
                        .map(this::buildUserJsonAsync)
                        .collect(Collectors.toList());

                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> {
                            JsonArray array = new JsonArray();
                            futures.forEach(f -> array.add(f.join()));
                            return ApiResponse.success(array);
                        });
            });
        } catch (Exception e) {
            FlexLogger.error("Unexpected error in handleAllUsersAsync: " + e.getMessage());
            return CompletableFuture.completedFuture(
                    ApiResponse.error(500, "Internal server error")
            );
        }
    }

    public CompletableFuture<ApiResponse<JsonElement>> handleUpdatePermissionsAsync(
            HttpServletRequest req,
            String username
    ) {
        try {
            String token = RequestUtils.extractBearerToken(req);
            if (token == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Missing authorization token"));
            }

            if (!TokenManager.isValidToken(token, "access")) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            String executor = TokenManager.getUsernameFromToken(token);
            if (executor == null || executor.isEmpty()) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            UUID executorUuid = resolver.usernameToUuid(executor);
            if (executorUuid == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("User not linked to Minecraft account"));
            }

            // Parse request body BEFORE async operations
            JsonObject body;
            try {
                body = RequestUtils.parseJsonBody(req);
                if (body == null) {
                    FlexLogger.error("Failed to parse request body - body is null");
                    return CompletableFuture.completedFuture(ApiResponse.badRequest("Invalid or empty request body"));
                }
            } catch (Exception e) {
                FlexLogger.error("Failed to parse request body: " + e.getMessage());
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Invalid JSON in request body"));
            }

            return FlexBansPermissionLookup.getFlexBansPermissions(executorUuid)
                    .exceptionally(e -> {
                        FlexLogger.error("Error fetching permissions for executor " + executor + ": " + e.getMessage());
                        return Set.of();
                    })
                    .thenCompose(perms -> {
                        if (!perms.contains("flexbans.web.admin")) {
                            return CompletableFuture.completedFuture(ApiResponse.unauthorized("You don't have permission"));
                        }

                        UserInfo targetUser = userManager.getUserInfo(username);
                        if (targetUser == null) {
                            return CompletableFuture.completedFuture(ApiResponse.notFound("User not found"));
                        }

                        return updatePermissionsUsingLookupAsync(targetUser, body);
                    });

        } catch (Exception e) {
            FlexLogger.error("Unexpected error in handleUpdatePermissionsAsync: " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    private CompletableFuture<ApiResponse<JsonElement>> updatePermissionsUsingLookupAsync(
            UserInfo targetUser,
            JsonObject body
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (body == null) {
                    return ApiResponse.badRequest("Invalid request body");
                }

                JsonArray add = body.has("add") ? body.getAsJsonArray("add") : new JsonArray();
                JsonArray remove = body.has("remove") ? body.getAsJsonArray("remove") : new JsonArray();

                UUID mcUuid = targetUser.minecraftUuid;
                if (mcUuid == null) {
                    return ApiResponse.badRequest("Target user is not linked to a Minecraft account");
                }

                // Track blocked actions
                Set<String> blockedAdd = new java.util.HashSet<>();
                Set<String> blockedRemove = new java.util.HashSet<>();

                // -----------------
                // ADD permissions
                // -----------------
                add.forEach(el -> {
                    try {
                        String perm = el.getAsString();

                        if (!perm.startsWith(PERM_PREFIX)) {
                            blockedAdd.add(perm);
                            return;
                        }

                        FlexBansPermissionLookup.givePermission(mcUuid, perm);
                        FlexLogger.info("Added permission '" + perm + "' to user " + targetUser.username);

                    } catch (Exception e) {
                        FlexLogger.error("Failed to add permission: " + e.getMessage());
                    }
                });

                // -----------------
                // REMOVE permissions
                // -----------------
                remove.forEach(el -> {
                    try {
                        String perm = el.getAsString();

                        if (PROTECTED_PERMISSIONS.contains(perm)) {
                            blockedRemove.add(perm);
                            return;
                        }

                        FlexBansPermissionLookup.removePermission(mcUuid, perm);
                        FlexLogger.info("Removed permission '" + perm + "' from user " + targetUser.username);

                    } catch (Exception e) {
                        FlexLogger.error("Failed to remove permission: " + e.getMessage());
                    }
                });

                // -----------------
                // If anything was blocked → return error
                // -----------------
                if (!blockedAdd.isEmpty() || !blockedRemove.isEmpty()) {
                    JsonObject errorData = new JsonObject();
                    errorData.add("blockedAdd", permissionsToJsonArray(blockedAdd));
                    errorData.add("blockedRemove", permissionsToJsonArray(blockedRemove));

                    return ApiResponse.error(
                            400,
                            "Some permissions were rejected by server rules",
                            errorData
                    );
                }

                // -----------------
                // Fetch updated permissions
                // -----------------
                Set<String> finalPerms = FlexBansPermissionLookup.getFlexBansPermissions(mcUuid)
                        .exceptionally(e -> {
                            FlexLogger.error("Error fetching permissions for user " +
                                    targetUser.username + ": " + e.getMessage());
                            return Set.of();
                        })
                        .join();

                return ApiResponse.success(permissionsToJsonArray(finalPerms));

            } catch (Exception e) {
                FlexLogger.error("Failed to update permissions: " + e.getMessage());
                return ApiResponse.error(500, "Failed to update permissions");
            }
        });
    }

    private CompletableFuture<JsonObject> buildUserJsonAsync(UserInfo user) {
        JsonObject json = new JsonObject();

        json.addProperty("id", user.id);
        json.addProperty("username", user.username);
        json.addProperty("verified", user.isVerified);
        json.addProperty("verifiedAt", user.verifiedAt);
        json.addProperty("discordId", user.discordId);

        UUID mcUuid = user.minecraftUuid;
        if (mcUuid == null) {
            json.add("permissions", new JsonArray());
            json.addProperty("prefix", "");
            return CompletableFuture.completedFuture(json);
        }

        CompletableFuture<String> prefixFuture =
                FlexBansPermissionLookup.getUserPrefix(mcUuid);

        CompletableFuture<Set<String>> permissionsFuture =
                FlexBansPermissionLookup.getFlexBansPermissions(mcUuid)
                        .exceptionally(e -> {
                            FlexLogger.error("Permission fetch failed for " + user.username + ": " + e.getMessage());
                            return Set.of();
                        });

        return prefixFuture.thenCombine(permissionsFuture, (prefix, perms) -> {
            json.addProperty("minecraftUuid", mcUuid.toString());
            json.addProperty("prefix", prefix);
            json.add("permissions", permissionsToJsonArray(perms));
            return json;
        });
    }

    private JsonArray permissionsToJsonArray(Set<String> permissions) {
        JsonArray array = new JsonArray();
        permissions.forEach(array::add);
        return array;
    }
}