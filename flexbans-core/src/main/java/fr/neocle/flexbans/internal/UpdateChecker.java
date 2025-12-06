package fr.neocle.flexbans.internal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.logger.FlexLogger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class UpdateChecker {

    private static final String CHECK_URL =
            "https://license-checker.license-verif.workers.dev/check-version?version=";

    private final String currentVersion;
    private final Gson gson = new Gson();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void start() {
        scheduler.schedule(this::checkNow, 0, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkNow, 3, 3, TimeUnit.HOURS);
    }

    private void checkNow() {
        try {
            URL url = new URL(CHECK_URL + currentVersion);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            JsonObject json = gson.fromJson(
                    new InputStreamReader(connection.getInputStream()),
                    JsonObject.class
            );

            boolean upToDate = json.get("upToDate").getAsBoolean();
            String latest = json.get("latest").getAsString();

            if (!upToDate) {
                FlexLogger.warn("==================================================");
                FlexLogger.warn("FlexBans is NOT up to date!");
                FlexLogger.warn("Current version : " + currentVersion);
                FlexLogger.warn("Latest version  : " + latest);
                FlexLogger.warn("Find the latest version on SITE_NAME");
                FlexLogger.warn("HTTPS://SITEDOMAIN/PLUGINURL.PLUGINID");
                FlexLogger.warn("==================================================");
            }

        } catch (Exception ex) {
            FlexLogger.warn("Failed to check updates: " + ex.getMessage());
        }
    }
}
