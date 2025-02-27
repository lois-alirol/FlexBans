package fr.neocle.litebansweb.utils.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.floodgate.api.FloodgateApi;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerHeadImage {
    private static final Logger logger = Logger.getLogger("LitebansWeb");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private static Path cacheDirectory;
    private final FloodgateApi floodgateApi;
    
    public PlayerHeadImage(UsernameUUIDConverters usernameUUIDConverters, File pluginFolder) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
        cacheDirectory = Paths.get(pluginFolder.getAbsolutePath(), "cache", "heads");

        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }
    }

    private boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            logger.info("Floodgate is not loaded. Skipping Bedrock players head retrieval.");
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public String getPlayerHeadUrl(String username, String size) throws Exception {
        String uuidstr = usernameUUIDConverters.usernameToUUID(username);
        UUID uuid = UUID.fromString(uuidstr);

        if (floodgateApi != null && floodgateApi.isFloodgateId(uuid)) {
            Path cachedImagePath = getCachedImagePath(username);
            username = username.startsWith(floodgateApi.getPlayerPrefix()) ? username.substring(floodgateApi.getPlayerPrefix().length()) : username;

            if (Files.exists(cachedImagePath)) {
                return "/player-heads/" + username;
            } else {
                String xuidStr = uuid.toString().replaceAll("-", "").substring(16);
    
                if (!xuidStr.isEmpty()) {
                    long xuidDecimal = Long.parseLong(xuidStr, 16);
                    String apiUrl = "https://api.geysermc.org/v2/skin/" + xuidDecimal;
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                        connection.setRequestMethod("GET");
                        int responseCode = connection.getResponseCode();
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
    
                            String responseString = response.toString();
    
                            JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
                            JsonElement textureIdElement = jsonObject.get("texture_id");
    
                            if (textureIdElement != null && textureIdElement.isJsonPrimitive()) {
                                String textureId = textureIdElement.getAsString();
                                String imageUrl = "https://textures.minecraft.net/texture/" + textureId;
    
                                return processAndCacheImage(username, imageUrl);
                            } else {
                                logger.warning("texture_id not found in the response.");
                            }
                        } else {
                            logger.warning("Failed to retrieve Bedrock player skin from API. Response code: " + responseCode);
                        }
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
            }
        }
    
        return "https://mc-heads.net/avatar/" + username + "/" + size + ".png";
    }
    

    private Path getCachedImagePath(String username) {
        return cacheDirectory.resolve(username + ".png");
    }

    @SuppressWarnings("deprecation")
    public String processAndCacheImage(String username, String imageUrl) throws IOException {
        Path cachedImagePath = getCachedImagePath(username);
    
        if (Files.exists(cachedImagePath)) {
            return "/player-heads/" + username;
        }
    
        BufferedImage img = ImageIO.read(new URL(imageUrl));
        if (img == null) {
            throw new IOException("Failed to download or read image from URL: " + imageUrl);
        }
    
        int width = img.getWidth();
        int height = img.getHeight();
    
        if (width < 64 || height < 64) {
            throw new IOException("Image is too small to crop the required areas.");
        }
    
        BufferedImage croppedImage = img.getSubimage(8, 8, 8, 8);
    
        BufferedImage resizedImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(croppedImage, 0, 0, 32, 32, null);
        g2d.dispose();
    
        ImageIO.write(resizedImage, "PNG", cachedImagePath.toFile());
    
        return "/player-heads/" + username;
    }        
}
