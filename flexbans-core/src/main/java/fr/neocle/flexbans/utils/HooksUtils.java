package fr.neocle.flexbans.utils;

import fr.neocle.flexbans.configs.ConfigManager;
import litebans.api.Database;
import litebans.api.exception.MissingImplementationException;

public class HooksUtils {
    public static boolean usingFlexBansSystem() {
        return ConfigManager.getBoolean("punishments-system.built-in.enabled");
    }

    public static boolean usingLiteBansSystem() {
        return isLiteBansLoaded()
                && !ConfigManager.getBoolean("punishments-system.built-in.enabled")
                && ConfigManager.getBoolean("punishments-system.hooks.litebans")
                && isLiteBansAvailable();
    }

    public static boolean isLiteBansLoaded() {
        try {
            Class.forName("litebans.api.Database");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isLiteBansAvailable() {
        try {
            Database.get();
            return true;
        } catch (MissingImplementationException e) {
            return false;
        }
    }
}
