package fr.neocle.litebansweb.utils.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class UsernameUUIDConverters {
    private static final Logger logger = Logger.getLogger("LitebansWeb");
    private final FloodgateApi floodgateApi;

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

        if ("[CONSOLE]".equalsIgnoreCase(uuid)) {
            return "Console";
        }

        try {
            UUID playerUUID = UUID.fromString(uuid);
    
            if (floodgateApi != null && floodgateApi.isFloodgateId(playerUUID)) {
                String xuidStr = uuid.replaceAll("-", "").substring(16);
                if (!xuidStr.isEmpty()) {
                    try {
                        long xuid = Long.parseLong(xuidStr, 16);
                        String gamertag = floodgateApi.getGamertagFor(xuid).get();
                        return floodgateApi.getPlayerPrefix() + gamertag;
                    } catch (Exception e) {
                        logger.severe("Error fetching Bedrock player username for XUID: " + xuidStr);
                        e.printStackTrace();
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
    
                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    if ("player.found".equals(jsonResponse.get("code").getAsString())) {
                        JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");
                        return playerData.get("username").getAsString();
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
        
        if(floodgateApi != null && username.startsWith(floodgateApi.getPlayerPrefix())) {
            CompletableFuture<UUID> uuid = floodgateApi.getUuidFor(username.substring(floodgateApi.getPlayerPrefix().length()));
            if (uuid != null) {
                try {
                    UUID playerUUID = uuid.get();
                    if (playerUUID != null) {
                        return playerUUID.toString();
                    } else {
                        logger.warning("Floodgate API couldn't find the UUID for player: " + username);
                        return "UUID not found in Floodgate";
                    }
                } catch (Exception e) {
                    logger.severe("Error while fetching UUID from Floodgate API: " + e.getMessage());
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

                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();

                if (jsonResponse.get("code").getAsString().equals("player.found")) {
                    JsonObject playerData = jsonResponse.getAsJsonObject("data").getAsJsonObject("player");
                    return playerData.get("id").getAsString();
                } else {
                    logger.warning("Player with username " + username + " not found.");
                    return "UUID not found";
                }
            } else {
                logger.warning("Failed to retrieve UUID. HTTP response code: " + status);
                return "Error retrieving player data";
            }

        } catch (Exception e) {
            logger.severe("Error while fetching UUID for username " + username);
            e.printStackTrace();
            return "Error fetching data";
        }
    }
}
