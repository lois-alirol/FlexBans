package fr.neocle.flexbans.web.util;

import com.google.gson.JsonObject;

public class RequestValidator {
    public static String getRequiredString(JsonObject json, String field) throws IllegalArgumentException {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return json.get(field).getAsString();
    }

    public static String getOptionalString(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            return null;
        }
        return json.get(field).getAsString();
    }
}