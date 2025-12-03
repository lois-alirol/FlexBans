package fr.neocle.flexbans.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WebhooksConfigManager {
    private static Logger logger;
    private static File webhooksConfigFile;
    private static Yaml yaml;
    private static Node rootNode;
    private static Map<String, Object> webhooksConfigData = new HashMap<>();

    private static final String DEFAULT_WEBHOOKS_CONFIG = "webhooks.yml";

    public static void initialize(Logger logger, Path dataFolder) {
        WebhooksConfigManager.logger = logger;
        webhooksConfigFile = new File(dataFolder.toFile(), DEFAULT_WEBHOOKS_CONFIG);

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        dumperOptions.setProcessComments(true);

        yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);

        ensureConfigExists();
        loadConfig();
    }

    private static void ensureConfigExists() {
        if (!webhooksConfigFile.exists()) {
            try (InputStream resourceStream = WebhooksConfigManager.class.getClassLoader().getResourceAsStream(DEFAULT_WEBHOOKS_CONFIG)) {
                if (resourceStream != null) {
                    Files.copy(resourceStream, webhooksConfigFile.toPath());
                } else {
                    logger.warning("Missing default config.yml. Contact plugin's developer.");
                }
            } catch (IOException e) {
                logger.severe("Failed to create config.yml: " + e.getMessage());
            }
        }
    }

    public static void loadConfig() {
        if (!webhooksConfigFile.exists()) {
            logger.warning("Config file not found: " + DEFAULT_WEBHOOKS_CONFIG);
            return;
        }

        try (FileReader reader = new FileReader(webhooksConfigFile)) {
            rootNode = yaml.compose(reader);
            if (rootNode instanceof MappingNode mappingNode) {
                ensureDefaults(mappingNode);
                webhooksConfigData.clear();
                parseConfig(mappingNode, "");
            }
        } catch (IOException e) {
            logger.severe("Error loading config file: " + e.getMessage());
        }
    }

    private static void ensureDefaults(MappingNode root) {
        try (InputStream defaultStream = WebhooksConfigManager.class.getClassLoader().getResourceAsStream(DEFAULT_WEBHOOKS_CONFIG)) {
            if (defaultStream != null) {
                Node defaultRoot = yaml.compose(new InputStreamReader(defaultStream));
                if (defaultRoot instanceof MappingNode defaultMappingNode) {
                    boolean updated = mergeDefaults(root, defaultMappingNode);
                    if (updated) {
                        try (FileWriter writer = new FileWriter(webhooksConfigFile)) {
                            yaml.serialize(root, writer);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("Failed to check or update missing config keys: " + e.getMessage());
        }
    }

    private static boolean mergeDefaults(MappingNode target, MappingNode defaults) {
        boolean updated = false;
        Map<String, NodeTuple> targetMap = new HashMap<>();
        for (NodeTuple tuple : target.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
                targetMap.put(keyNode.getValue(), tuple);
            }
        }

        for (NodeTuple defaultTuple : defaults.getValue()) {
            if (defaultTuple.getKeyNode() instanceof ScalarNode defaultKeyNode) {
                String key = defaultKeyNode.getValue();
                if (!targetMap.containsKey(key)) {
                    target.getValue().add(defaultTuple);
                    updated = true;
                } else {
                    Node targetValue = targetMap.get(key).getValueNode();
                    Node defaultValue = defaultTuple.getValueNode();
                    if (targetValue instanceof MappingNode targetMapping && defaultValue instanceof MappingNode defaultMapping) {
                        updated |= mergeDefaults(targetMapping, defaultMapping);
                    }
                }
            }
        }
        return updated;
    }

    private static void parseConfig(MappingNode node, String parentKey) {
        for (NodeTuple tuple : node.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
                String key = parentKey.isEmpty() ? keyNode.getValue() : parentKey + "." + keyNode.getValue();
                Node valueNode = tuple.getValueNode();

                if (valueNode instanceof ScalarNode scalarNode) {
                    webhooksConfigData.put(key, scalarNode.getValue());
                } else if (valueNode instanceof MappingNode subNode) {
                    parseConfig(subNode, key);
                } else if (valueNode instanceof SequenceNode sequenceNode) {
                    List<String> listValues = new ArrayList<>();
                    for (Node listItem : sequenceNode.getValue()) {
                        if (listItem instanceof ScalarNode listItemNode) {
                            listValues.add(listItemNode.getValue());
                        }
                    }
                    webhooksConfigData.put(key, listValues);
                }
            }
        }
    }

    public static void reload() {
        webhooksConfigData.clear();
        loadConfig();
    }

    public static Object getConfigValue(String key) {
        return webhooksConfigData.getOrDefault(key, key);
    }

    // ====== PRIMITIVE GETTERS ======

    public static String getString(String key) {
        Object value = getConfigValue(key);
        return value != null ? value.toString() : "";
    }

    public static int getInt(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid int value for key: " + key);
        return 0;
    }

    public static boolean getBoolean(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);

        logger.warning("Invalid boolean value for key: " + key);
        return false;
    }

    public static long getLong(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid long value for key: " + key);
        return 0L;
    }

    public static double getDouble(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid double value for key: " + key);
        return 0.0;
    }

    public static float getFloat(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.floatValue();
        if (value instanceof String s) {
            try { return Float.parseFloat(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid float value for key: " + key);
        return 0f;
    }

    public static short getShort(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.shortValue();
        if (value instanceof String s) {
            try { return Short.parseShort(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid short value for key: " + key);
        return (short) 0;
    }

    public static byte getByte(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Number n) return n.byteValue();
        if (value instanceof String s) {
            try { return Byte.parseByte(s); } catch (Exception ignored) {}
        }
        logger.warning("Invalid byte value for key: " + key);
        return (byte) 0;
    }

    public static char getChar(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Character c) return c;
        if (value instanceof String s && s.length() == 1) return s.charAt(0);

        logger.warning("Invalid char value for key: " + key);
        return '\0';
    }

    @SuppressWarnings("unchecked")
    public static List<String> getList(String key) {
        Object value = getConfigValue(key);

        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object obj : list) result.add(String.valueOf(obj));
            return result;
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(String key) {
        Object value = getConfigValue(key);

        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return new HashMap<>();
    }

    public static Map<String, Object> getConfig() {
        return webhooksConfigData;
    }
}
