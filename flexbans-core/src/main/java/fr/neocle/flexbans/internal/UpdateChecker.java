package fr.neocle.flexbans.internal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
    private final Logger logger;
    private final Gson gson = new Gson();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public UpdateChecker(String currentVersion, Logger logger) {
        this.currentVersion = currentVersion;
        this.logger = logger;
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
                logger.warning("==================================================");
                logger.warning("FlexBans is NOT up to date!");
                logger.warning("Current version : " + currentVersion);
                logger.warning("Latest version  : " + latest);
                logger.warning("Find the latest version on SITE_NAME");
                logger.warning("HTTPS://SITEDOMAIN/PLUGINURL.PLUGINID");
                logger.warning("==================================================");
            }

        } catch (Exception ex) {
            logger.warning("Failed to check updates: " + ex.getMessage());
        }
    }
}
