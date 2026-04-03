package fr.neocle.flexbans.util.loader;

import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.network.HttpClientProvider;
import net.sf.image4j.codec.ico.ICOEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FaviconConverter {

    private static final int FAVICON_SIZE = 32;
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;
    private static final FlexLogger LOGGER = FlexLogger.get(FaviconConverter.class);

    public static void convertUrlToFavicon(String imageUrl, File targetFolder) throws Exception {
        if (!targetFolder.exists()) targetFolder.mkdirs();
        if (!targetFolder.isDirectory()) throw new IllegalArgumentException("Not a directory: " + targetFolder);

        File outputFile = new File(targetFolder, "favicon.ico");

        byte[] imageBytes = fetchImageBytes(imageUrl);
        if (imageBytes == null) throw new IllegalArgumentException("Could not download image from URL.");

        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (sourceImage == null) throw new IllegalArgumentException("Could not decode image.");

        BufferedImage resizedImage = resizeImage(sourceImage, FAVICON_SIZE, FAVICON_SIZE);

        ICOEncoder.write(resizedImage, outputFile);

        LOGGER.debug("Favicon successfully saved to: {}", outputFile.getAbsolutePath());
    }

    private static byte[] fetchImageBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0 (compatible; FlexBans/1.0)")
                .header("Accept", "image/png,image/jpeg,image/webp,image/*,*/*")
                .GET()
                .build();

        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200) return response.body();
        return null;
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        return resizedImage;
    }
}