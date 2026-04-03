package fr.neocle.flexbans.util;

import fr.neocle.flexbans.config.ConfigManager;
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
            Class<?> dbClass = Class.forName("litebans.api.Database");
            dbClass.getMethod("get").invoke(null);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            return false;
        }
    }
}
