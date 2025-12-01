package fr.neocle.flexbans.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;
import java.util.logging.Logger;

public class ImagesLoader {
    public static void extractImagesFromJar(Logger logger, File outputFolder) {
        if (logger != null)
            logger.info("Extracting embedded /images/ from JAR...");

        try {
            var url = ImagesLoader.class.getClassLoader().getResource("images");
            if (url == null) {
                if (logger != null)
                    logger.warning("No /images/ directory found in the JAR.");
                return;
            }

            if (!outputFolder.exists())
                outputFolder.mkdirs();

            URI uri = url.toURI();
            FileSystem fs = null;

            if ("jar".equals(uri.getScheme())) {
                fs = FileSystems.newFileSystem(uri, Map.of());
            }

            Path imagesPath = (fs != null) ? fs.getPath("images") : Paths.get(uri);

            Files.walk(imagesPath).forEach(path -> {
                try {
                    if (!Files.isRegularFile(path))
                        return;

                    String fileName = path.getFileName().toString();
                    File targetFile = new File(outputFolder, fileName);

                    Files.copy(
                            path,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    if (logger != null)
                        logger.info("Loaded image: " + fileName);

                } catch (IOException e) {
                    if (logger != null)
                        logger.warning("Failed to copy image: " + e.getMessage());
                }
            });

            if (fs != null)
                fs.close();

        } catch (IOException | URISyntaxException e) {
            if (logger != null)
                logger.severe("Error extracting /images/: " + e.getMessage());
        }
    }
}
