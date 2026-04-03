package fr.neocle.flexbans.locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.neocle.flexbans.logger.FlexLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static File langFolder;
    private static Yaml yaml;
    private static MiniMessage miniMessage;
    public static Map<String, String> languageData = new HashMap<>();

    private static final FlexLogger LOGGER = FlexLogger.get(LanguageManager.class);

    private static final String DEFAULT_LANGUAGE = "en_US.yml";
    private static final String[] AVAILABLE_LANGUAGES = {"en_US.yml", "fr_FR.yml", "de_DE.yml"};

    public static void initialize(Path dataFolder) {
        langFolder = new File(dataFolder.toFile(), "lang");

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        yaml = new Yaml(dumperOptions);

        miniMessage = MiniMessage.miniMessage();

        ensureLanguageFilesExist();
        updateAllLanguageFiles();
        exportDashboardJson(dataFolder);
    }

    private static void updateAllLanguageFiles() {
        for (String langFile : AVAILABLE_LANGUAGES) {
            File file = new File(langFolder, langFile);
            if (!file.exists()) continue;

            try (InputStream inputStream = new FileInputStream(file)) {
                Map<String, Object> loadedData = yaml.load(inputStream);

                if (loadedData == null) {
                    loadedData = new HashMap<>();
                }

                ensureDefaults(file, loadedData);

            } catch (IOException e) {
                LOGGER.error("Error updating defaults for {}: ", langFile, e);
            }
        }
    }

    private static void ensureLanguageFilesExist() {
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        for (String langFile : AVAILABLE_LANGUAGES) {
            File targetFile = new File(langFolder, langFile);
            if (!targetFile.exists()) {
                try (InputStream resourceStream = LanguageManager.class.getClassLoader().getResourceAsStream("lang/" + langFile)) {
                    if (resourceStream != null) {
                        Files.copy(resourceStream, targetFile.toPath());
                    } else {
                        LOGGER.warn("Missing language file: {}. Contact plugin's developer.", langFile);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to load language file {}: ", langFile, e);
                }
            }
        }
    }

    public static void loadLanguage(String lang) {
        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            LOGGER.warn("Language file not found: {}.yml. Defaulting to ", lang, DEFAULT_LANGUAGE);
            langFile = new File(langFolder, DEFAULT_LANGUAGE);
        }

        try (InputStream inputStream = new FileInputStream(langFile)) {
            Map<String, Object> loadedData = yaml.load(inputStream);
            if (loadedData != null) {
                ensureDefaults(langFile, loadedData);
                flattenMap("", loadedData);

                LOGGER.info("Loaded language: {}", lang);
            }
        } catch (IOException e) {
            LOGGER.error("Error loading language file {}: ", lang, e);
        }
    }

    public static void exportDashboardJson(Path dataFolder) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (String langFile : AVAILABLE_LANGUAGES) {
            File file = new File(langFolder, langFile);

            if (!file.exists()) {
                LOGGER.warn("Skipping {} (file does not exist).", langFile);
                continue;
            }

            try (InputStream input = new FileInputStream(file)) {
                Map<String, Object> yamlData = yaml.load(input);
                if (yamlData == null) {
                    LOGGER.warn("Dashboard Export: Skipping {} (file is empty or invalid YAML).", langFile);
                    continue;
                }

                Object dashboardNode = yamlData.get("dashboard");
                if (!(dashboardNode instanceof Map)) {
                    LOGGER.warn("Skipping {} (no root 'dashboard' map found).", langFile);
                    continue;
                }

                String lang = langFile.replace(".yml", "");
                File outDir = new File(dataFolder.toFile(), "public/locales/" + lang);

                if (!outDir.exists() && !outDir.mkdirs()) {
                    LOGGER.error("Failed to create directory {}", outDir.getPath());
                    continue;
                }

                File outFile = new File(outDir, "common.json");

                try (Writer writer = new FileWriter(outFile)) {
                    gson.toJson(dashboardNode, writer);
                }

            } catch (Exception e) {
                LOGGER.error("Failed exporting dashboard JSON for {}: ", langFile, e);
            }
        }
    }

    private static void flattenMap(String parentKey, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String currentKey = String.valueOf(entry.getKey());
            String key = parentKey.isEmpty() ? currentKey : parentKey + "." + currentKey;

            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMap(key, (Map<?, ?>) value);
            } else {
                languageData.put(key, value != null ? value.toString() : "");
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
            LOGGER.error("Failed to check or update missing language keys: ", e);
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
            return new BaseComponent[]{new TextComponent(legacyMessage)};
        }
    }

    public static void reload(String lang) {
        ensureLanguageFilesExist();
        languageData.clear();
        loadLanguage(lang);
    }
}