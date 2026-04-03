package fr.neocle.flexbans.util.loader;

import fr.neocle.flexbans.logger.FlexLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;

public class PublicFolderLoader {
    private static final FlexLogger LOGGER = FlexLogger.get(PublicFolderLoader.class);

    public static void extractPublicFolder(File outputFolder) {
        LOGGER.debug("Extracting embedded /public/ from JAR...");

        try {
            var url = PublicFolderLoader.class.getClassLoader().getResource("public");
            if (url == null) {
                LOGGER.warn("No /public/ directory found in the JAR.");
                return;
            }

            if (!outputFolder.exists()) {
                boolean created = outputFolder.mkdirs();
                if (created) LOGGER.debug("Created folder: {}", outputFolder.getAbsolutePath());
            }

            URI uri = url.toURI();
            FileSystem fs = null;

            Path jarPath;
            if ("jar".equals(uri.getScheme())) {
                fs = FileSystems.newFileSystem(uri, Map.of());
                jarPath = fs.getPath("public");
            } else {
                jarPath = Paths.get(uri);
            }

            Files.walk(jarPath).forEach(path -> {
                try {
                    if (!Files.isRegularFile(path)) return;

                    Path relativePath = jarPath.relativize(path);
                    File targetFile = new File(outputFolder, relativePath.toString());

                    targetFile.getParentFile().mkdirs();

                    Files.copy(
                            path,
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    LOGGER.debug("Copied file: {}", relativePath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to copy file: ", e);
                }
            });

            if (fs != null) fs.close();

        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error extracting /public/: ", e);
        }
    }
}