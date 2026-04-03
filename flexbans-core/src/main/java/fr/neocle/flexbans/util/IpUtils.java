package fr.neocle.flexbans.util;

import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.network.HttpClientProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class IpUtils {
    private static final FlexLogger LOGGER = FlexLogger.get(IpUtils.class);
    private static final HttpClient CLIENT = HttpClientProvider.CLIENT;

    public static String getPublicIP() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ipify.org"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to retrieve public IP Address: ", e);
        }

        return null;
    }
}