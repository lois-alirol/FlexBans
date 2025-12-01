package fr.neocle.flexbans.utils.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.geysermc.floodgate.api.FloodgateApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UsernameUUIDConverters {
    private static final Logger logger = Logger.getLogger("FlexBans");
    private final FloodgateApi floodgateApi;

    private final Map<String, String> uuidToUsernameCache = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToUUIDCache = new ConcurrentHashMap<>();

    public UsernameUUIDConverters() {
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    private boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            logger.info("Floodgate is not loaded. Skipping Bedrock players usernames retrieval.");
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public String UUIDtoUsername(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            logger.warning("Invalid UUID provided.");
            return "Invalid UUID";
        }

        if (uuidToUsernameCache.containsKey(uuid)) {
            return uuidToUsernameCache.get(uuid);
        }

        if ("[CONSOLE]".equalsIgnoreCase(uuid) || "Console".equalsIgnoreCase(uuid)) {
            return "Console";
        }

        String username;

        try {
            UUID playerUUID = UUID.fromString(uuid);

            if (floodgateApi != null && floodgateApi.isFloodgateId(playerUUID)) {
                String xuidStr = uuid.replaceAll("-", "").substring(16);
                if (!xuidStr.isEmpty()) {
                    try {
                        long xuid = Long.parseLong(xuidStr, 16);
                        String gamertag = floodgateApi.getGamertagFor(xuid).get();
                        String prefix = floodgateApi.getPlayerPrefix();
                        username = prefix + gamertag;

                        if (username != null && !username.startsWith("Error") && !username.equals("Player not found")) {
                            uuidToUsernameCache.put(uuid, username);
                            usernameToUUIDCache.put(username, uuid);
                        }

                        return username;
                    } catch (Exception e) {
                        return "Error fetching Bedrock player username";
                    }
                }
                return "XUID not found for Bedrock player";
            }

            String apiUrl = "https://playerdb.co/api/player/minecraft/" + uuid.replace("-", "");
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
                    if ("player.found".equals(jsonResponse.get("code").getAsString())) {
                        JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");
                        username = playerData.get("username").getAsString();

                        if (username != null && !username.startsWith("Error") && !username.equals("Player not found")) {
                            uuidToUsernameCache.put(uuid, username);
                            usernameToUUIDCache.put(username, uuid);
                        }

                        return username;
                    } else {
                        logger.warning("Player with UUID " + uuid + " not found. Response: " + jsonResponse);
                        return "Player not found";
                    }
                }
            } else {
                logger.warning("Failed to retrieve username. HTTP response code: " + status);
                return "Error retrieving player data";
            }
        } catch (IllegalArgumentException e) {
            logger.severe("Invalid UUID format: " + uuid);
            return "Invalid UUID format";
        } catch (Exception e) {
            logger.severe("Unexpected error while fetching username for UUID: " + uuid);
            e.printStackTrace();
            return "Error fetching data";
        }
    }

    @SuppressWarnings("deprecation")
    public String usernameToUUID(String username) {
        if (username == null || username.isEmpty()) {
            logger.warning("Invalid username provided.");
            return "Invalid username";
        }

        if (usernameToUUIDCache.containsKey(username)) {
            return usernameToUUIDCache.get(username);
        }

        if (floodgateApi != null && username.startsWith(floodgateApi.getPlayerPrefix())) {
            CompletableFuture<UUID> uuid = floodgateApi.getUuidFor(username.substring(floodgateApi.getPlayerPrefix().length()));
            if (uuid != null) {
                try {
                    UUID playerUUID = uuid.get();
                    if (playerUUID != null) {

                        usernameToUUIDCache.put(username, playerUUID.toString());
                        uuidToUsernameCache.put(playerUUID.toString(), username);

                        return playerUUID.toString();
                    } else {
                        logger.warning("Floodgate API couldn't find the UUID for player: " + username);
                        return "UUID not found in Floodgate";
                    }
                } catch (Exception e) {
                    return "Error fetching UUID from Floodgate";
                }
            }
        }

        String apiUrl = "https://playerdb.co/api/player/minecraft/" + username;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();

                if (jsonResponse.get("code").getAsString().equals("player.found")) {
                    JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");

                    if (playerData.get("id").getAsString() != null && !playerData.get("id").getAsString().startsWith("Error") && !playerData.get("id").getAsString().equals("UUID not found")) {
                        usernameToUUIDCache.put(username, playerData.get("id").getAsString());
                        uuidToUsernameCache.put(playerData.get("id").getAsString(), username);
                    }

                    return playerData.get("id").getAsString();
                } else {
                    return "UUID not found";
                }
            } else {
                return "Error retrieving player data";
            }

        } catch (Exception e) {
            return "Error fetching data";
        }
    }
}