package fr.neocle.flexbans.configs;

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

    public static Map<String, Object> getConfig() {
        return webhooksConfigData;
    }
}
