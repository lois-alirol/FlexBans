package fr.neocle.flexbans.locale;

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

    private static final String DEFAULT_LANGUAGE = "en_US.yml";
    private static final String[] AVAILABLE_LANGUAGES = {"en_US.yml", "fr_FR.yml", "de_DE.yml"};

    public static void initialize(Logger logger, Path dataFolder) {
        LanguageManager.logger = logger;
        langFolder = new File(dataFolder.toFile(), "lang");

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        yaml = new Yaml(dumperOptions);

        miniMessage = MiniMessage.miniMessage();

        ensureLanguageFilesExist();
    }

    private static void ensureLanguageFilesExist() {
        if (!langFolder.exists() && langFolder.mkdirs()) {}

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
            logger.warning("Language file not found: " + lang + ".yml. Defaulting to " + DEFAULT_LANGUAGE);
            langFile = new File(langFolder, DEFAULT_LANGUAGE);
        }

        try (InputStream inputStream = new FileInputStream(langFile)) {
            Map<String, Object> loadedData = yaml.load(inputStream);
            if (loadedData != null) {
                ensureDefaults(langFile, loadedData);
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

    private static void ensureDefaults(File langFile, Map<String, Object> loadedData) {
        try (InputStream defaultStream = LanguageManager.class.getClassLoader().getResourceAsStream("lang/" + DEFAULT_LANGUAGE)) {
            if (defaultStream != null) {
                Map<String, Object> defaultData = yaml.load(defaultStream);
                if (defaultData != null) {
                    boolean updated = mergeDefaults(loadedData, defaultData);
                    if (updated) {
                        try (Writer writer = new FileWriter(langFile)) {
                            yaml.dump(loadedData, writer);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("Failed to check or update missing language keys: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean mergeDefaults(Map<String, Object> target, Map<String, Object> defaults) {
        boolean updated = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!target.containsKey(key)) {
                target.put(key, defaultValue);
                updated = true;
            } else if (defaultValue instanceof Map && target.get(key) instanceof Map) {
                updated |= mergeDefaults((Map<String, Object>) target.get(key), (Map<String, Object>) defaultValue);
            } else if (!(target.get(key) instanceof Map) && defaultValue instanceof Map) {
                Map<String, Object> nestedMap = new HashMap<>();
                nestedMap.put("value", target.get(key));
                target.put(key, nestedMap);
                updated = true;
            }
        }
        return updated;
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