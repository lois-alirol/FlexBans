package fr.neocle.flexbans.handler.security.oauth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.error.ForbiddenError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class DiscordOAuthHandler {

    private static final Gson GSON = new Gson();

    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;
    private final ForbiddenError forbiddenError;
    private final Logger logger;

    private final boolean oauthEnabled = ConfigManager.getBoolean("discord-oauth.enabled");

    public DiscordOAuthHandler(DatabaseUtils databaseUtils, ForbiddenError forbiddenError,
                               EventDispatcher eventDispatcher, Logger logger) {
        this.databaseUtils = databaseUtils;
        this.forbiddenError = forbiddenError;
        this.eventDispatcher = eventDispatcher;
        this.logger = logger;
    }

    public void initiateOAuthFlow(HttpServletResponse response) throws IOException {
        if (!oauthEnabled) {
            return;
        }

        String redirectUri = encodeUri(ConfigManager.getString("discord-oauth.redirect-uri"));
        String authUrl = "https://discord.com/api/oauth2/authorize?client_id=" +
                ConfigManager.getString("discord-oauth.client-id") +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=" + ConfigManager.getString("discord-oauth.scope");

        response.sendRedirect(authUrl);
    }

    public void handleOAuthCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!oauthEnabled) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth is not enabled.");
            return;
        }

        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        String code = request.getParameter("code");
        if (code == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing OAuth code parameter.");
            return;
        }

        try {
            String accessToken = getAccessToken(code);
            String userId = getUserId(accessToken);

            if (!isUserAllowed(userId)) {
                forbiddenError.handle(request, response);
                return;
            }

            request.getSession().setAttribute("userId", userId);
            request.getSession().setAttribute("accessToken", accessToken);

            databaseUtils.getUserManager().insertDiscordId(userId);
            eventDispatcher.discordUserLoginEvent(userId, userAgent, ipAddress);

            response.sendRedirect("/index");
        } catch (Exception e) {
            logger.severe("Error during OAuth callback: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing OAuth callback.");
        }
    }

    public String encodeUri(String uri) throws IOException {
        return URLEncoder.encode(uri, StandardCharsets.UTF_8.toString());
    }

    public String getAccessToken(String code) throws IOException {
        URL url = new URL("https://discord.com/api/oauth2/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String redirectUri = encodeUri(ConfigManager.getString("discord-oauth.redirect-uri"));
        String params = "client_id=" + ConfigManager.getString("discord-oauth.client-id") +
                "&client_secret=" + ConfigManager.getString("discord-oauth.client-secret") +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + redirectUri;

        try (OutputStream os = connection.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return json.get("access_token").getAsString();
            }
        } else {
            throw new IOException("Failed to retrieve access token from Discord. HTTP " + connection.getResponseCode());
        }
    }

    public String getUserId(String accessToken) throws IOException {
        URL url = new URL("https://discord.com/api/v10/users/@me");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return json.get("id").getAsString();
            }
        } else {
            throw new IOException("Failed to retrieve user information from Discord. HTTP " + connection.getResponseCode());
        }
    }

    public String getUsernameFromId(String userId) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean isUserAllowed(String userId) {
        List<String> allowedUsers = ConfigManager.getList("discord-oauth.allowed-users");
        return allowedUsers.contains(userId);
    }

    public void updateConfig() {
        // TODO: implement config reload logic if necessary
    }
}
