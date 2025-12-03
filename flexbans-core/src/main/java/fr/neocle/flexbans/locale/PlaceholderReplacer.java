package fr.neocle.flexbans.locale;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderReplacer {

    private final Map<String, String> placeholders = new HashMap<>();

    public PlaceholderReplacer add(String placeholder, String value) {
        if (placeholder == null || value == null) return this;
        placeholders.put(placeholder, value);
        return this;
    }

    public PlaceholderReplacer addAll(Map<String, String> values) {
        if (values != null) placeholders.putAll(values);
        return this;
    }

    public String apply(String text) {
        if (text == null) return null;

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result = result.replace(key, value);
        }
        return result;
    }

    public static String apply(String text, Map<String, String> values) {
        return new PlaceholderReplacer()
                .addAll(values)
                .apply(text);
    }
}
