package fr.neocle.flexbans.util.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.network.HttpClientProvider;
import org.geysermc.floodgate.api.FloodgateApi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UuidUsernameResolver {
    private static UuidUsernameResolver instance;

    private static final FlexLogger LOGGER = FlexLogger.get(UuidUsernameResolver.class);
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;


    private final FloodgateApi floodgateApi;
    private final ProfilesManager profilesManager;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    private static final UUID CONSOLE_UUID = UUID.fromString("f78a4d8d-d51b-4b39-98a3-230f2de0c670");
    private static final String PLAYER_DB_API = "https://playerdb.co/api/player/minecraft/";
    private static final String ERROR_RESPONSE = "ERROR!";
    private static final int API_TIMEOUT_MS = 5000;
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
            LOGGER.info("Floodgate not detected. Bedrock support disabled.");
            return false;
        }
    }

    public String uuidToUsername(UUID uuid) {
        if (uuid == null) {
            LOGGER.warn("Attempted to resolve a null UUID.");
            return ERROR_RESPONSE;
        }

        if (uuid.equals(CONSOLE_UUID)) {
            return "Console";
        }

        if (cache.containsKey(uuid)) {
            String username = cache.get(uuid);
            LOGGER.debug("Memory cache hit: {} -> {}", uuid, username);
            return username;
        }

        String cachedUsername = null;
        try {
            cachedUsername = profilesManager.getCurrentUsername(uuid).join();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch current username from DB for {}: ", uuid, e);
        }

        if (cachedUsername != null && !cachedUsername.isEmpty()) {
            LOGGER.debug("Database cache hit: {} -> {}", uuid, cachedUsername);
            cache.put(uuid, cachedUsername);
            return cachedUsername;
        }

        LOGGER.debug("Cache miss for {}. Attempting external resolution...", uuid);

        String username = resolveUsernameFromBedrock(uuid);
        if (username != null) return username;

        username = resolveUsernameFromApi(uuid);
        if (username != null) return username;

        LOGGER.warn("Failed to resolve username for UUID: {}", uuid);
        return ERROR_RESPONSE;
    }

    private String resolveUsernameFromBedrock(UUID uuid) {
        if (floodgateApi == null || !floodgateApi.isFloodgateId(uuid)) return null;

        try {
            String uuidStr = uuid.toString().replace("-", "");
            if (uuidStr.length() < 16) return ERROR_RESPONSE;

            long xuid = Long.parseUnsignedLong(uuidStr.substring(16), 16);
            String gamertag = floodgateApi.getGamertagFor(xuid).get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            String username = floodgateApi.getPlayerPrefix() + gamertag;

            if (!username.startsWith("Error") && !username.equals("Player not found")) {
                cacheAndRecord(uuid, username);
                LOGGER.debug("Resolved Bedrock user: {} -> {}", uuid, username);
                return username;
            }
        } catch (Exception e) {
            LOGGER.error("Floodgate resolution failed for {}", uuid, e);
        }
        return null;
    }

    private String resolveUsernameFromApi(UUID uuid) {
        try {
            String identifier = uuid.toString().replace("-", "");
            JsonObject json = callPlayerDbApi(identifier);

            if (json == null || !PLAYER_FOUND_CODE.equals(json.get("code").getAsString())) {
                return null;
            }

            String username = json.getAsJsonObject("data")
                    .getAsJsonObject("player")
                    .get("username").getAsString();

            if (username != null) {
                cacheAndRecord(uuid, username);
                LOGGER.debug("Resolved via PlayerDB: {} -> {}", uuid, username);
                return username;
            }
        } catch (Exception e) {
            LOGGER.error("API resolution failed for UUID: {}", uuid, e);
        }
        return null;
    }

    public UUID usernameToUuid(String username) {
        if (username == null || username.isEmpty()) return null;
        if (username.equalsIgnoreCase("Console") || username.equalsIgnoreCase("[CONSOLE]")) {
            return CONSOLE_UUID;
        }

        for (Map.Entry<UUID, String> entry : cache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(username)) {
                return entry.getKey();
            }
        }

        UUID cachedUuid = null;
        try {
            cachedUuid = profilesManager.getUuid(username).join();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch current username from DB for {}: ", username, e);
        }

        UUID uuid = resolveUuidFromBedrock(username);
        if (uuid == null) uuid = resolveUuidFromApi(username);

        return uuid;
    }

    private UUID resolveUuidFromBedrock(String username) {
        if (floodgateApi == null || !username.startsWith(floodgateApi.getPlayerPrefix())) return null;

        try {
            String gamertag = username.substring(floodgateApi.getPlayerPrefix().length());
            CompletableFuture<UUID> future = floodgateApi.getUuidFor(gamertag);

            if (future == null) return null;

            UUID uuid = future.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (uuid != null) {
                cacheAndRecord(uuid, username);
                return uuid;
            }
        } catch (Exception e) {
            LOGGER.error("Floodgate UUID resolution failed for {}", username, e);
        }
        return null;
    }

    private UUID resolveUuidFromApi(String username) {
        try {
            JsonObject json = callPlayerDbApi(username);
            if (json == null || !PLAYER_FOUND_CODE.equals(json.get("code").getAsString())) return null;

            String uuidStr = json.getAsJsonObject("data")
                    .getAsJsonObject("player")
                    .get("id").getAsString();

            UUID uuid = UUID.fromString(uuidStr);
            cacheAndRecord(uuid, username);
            return uuid;
        } catch (Exception e) {
            LOGGER.error("API UUID resolution failed for {}", username, e);
        }
        return null;
    }

    private JsonObject callPlayerDbApi(String identifier) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PLAYER_DB_API + identifier))
                    .timeout(Duration.ofMillis(API_TIMEOUT_MS))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warn("PlayerDB returned status {}: {}", response.statusCode(), identifier);
                return null;
            }

            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Network error while calling PlayerDB for {}", identifier, e);
            return null;
        }
    }

    private void cacheAndRecord(UUID uuid, String username) {
        cache.put(uuid, username);
        profilesManager.recordUsername(uuid, username);
        LOGGER.debug("Saved to cache & DB: {} <-> {} (Cache size: {})", uuid, username, cache.size());
    }
}