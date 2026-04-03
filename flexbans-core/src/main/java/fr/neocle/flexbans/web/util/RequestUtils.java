package fr.neocle.flexbans.web.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

public class RequestUtils {
    private static final Gson gson = new Gson();
    private static final FlexLogger LOGGER = FlexLogger.get(RequestUtils.class);

    public static String extractBearerToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim();
        }

        if (req.getCookies() != null) {
            String temp = null;
            String access = null;

            for (Cookie cookie : req.getCookies()) {
                if ("accessToken".equals(cookie.getName())) access = cookie.getValue();
                if ("tempToken".equals(cookie.getName())) temp = cookie.getValue();
            }

            return (access != null) ? access : temp;
        }
        return null;
    }

    public static JsonObject parseJsonBody(HttpServletRequest req) throws IOException {
        String body = req.getReader().lines().collect(Collectors.joining());

        if (body.isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            LOGGER.error("Invalid JSON body: ", e);
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }

    public static String getRequiredString(JsonObject body, String key, int minLen, int maxLen, String fieldName) {
        if (!body.has(key) || body.get(key).isJsonNull()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String value = body.get(key).getAsString().trim();
        if (value.length() < minLen || value.length() > maxLen) {
            throw new IllegalArgumentException(fieldName + " must be between " + minLen + " and " + maxLen + " characters");
        }
        return value;
    }

    public static int getPageParameter(HttpServletRequest req) {
        String pageParam = req.getParameter("page");
        if (pageParam != null && !pageParam.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(pageParam.trim()));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }
}