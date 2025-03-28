package fr.neocle.flexbans.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class ResourceLoader {
    private static final Yaml yaml = new Yaml();

    public static Map<String, Object> loadConfig(Path path, Logger logger) {
        try {
            File file = path.resolve("config.yml").toFile();
            try (FileInputStream inputStream = new FileInputStream(file)) {
                return yaml.load(inputStream);
            }
        } catch (IOException e) {
            logger.severe("Failed to read config file: " + e.getMessage());
            return null;
        }
    }

    public static Map<String, Object> loadWebhooksConfig(Path path, Logger logger) {
        try {
            File file = path.resolve("webhooks.yml").toFile();
            try (FileInputStream inputStream = new FileInputStream(file)) {
                return yaml.load(inputStream);
            }
        } catch (IOException e) {
            logger.severe("Failed to read config file: " + e.getMessage());
            return null;
        }
    }

    public static String loadHtmlTemplate(String path) throws IOException {
        InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}
