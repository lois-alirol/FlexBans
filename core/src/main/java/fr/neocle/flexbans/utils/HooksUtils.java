package fr.neocle.flexbans.utils;

import fr.neocle.flexbans.configs.ConfigManager;

public class HooksUtils {
    public static boolean usingFlexBansSystem() {
        return Boolean.parseBoolean((String) ConfigManager.getConfigValue("punishments-system.built-in.enabled"));
    }

    public static boolean usingLiteBansSystem() {
        System.out.println(isLiteBansLoaded() + " " + !Boolean.parseBoolean((String) ConfigManager.getConfigValue("punishments-system.built-in.enabled") + " " + Boolean.parseBoolean((String) ConfigManager.getConfigValue("punishments-system.hooks.litebans"))));
        return isLiteBansLoaded()
                && !Boolean.parseBoolean((String) ConfigManager.getConfigValue("punishments-system.built-in.enabled"))
                && Boolean.parseBoolean((String) ConfigManager.getConfigValue("punishments-system.hooks.litebans"));
    }

    public static boolean isLiteBansLoaded() {
        try {
            Class.forName("litebans.api.Database");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
