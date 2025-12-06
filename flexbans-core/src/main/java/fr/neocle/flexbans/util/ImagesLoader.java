package fr.neocle.flexbans.util;

import fr.neocle.flexbans.logger.FlexLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;
import java.util.logging.Logger;

public class ImagesLoader {
    public static void extractImagesFromJar(File outputFolder) {
        FlexLogger.info("Extracting embedded /images/ from JAR...");

        try {
            var url = ImagesLoader.class.getClassLoader().getResource("images");
            if (url == null) {
                FlexLogger.warn("No /images/ directory found in the JAR.");
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

                    FlexLogger.info("Loaded image: " + fileName);

                } catch (IOException e) {
                    FlexLogger.warn("Failed to copy image: " + e.getMessage());
                }
            });

            if (fs != null)
                fs.close();

        } catch (IOException | URISyntaxException e) {
            FlexLogger.error("Error extracting /images/: " + e.getMessage());
        }
    }
}
