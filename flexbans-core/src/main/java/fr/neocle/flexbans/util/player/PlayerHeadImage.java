package fr.neocle.flexbans.util.player;

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
        long oneWeekAgo = now - TimeUnit.DAYS.toMillis(7);

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

    private BufferedImage readImageFromUrl(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "image/png,image/webp,image/*,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("Server returned HTTP code: " + responseCode + " for URL: " + imageUrl);
        }

        try (var inputStream = connection.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("ImageIO returned null for URL: " + imageUrl);
            }
            return image;
        } finally {
            connection.disconnect();
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
            int sizeInt;
            try {
                sizeInt = Integer.parseInt(size);
            } catch (NumberFormatException e) {
                sizeInt = 32;
            }

            String imageUrl = "https://crafatar.com/avatars/" + uuid + "?size=" + sizeInt + "&overlay";
            return processAndCacheImage(username, imageUrl, false);
        }

        return "https://crafatar.com/avatars/" + uuid + "?size=" + size + "&overlay";
    }

    private String fetchTextureUrlFromAPI(String apiUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JsonObject jsonObject = new JsonParser().parse(response.toString()).getAsJsonObject();
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

        BufferedImage img = null;
        Exception lastException = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                img = readImageFromUrl(imageUrl);
                break;
            } catch (IOException e) {
                lastException = e;
                logger.warning("Attempt " + (attempt + 1) + " failed to download image: " + e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (img == null) {
            logger.severe("Failed to download image after 3 attempts. Using fallback image.");

            File fallbackFile = new File("plugins/FlexBans/images/fallback_head.png");

            if (!fallbackFile.exists()) {
                throw new IOException("Fallback head not found at: " + fallbackFile.getAbsolutePath(), lastException);
            }

            img = ImageIO.read(fallbackFile);

            if (img == null) {
                throw new IOException("Failed to load fallback image!", lastException);
            }
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