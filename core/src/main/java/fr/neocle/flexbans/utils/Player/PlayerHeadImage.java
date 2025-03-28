package fr.neocle.flexbans.utils.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geysermc.floodgate.api.FloodgateApi;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlayerHeadImage {
    private static final Logger logger = Logger.getLogger("FlexBans");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private static Path cacheDirectory;
    private final FloodgateApi floodgateApi;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public PlayerHeadImage(UsernameUUIDConverters usernameUUIDConverters, File pluginFolder) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
        cacheDirectory = Paths.get(pluginFolder.getAbsolutePath(), "cache", "heads");

        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }

        scheduler.scheduleAtFixedRate(this::cleanUpCache, 0, 6, TimeUnit.HOURS);
    }

    public void cleanUpCache() {
        File cacheDir = cacheDirectory.toFile();

        if (!cacheDir.exists() || !cacheDir.isDirectory()) {
            logger.warning("Cache directory does not exist or is not a directory.");
            return;
        }

        File[] files = cacheDir.listFiles();
        if (files == null) return;

        long now = System.currentTimeMillis();
        long oneWeekAgo = now - TimeUnit.DAYS.toMillis(7); // 7 days

        for (File file : files) {
            if (file.isFile() && file.lastModified() < oneWeekAgo) {
                if (!file.delete()) {
                    logger.warning("Failed to delete old cached file: " + file.getName());
                } else {
                    logger.info("Deleted old cached file: " + file.getName());
                }
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
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

    public String getPlayerHeadUrl(String username, String size) throws Exception {
        String uuidstr = usernameUUIDConverters.usernameToUUID(username);
        UUID uuid = UUID.fromString(uuidstr);
        Path cachedImagePath = getCachedImagePath(username);

        if (Files.exists(cachedImagePath)) {
            return "/player-heads/" + username;
        }

        if (floodgateApi != null && floodgateApi.isFloodgateId(uuid)) {
            username = username.startsWith(floodgateApi.getPlayerPrefix()) ?
                    username.substring(floodgateApi.getPlayerPrefix().length()) : username;

            String xuidStr = uuid.toString().replaceAll("-", "").substring(16);
            if (!xuidStr.isEmpty()) {
                long xuidDecimal = Long.parseLong(xuidStr, 16);
                String apiUrl = "https://api.geysermc.org/v2/skin/" + xuidDecimal;
                String imageUrl = fetchTextureUrlFromAPI(apiUrl);

                if (imageUrl != null) {
                    return processAndCacheImage(floodgateApi.getPlayerPrefix() + username, imageUrl, true);
                }
            }
        } else {
            String imageUrl = "https://mc-heads.net/avatar/" + uuid + "/" + size + ".png";
            return processAndCacheImage(username, imageUrl, false);
        }

        return "https://mc-heads.net/avatar/" + uuid + "/" + size + ".png";
    }

    private String fetchTextureUrlFromAPI(String apiUrl) {
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

                JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonElement textureIdElement = jsonObject.get("texture_id");

                if (textureIdElement != null && textureIdElement.isJsonPrimitive()) {
                    return "https://textures.minecraft.net/texture/" + textureIdElement.getAsString();
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to retrieve Bedrock player skin from API: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }


    private Path getCachedImagePath(String username) {
        return cacheDirectory.resolve(username + ".png");
    }

    @SuppressWarnings("deprecation")
    public String processAndCacheImage(String username, String imageUrl, boolean bedrockHead) throws IOException {
        Path cachedImagePath = getCachedImagePath(username);

        if (Files.exists(cachedImagePath)) {
            return "/player-heads/" + username;
        }

        BufferedImage img = ImageIO.read(new URL(imageUrl));
        if (img == null) {
            throw new IOException("Failed to download or read image from URL: " + imageUrl);
        }

        if (bedrockHead) {
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
        } else {
            ImageIO.write(img, "PNG", cachedImagePath.toFile());
        }
        return "/player-heads/" + username;
    }
}
