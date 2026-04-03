package fr.neocle.flexbans.internal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.network.HttpClientProvider;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {
    private static final String CHECK_URL =
            "https://license.loisalirol.com/check-version?version=";
    private static final FlexLogger LOGGER = FlexLogger.get(UpdateChecker.class);
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;

    private final String currentVersion;
    private final Gson gson = new Gson();

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void start() {
        TaskScheduler.get().runRepeating(this::checkNow, 10800000);
    }

    private void checkNow() {
        String url = CHECK_URL + currentVersion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("Failed to check updates, HTTP status: {}", response.statusCode());
                return;
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            boolean upToDate = json.get("upToDate").getAsBoolean();
            String latest = json.get("latest").getAsString();

            String yellow = "\u001B[38;5;214m";
            String lightYellow = "\u001B[38;5;228m";
            String reset = "\u001B[0m";

            if (!upToDate) {
                LOGGER.info(yellow + "----------===============☰☰☰☰☰☰☰☰☰☰☰===============----------" + reset);
                LOGGER.info(" ");
                LOGGER.info(yellow + "FlexBans is NOT up to date!" + reset);
                LOGGER.info(yellow + "Update at: " + lightYellow + "HTTPS://SITEDOMAIN/PLUGINURL.PLUGINID" + reset);
                LOGGER.info(yellow + " > Current version: " + lightYellow + currentVersion + reset);
                LOGGER.info(yellow + " > Latest version : " + lightYellow + latest + reset);
                LOGGER.info(" ");
                LOGGER.info(yellow + "----------===============☰☰☰☰☰☰☰☰☰☰☰===============----------" + reset);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to check updates: ", e);
        }
    }
}
