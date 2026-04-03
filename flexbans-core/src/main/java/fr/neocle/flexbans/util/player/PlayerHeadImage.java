package fr.neocle.flexbans.util.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.network.HttpClientProvider;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;
import org.geysermc.floodgate.api.FloodgateApi;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayerHeadImage {
    private static final FlexLogger LOGGER = FlexLogger.get(PlayerHeadImage.class);
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;

    private final UuidUsernameResolver resolver;
    private final FloodgateApi floodgateApi;
    private final Path headCache;
    private final Path skinCache;

    public PlayerHeadImage(File pluginFolder) {
        this.resolver = UuidUsernameResolver.get();
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;

        this.headCache = pluginFolder.toPath().resolve("cache/heads");
        this.skinCache = pluginFolder.toPath().resolve("cache/skins");

        initDirectories();

        TaskScheduler.get().runRepeating(this::cleanUpCache, 21600000);
    }

    private void initDirectories() {
        try {
            Files.createDirectories(headCache);
            Files.createDirectories(skinCache);
        } catch (IOException e) {
            LOGGER.error("Could not create image cache directories", e);
        }
    }

    public CompletableFuture<String> getPlayerHeadUrl(String username, int size) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = resolver.usernameToUuid(username);
                Path cached = headCache.resolve(username + ".png");

                if (Files.exists(cached)) return "/player-heads/" + username;

                boolean isBedrock = floodgateApi != null && floodgateApi.isFloodgateId(uuid);
                String imageUrl = isBedrock
                        ? fetchBedrockTextureUrl(uuid).join()
                        : "https://mc-heads.net/avatar/" + uuid + "/" + size;

                if (imageUrl == null) return "/fallback-head";

                return processAndCache(username, imageUrl, cached, isBedrock).join();
            } catch (Exception e) {
                LOGGER.error("Error providing head URL for {}", username, e);
                return "/fallback-head";
            }
        });
    }

    public CompletableFuture<String> getPlayerSkinUrl(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = resolver.usernameToUuid(username);
                Path cached = skinCache.resolve(username + ".png");

                if (Files.exists(cached)) return "/player-skins/" + username;

                boolean isBedrock = floodgateApi != null && floodgateApi.isFloodgateId(uuid);
                String imageUrl = isBedrock
                        ? fetchBedrockTextureUrl(uuid).join()
                        : "https://mc-heads.net/skin/" + uuid;

                if (imageUrl == null) return "/fallback-skin";

                return processAndCache(username, imageUrl, cached, false).join();
            } catch (Exception e) {
                LOGGER.error("Error providing skin URL for {}", username, e);
                return "/fallback-skin";
            }
        });
    }

    private CompletableFuture<String> fetchBedrockTextureUrl(UUID uuid) {
        long xuid = Long.parseUnsignedLong(uuid.toString().replace("-", "").substring(16), 16);
        String url = "https://api.geysermc.org/v2/skin/" + xuid;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) return null;
                    JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                    return json.has("texture_id")
                            ? "https://textures.minecraft.net/texture/" + json.get("texture_id").getAsString()
                            : null;
                }).exceptionally(ex -> {
                    LOGGER.warn("Geyser API unreachable for XUID: {}", xuid);
                    return null;
                });
    }

    private CompletableFuture<String> processAndCache(String name, String url, Path targetPath, boolean shouldCropHead) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage img = downloadImage(url);
                if (img == null) throw new IOException("Download failed");

                if (shouldCropHead) {
                    img = img.getSubimage(8, 8, 8, 8);
                    BufferedImage resized = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = resized.createGraphics();
                    g.drawImage(img, 0, 0, 32, 32, null);
                    g.dispose();
                    img = resized;
                }

                ImageIO.write(img, "PNG", targetPath.toFile());

                return targetPath.startsWith(headCache) ? "/player-heads/" + name : "/player-skins/" + name;
            } catch (Exception e) {
                LOGGER.error("Failed to process image for {} at {}", name, url, e);
                return "/fallback";
            }
        });
    }

    private BufferedImage downloadImage(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "FlexBans-Web-Client/1.0")
                .GET()
                .build();

        for (int i = 0; i < 3; i++) {
            try {
                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    try (var is = new ByteArrayInputStream(response.body())) {
                        return ImageIO.read(is);
                    }
                }
            } catch (IOException e) {
                if (i == 2) throw e;
                Thread.sleep(500);
            }
        }
        return null;
    }

    public void cleanUpCache() {
        cleanDir(headCache);
        cleanDir(skinCache);
    }

    private void cleanDir(Path dir) {
        try (var stream = Files.list(dir)) {
            long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            stream.filter(p -> p.toFile().lastModified() < threshold)
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            LOGGER.info("Purged old cache file: {}", p.getFileName());
                        } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            LOGGER.error("Cache cleanup failed for directory: {}", dir, e);
        }
    }

    private boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}