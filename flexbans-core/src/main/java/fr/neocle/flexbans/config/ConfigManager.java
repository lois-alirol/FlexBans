package fr.neocle.flexbans.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final YamlConfig config = new YamlConfig("config.yml", ConfigManager.class);

    public static void initialize(Path dataFolder)          { config.initialize(dataFolder); }
    public static void reload()                             { config.reload(); }
    public static Object getConfigValue(String key)         { return config.get(key); }
    public static Map<String, Object> getConfigSection(String path) { return config.getMap(path); }
    public static Map<String, Object> getConfig()           { return config.getAll(); }

    public static String getString(String key)              { return config.getString(key); }
    public static int getInt(String key)                    { return config.getInt(key); }
    public static boolean getBoolean(String key)            { return config.getBoolean(key); }
    public static long getLong(String key)                  { return config.getLong(key); }
    public static double getDouble(String key)              { return config.getDouble(key); }
    public static float getFloat(String key)                { return config.getFloat(key); }
    public static short getShort(String key)                { return config.getShort(key); }
    public static byte getByte(String key)                  { return config.getByte(key); }
    public static char getChar(String key)                  { return config.getChar(key); }
    public static List<String> getList(String key)          { return config.getList(key); }
    public static Map<String, Object> getMap(String key)    { return config.getMap(key); }
}