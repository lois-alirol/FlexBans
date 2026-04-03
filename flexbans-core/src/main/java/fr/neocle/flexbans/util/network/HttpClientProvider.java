package fr.neocle.flexbans.util.network;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientProvider {
    public static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
}