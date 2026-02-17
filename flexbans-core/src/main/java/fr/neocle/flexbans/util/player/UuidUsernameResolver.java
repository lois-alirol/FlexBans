package fr.neocle.flexbans.util.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.database.player.ProfilesManager;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UuidUsernameResolver {
    private static UuidUsernameResolver instance;

    private final FloodgateApi floodgateApi;
    private final ProfilesManager profilesManager;

    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    private static final UUID CONSOLE_UUID = UUID.fromString("f78a4d8d-d51b-4b39-98a3-230f2de0c670");
    private static final String PLAYER_DB_API = "https://playerdb.co/api/player/minecraft/";
    private static final String ERROR_RESPONSE = "ERROR!";
    private static final int API_TIMEOUT = 5000;
    private static final String PLAYER_FOUND_CODE = "player.found";

    private UuidUsernameResolver(ProfilesManager profilesManager) {
        this.profilesManager = profilesManager;
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public static void initialize(ProfilesManager manager) {
        if (instance == null) {
            instance = new UuidUsernameResolver(manager);
        }
    }

    public static UuidUsernameResolver get() {
        return instance;
    }

    private boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            FlexLogger.info("Floodgate is not loaded. Skipping Bedrock players usernames retrieval.");
            return false;
        }
    }

    public String uuidToUsername(UUID uuid) {
        FlexLogger.debug("[UuidResolver] Starting UUID to Username resolution for: " + uuid);

        if (uuid == null) {
            FlexLogger.warn("Invalid UUID provided.");
            return ERROR_RESPONSE;
        }

        if (uuid.equals(CONSOLE_UUID)) {
            FlexLogger.debug("[UuidResolver] Resolved as Console UUID");
            return "Console";
        }

        if (cache.containsKey(uuid)) {
            String username = cache.get(uuid);
            FlexLogger.debug("[UuidResolver] Found in memory cache: " + uuid + " -> " + username);
            return username;
        }

        String cachedUsername = profilesManager.getCurrentUsername(uuid);
        if (cachedUsername != null && !cachedUsername.isEmpty()) {
            FlexLogger.debug("[UuidResolver] Found in database cache: " + uuid + " -> " + cachedUsername);
            cache.put(uuid, cachedUsername);
            return cachedUsername;
        }

        FlexLogger.debug("[UuidResolver] Not found in cache, attempting external resolution");

        String username = resolveUsernameFromBedrock(uuid);
        if (username != null) {
            FlexLogger.debug("[UuidResolver] Resolved via Bedrock/Floodgate: " + uuid + " -> " + username);
            return username;
        }

        username = resolveUsernameFromApi(uuid);
        if (username != null) {
            FlexLogger.debug("[UuidResolver] Resolved via PlayerDB API: " + uuid + " -> " + username);
            return username;
        }

        FlexLogger.debug("[UuidResolver] Failed to resolve UUID: " + uuid);
        return ERROR_RESPONSE;
    }

    private String resolveUsernameFromBedrock(UUID uuid) {
        if (floodgateApi == null || !floodgateApi.isFloodgateId(uuid)) {
            FlexLogger.debug("[UuidResolver] Not a Bedrock player or Floodgate not available");
            return null;
        }

        FlexLogger.debug("[UuidResolver] Detected Bedrock player UUID, resolving via Floodgate");

        try {
            String uuidStr = uuid.toString().replace("-", "");
            if (uuidStr.length() < 16) {
                FlexLogger.debug("[UuidResolver] Invalid Bedrock UUID format (too short)");
                return ERROR_RESPONSE;
            }

            long xuid = Long.parseUnsignedLong(uuidStr.substring(16), 16);
            FlexLogger.debug("[UuidResolver] Extracted XUID: " + xuid);

            String gamertag = floodgateApi.getGamertagFor(xuid).get(API_TIMEOUT, TimeUnit.MILLISECONDS);
            String prefix = floodgateApi.getPlayerPrefix();
            String username = prefix + gamertag;

            FlexLogger.debug("[UuidResolver] Floodgate returned gamertag: " + gamertag + " with prefix: " + prefix);

            if (!username.startsWith("Error") && !username.equals("Player not found")) {
                cacheAndRecord(uuid, username);
                FlexLogger.debug("[UuidResolver] Successfully resolved and cached Bedrock player");
                return username;
            }

            FlexLogger.debug("[UuidResolver] Floodgate returned error or player not found");
            return ERROR_RESPONSE;
        } catch (Exception e) {
            FlexLogger.error("Failed to resolve Bedrock player: " + uuid, e);
            return ERROR_RESPONSE;
        }
    }

    private String resolveUsernameFromApi(UUID uuid) {
        FlexLogger.debug("[UuidResolver] Attempting to resolve username via PlayerDB API");

        try {
            String identifier = uuid.toString().replace("-", "");
            JsonObject jsonResponse = callPlayerDbApi(identifier);

            if (jsonResponse == null) {
                FlexLogger.warn("Failed to retrieve username for UUID: " + uuid);
                return null;
            }

            FlexLogger.debug("[UuidResolver] PlayerDB API response code: " + jsonResponse.get("code").getAsString());

            if (!PLAYER_FOUND_CODE.equals(jsonResponse.get("code").getAsString())) {
                FlexLogger.warn("Player with UUID " + uuid + " not found. Response: " + jsonResponse);
                return null;
            }

            JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");
            String username = playerData.get("username").getAsString();

            FlexLogger.debug("[UuidResolver] PlayerDB returned username: " + username);

            if (username != null && !username.startsWith("Error") && !username.equals("Player not found")) {
                cacheAndRecord(uuid, username);
                FlexLogger.debug("[UuidResolver] Successfully resolved and cached from PlayerDB");
                return username;
            }

            return null;
        } catch (IllegalArgumentException e) {
            FlexLogger.error("Invalid UUID format: " + uuid, e);
            return null;
        } catch (Exception e) {
            FlexLogger.error("Unexpected error while fetching username for UUID: " + uuid, e);
            return null;
        }
    }

    public UUID usernameToUuid(String username) {
        FlexLogger.debug("[UuidResolver] Starting Username to UUID resolution for: " + username);

        if (username == null || username.isEmpty()) {
            FlexLogger.warn("Invalid username provided.");
            return null;
        }

        if (username.equalsIgnoreCase("[CONSOLE]") ||
            username.equalsIgnoreCase("Console")) {
            return CONSOLE_UUID;
        }

        // Search cache for matching username
        for (Map.Entry<UUID, String> entry : cache.entrySet()) {
            if (entry.getValue().equals(username)) {
                FlexLogger.debug("[UuidResolver] Found in memory cache: " + username + " -> " + entry.getKey());
                return entry.getKey();
            }
        }

        UUID cachedUuid = profilesManager.getUuid(username);
        if (cachedUuid != null) {
            FlexLogger.debug("[UuidResolver] Found in database cache: " + username + " -> " + cachedUuid);
            cache.put(cachedUuid, username);
            return cachedUuid;
        }

        FlexLogger.debug("[UuidResolver] Not found in cache, attempting external resolution");

        UUID uuid = resolveUuidFromBedrock(username);
        if (uuid != null) {
            FlexLogger.debug("[UuidResolver] Resolved via Bedrock/Floodgate: " + username + " -> " + uuid);
            return uuid;
        }

        uuid = resolveUuidFromApi(username);
        if (uuid != null) {
            FlexLogger.debug("[UuidResolver] Resolved via PlayerDB API: " + username + " -> " + uuid);
        } else {
            FlexLogger.debug("[UuidResolver] Failed to resolve username: " + username);
        }

        return uuid;
    }

    private UUID resolveUuidFromBedrock(String username) {
        if (floodgateApi == null || !username.startsWith(floodgateApi.getPlayerPrefix())) {
            FlexLogger.debug("[UuidResolver] Not a Bedrock player or Floodgate not available");
            return null;
        }

        FlexLogger.debug("[UuidResolver] Detected Bedrock player username, resolving via Floodgate");

        try {
            String gamertag = username.substring(floodgateApi.getPlayerPrefix().length());
            FlexLogger.debug("[UuidResolver] Extracted gamertag: " + gamertag);

            CompletableFuture<UUID> uuidFuture = floodgateApi.getUuidFor(gamertag);

            if (uuidFuture == null) {
                FlexLogger.debug("[UuidResolver] Floodgate returned null future");
                return null;
            }

            UUID playerUUID = uuidFuture.get(API_TIMEOUT, TimeUnit.MILLISECONDS);

            if (playerUUID != null) {
                FlexLogger.debug("[UuidResolver] Floodgate returned UUID: " + playerUUID);
                cacheAndRecord(playerUUID, username);
                FlexLogger.debug("[UuidResolver] Successfully resolved and cached Bedrock player");
                return playerUUID;
            } else {
                FlexLogger.warn("Floodgate API couldn't find the UUID for player: " + username);
                return null;
            }
        } catch (Exception e) {
            FlexLogger.error("Failed to resolve Bedrock UUID for username: " + username, e);
            return null;
        }
    }

    private UUID resolveUuidFromApi(String username) {
        FlexLogger.debug("[UuidResolver] Attempting to resolve UUID via PlayerDB API");

        try {
            JsonObject jsonResponse = callPlayerDbApi(username);

            if (jsonResponse == null) {
                FlexLogger.debug("[UuidResolver] PlayerDB API returned null response");
                return null;
            }

            FlexLogger.debug("[UuidResolver] PlayerDB API response code: " + jsonResponse.get("code").getAsString());

            if (!PLAYER_FOUND_CODE.equals(jsonResponse.get("code").getAsString())) {
                FlexLogger.debug("[UuidResolver] Player not found in PlayerDB");
                return null;
            }

            JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");
            String uuidStr = playerData.get("id").getAsString();

            FlexLogger.debug("[UuidResolver] PlayerDB returned UUID: " + uuidStr);

            if (uuidStr != null && !uuidStr.startsWith("Error") && !uuidStr.equals("UUID not found")) {
                UUID uuid = UUID.fromString(uuidStr);
                cacheAndRecord(uuid, username);
                FlexLogger.debug("[UuidResolver] Successfully resolved and cached from PlayerDB");
                return uuid;
            }

            return null;
        } catch (Exception e) {
            FlexLogger.error("Failed to resolve UUID for username: " + username, e);
            return null;
        }
    }

    private JsonObject callPlayerDbApi(String identifier) throws IOException {
        String apiUrl = PLAYER_DB_API + identifier;
        FlexLogger.debug("[UuidResolver] Calling PlayerDB API: " + apiUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(API_TIMEOUT);
        connection.setReadTimeout(API_TIMEOUT);

        try {
            int status = connection.getResponseCode();
            FlexLogger.debug("[UuidResolver] PlayerDB API HTTP status: " + status);

            if (status != 200) {
                FlexLogger.warn("Failed to retrieve data from PlayerDB API. HTTP response code: " + status);
                return null;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                FlexLogger.debug("[UuidResolver] PlayerDB API raw response: " + response.toString());
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
        } finally {
            connection.disconnect();
        }
    }

    private void cacheAndRecord(UUID uuid, String username) {
        FlexLogger.debug("[UuidResolver] Caching and recording: " + uuid + " <-> " + username);
        cache.put(uuid, username);
        profilesManager.recordUsername(uuid, username);
        FlexLogger.debug("[UuidResolver] Cache size: " + cache.size());
    }
}