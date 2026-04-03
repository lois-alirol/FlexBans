package fr.neocle.flexbans.config;

import fr.neocle.flexbans.logger.FlexLogger;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class YamlConfig {

    private final String resourceName;
    private final FlexLogger logger;

    private File configFile;
    private Yaml yaml;
    private Node rootNode;
    private final Map<String, Object> data = new HashMap<>();

    public YamlConfig(String resourceName, Class<?> ownerClass) {
        this.resourceName = resourceName;
        this.logger = FlexLogger.get(ownerClass);
    }

    public void initialize(Path dataFolder) {
        configFile = new File(dataFolder.toFile(), resourceName);

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        dumperOptions.setProcessComments(true);

        yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);

        ensureConfigExists();
        load();
    }

    private void ensureConfigExists() {
        if (configFile.exists()) return;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
            } else {
                logger.warn("Missing default {}. Contact plugin's developer.", resourceName);
            }
        } catch (IOException e) {
            logger.error("Failed to create {}: ", resourceName, e);
        }
    }

    public void load() {
        if (!configFile.exists()) {
            logger.warn("Config file not found: {}", resourceName);
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            rootNode = yaml.compose(reader);
            if (rootNode instanceof MappingNode mappingNode) {
                ensureDefaults(mappingNode);
                data.clear();
                parseInto(mappingNode, "", data);
            }
        } catch (IOException e) {
            logger.error("Error loading {}: ", resourceName, e);
        }
    }

    private void ensureDefaults(MappingNode root) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) return;
            Node defaultRoot = yaml.compose(new InputStreamReader(in));
            if (defaultRoot instanceof MappingNode defaultMapping) {
                boolean updated = mergeDefaults(root, defaultMapping);
                if (updated) {
                    try (FileWriter writer = new FileWriter(configFile)) {
                        yaml.serialize(root, writer);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to merge defaults for {}: ", resourceName, e);
        }
    }

    private boolean mergeDefaults(MappingNode target, MappingNode defaults) {
        boolean updated = false;
        Map<String, NodeTuple> targetMap = new HashMap<>();
        for (NodeTuple t : target.getValue()) {
            if (t.getKeyNode() instanceof ScalarNode k) targetMap.put(k.getValue(), t);
        }
        for (NodeTuple dt : defaults.getValue()) {
            if (!(dt.getKeyNode() instanceof ScalarNode dk)) continue;
            String key = dk.getValue();
            if (!targetMap.containsKey(key)) {
                target.getValue().add(dt);
                updated = true;
            } else {
                Node tv = targetMap.get(key).getValueNode();
                Node dv = dt.getValueNode();
                if (tv instanceof MappingNode tm && dv instanceof MappingNode dm) {
                    updated |= mergeDefaults(tm, dm);
                }
            }
        }
        return updated;
    }

    private void parseInto(MappingNode node, String parentKey, Map<String, Object> out) {
        for (NodeTuple tuple : node.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode keyNode)) continue;
            String key = keyNode.getValue();
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;
            Node valueNode = tuple.getValueNode();

            if (valueNode instanceof ScalarNode scalar) {
                out.put(fullKey, scalar.getValue());
            } else if (valueNode instanceof MappingNode sub) {
                parseInto(sub, fullKey, out);
            } else if (valueNode instanceof SequenceNode seq) {
                List<String> list = new ArrayList<>();
                for (Node item : seq.getValue()) {
                    if (item instanceof ScalarNode s) list.add(s.getValue());
                }
                out.put(fullKey, list);
            }
        }
    }

    public void reload() {
        data.clear();
        load();
    }

    // ====== GETTERS ======

    public Map<String, Object> getAll() { return data; }

    public Object get(String key) {
        return data.getOrDefault(key, key);
    }

    public String getString(String key) {
        Object v = get(key);
        return v != null ? v.toString() : "";
    }

    public int getInt(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) try { return Integer.parseInt(s); } catch (Exception ignored) {}
        logger.warn("Invalid int for key: {}", key); return 0;
    }

    public boolean getBoolean(String key) {
        Object v = get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        logger.warn("Invalid boolean for key: {}", key); return false;
    }

    public long getLong(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) try { return Long.parseLong(s); } catch (Exception ignored) {}
        logger.warn("Invalid long for key: {}", key); return 0L;
    }

    public double getDouble(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) try { return Double.parseDouble(s); } catch (Exception ignored) {}
        logger.warn("Invalid double for key: {}", key); return 0.0;
    }

    public float getFloat(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.floatValue();
        if (v instanceof String s) try { return Float.parseFloat(s); } catch (Exception ignored) {}
        logger.warn("Invalid float for key: {}", key); return 0f;
    }

    public short getShort(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.shortValue();
        if (v instanceof String s) try { return Short.parseShort(s); } catch (Exception ignored) {}
        logger.warn("Invalid short for key: {}", key); return (short) 0;
    }

    public byte getByte(String key) {
        Object v = get(key);
        if (v instanceof Number n) return n.byteValue();
        if (v instanceof String s) try { return Byte.parseByte(s); } catch (Exception ignored) {}
        logger.warn("Invalid byte for key: {}", key); return (byte) 0;
    }

    public char getChar(String key) {
        Object v = get(key);
        if (v instanceof Character c) return c;
        if (v instanceof String s && s.length() == 1) return s.charAt(0);
        logger.warn("Invalid char for key: {}", key); return '\0';
    }

    public List<String> getList(String key) {
        Object v = get(key);
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) result.add(String.valueOf(o));
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        Object v = get(key);
        if (v instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return new HashMap<>();
    }
}