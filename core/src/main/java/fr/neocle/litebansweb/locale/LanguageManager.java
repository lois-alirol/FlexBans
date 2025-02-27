package fr.neocle.litebansweb.locale;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LanguageManager {
    private static Logger logger;
    private static File langFolder;
    private static Yaml yaml;
    private static MiniMessage miniMessage;
    private static Map<String, String> languageData = new HashMap<>();

    private static final String[] AVAILABLE_LANGUAGES = {"en_US.yml", "fr_FR.yml", "de_DE.yml"};

    public static void initialize(Logger logger, Path dataFolder) {
        LanguageManager.logger = logger;
        langFolder = new File(dataFolder.toFile(), "lang");
        yaml = new Yaml(new DumperOptions());
        miniMessage = MiniMessage.miniMessage();

        ensureLanguageFilesExist();
    }

    private static void ensureLanguageFilesExist() {
        if (!langFolder.exists() && langFolder.mkdirs()) {
        }

        for (String langFile : AVAILABLE_LANGUAGES) {
            File targetFile = new File(langFolder, langFile);
            if (!targetFile.exists()) {
                try (InputStream resourceStream = LanguageManager.class.getClassLoader().getResourceAsStream("lang/" + langFile)) {
                    if (resourceStream != null) {
                        Files.copy(resourceStream, targetFile.toPath());
                    } else {
                        logger.warning("Missing language file: " + langFile + ". Contact plugin's developer.");
                    }
                } catch (IOException e) {
                    logger.severe("Failed to load language file " + langFile + ": " + e.getMessage());
                }
            }
        }
    }

    public static void loadLanguage(String lang) {
        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            logger.warning("Language file not found: " + langFile + ".yml");
            return;
        }

        try (InputStream inputStream = new FileInputStream(langFile)) {
            Map<String, Object> loadedData = yaml.load(inputStream);
            if (loadedData != null) {
                flattenMap("", loadedData);
                logger.info("Loaded language: " + lang);
            }
        } catch (IOException e) {
            logger.severe("Error loading language file " + lang + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void flattenMap(String parentKey, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flattenMap(key, (Map<String, Object>) entry.getValue());
            } else {
                languageData.put(key, entry.getValue().toString());
            }
        }
    }

    public static Component getMessageComponent(String key) {
        String message = languageData.getOrDefault(key, key);
        return miniMessage.deserialize(message);
    }

    public static String getMessageString(String key) {
        return languageData.getOrDefault(key, key);
    }

    public static BaseComponent[] getBungeeMessageComponent(CommandSender sender, String key) {
        Component component = getMessageComponent(key);

        if (sender instanceof ProxiedPlayer) {
            return BungeeComponentSerializer.get().serialize(component);
        } else {
            String legacyMessage = LegacyComponentSerializer.legacySection().serialize(component);
            return new BaseComponent[]{ new TextComponent(legacyMessage) };
        }
    }

}
