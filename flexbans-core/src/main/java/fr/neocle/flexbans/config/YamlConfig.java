package fr.neocle.flexbans.config;

import fr.neocle.flexbans.logger.FlexLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class AbstractConfigManager {
    protected File configFile;
    protected Yaml yaml;
    protected Node rootNode;
    protected Map<String, Object> configData = new HashMap<>();
    protected FlexLogger logger;

    protected abstract String getDefaultFileName();

    public void initialize(Path dataFolder, Class<?> clazz) {
        this.configFile = new File(dataFolder.toFile(), getDefaultFileName());
        this.logger = FlexLogger.get(clazz);

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        dumperOptions.setProcessComments(true);

        this.yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);

        ensureConfigExists(clazz);
        loadConfig();
    }

    protected void ensureConfigExists(Class<?> clazz) {
        if (!configFile.exists()) {
            try (InputStream resourceStream = clazz.getClassLoader().getResourceAsStream(getDefaultFileName())) {
                if (resourceStream != null) {
                    Files.copy(resourceStream, configFile.toPath());
                } else {
                    logger.warn("Missing default {}. Contact plugin's developer.", getDefaultFileName());
                }
            } catch (IOException e) {
                logger.error("Failed to create " + getDefaultFileName() + ": ", e);
            }
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            logger.warn("Config file not found: {}", getDefaultFileName());
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            rootNode = yaml.compose(reader);
            if (rootNode instanceof MappingNode mappingNode) {
                ensureDefaults();
                configData.clear();
                parseConfig(mappingNode, "");
            }
        } catch (IOException e) {
            logger.error("Error loading config file: ", e);
        }
    }

    protected void ensureDefaults() {
        try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(getDefaultFileName())) {
            if (defaultStream != null) {
                Node defaultRoot = yaml.compose(new InputStreamReader(defaultStream));
                if (defaultRoot instanceof MappingNode defaultMappingNode) {
                    boolean updated = mergeDefaults((MappingNode) rootNode, defaultMappingNode);
                    if (updated) {
                        try (FileWriter writer = new FileWriter(configFile)) {
                            yaml.serialize(rootNode, writer);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to check or update missing config keys: ", e);
        }
    }

    private boolean mergeDefaults(MappingNode target, MappingNode defaults) {
        boolean updated = false;
        Map<String, NodeTuple> targetMap = new HashMap<>();
        for (NodeTuple tuple : target.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode) targetMap.put(keyNode.getValue(), tuple);
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
                    if (targetValue instanceof MappingNode t && defaultValue instanceof MappingNode d) updated |= mergeDefaults(t, d);
                }
            }
        }
        return updated;
    }

    private void parseConfig(MappingNode node, String parentKey) {
        for (NodeTuple tuple : node.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
                String key = parentKey.isEmpty() ? keyNode.getValue() : parentKey + "." + keyNode.getValue();
                Node valueNode = tuple.getValueNode();

                if (valueNode instanceof ScalarNode scalarNode) configData.put(key, scalarNode.getValue());
                else if (valueNode instanceof MappingNode subNode) parseConfig(subNode, key);
                else if (valueNode instanceof SequenceNode sequenceNode) {
                    List<String> listValues = new ArrayList<>();
                    for (Node listItem : sequenceNode.getValue()) {
                        if (listItem instanceof ScalarNode listItemNode) listValues.add(listItemNode.getValue());
                    }
                    configData.put(key, listValues);
                }
            }
        }
    }

    public void reload() {
        configData.clear();
        loadConfig();
    }

    public Object getConfigValue(String key) {
        return configData.getOrDefault(key, key);
    }

    public Map<String, Object> getConfig() {
        return configData;
    }
}