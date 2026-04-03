package fr.neocle.flexbans.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.neocle.flexbans.util.network.HttpClientProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LicenseChecker {
    private static final String API_URL = "https://license.loisalirol.com/";
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;

    public static boolean isLicenseValid(String key, String ip) {
        if (ip == null || key == null) return false;

        String fullUrl = String.format("%s?key=%s&ip=%s", API_URL, key, ip);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return false;

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.has("valid") && json.get("valid").getAsBoolean();

        } catch (Exception e) {
            return false;
        }
    }

    public static String getDiscordId(String licenseKey) {
        if (licenseKey == null) return null;

        String fullUrl = API_URL + "get-discord?key=" + licenseKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.has("discord_id") ? json.get("discord_id").getAsString() : null;

        } catch (Exception e) {
            return null;
        }
    }
}