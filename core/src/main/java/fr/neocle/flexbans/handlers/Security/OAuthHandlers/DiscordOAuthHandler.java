package fr.neocle.flexbans.handlers.Security.OAuthHandlers;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.Errors.ForbiddenError;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

public class DiscordOAuthHandler {
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;
    private final ForbiddenError forbiddenError;
    private final Logger logger;

    private final boolean oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));

    public DiscordOAuthHandler(DatabaseUtils databaseUtils, ForbiddenError forbiddenError, EventDispatcher eventDispatcher, Logger logger) {
        this.databaseUtils = databaseUtils;
        this.forbiddenError = forbiddenError;
        this.eventDispatcher = eventDispatcher;
        this.logger = logger;
    }

    public void initiateOAuthFlow(HttpServletResponse response) throws IOException {
        if (!oauthEnabled) {
            return;
        }

        String redirectUri = encodeUri((String) ConfigManager.getConfigValue("discord-oauth.redirect-uri"));
        String authUrl = "https://discord.com/api/oauth2/authorize?client_id=" + ConfigManager.getConfigValue("discord-oauth.client-id") +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=" + ConfigManager.getConfigValue("discord-oauth.scope");

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
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing OAuth callback.");
            e.printStackTrace();
        }
    }

    public String encodeUri(String uri) throws IOException {
        return URLEncoder.encode(uri, StandardCharsets.UTF_8.toString());
    }

    @SuppressWarnings("deprecation")
    public String getAccessToken(String code) throws IOException {
        URL url = new URL("https://discord.com/api/oauth2/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String redirectUri = encodeUri((String) ConfigManager.getConfigValue("discord-oauth.redirect-uri"));
        String params = "client_id=" + ConfigManager.getConfigValue("discord-oauth.client-id") +
                "&client_secret=" + ConfigManager.getConfigValue("discord-oauth.client-secret") +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + redirectUri;

        try (OutputStream os = connection.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStream is = connection.getInputStream()) {
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject jsonResponse = new JSONObject(response);
                return jsonResponse.getString("access_token");
            }
        } else {
            throw new IOException("Failed to retrieve access token from Discord.");
        }
    }

    @SuppressWarnings("deprecation")
    public String getUserId(String accessToken) throws IOException {
        URL url = new URL("https://discord.com/api/v10/users/@me");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStream is = connection.getInputStream()) {
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject jsonResponse = new JSONObject(response);
                return jsonResponse.getString("id");
            }
        } else {
            throw new IOException("Failed to retrieve user information from Discord.");
        }
    }

    public boolean isUserAllowed(String userId) {
        @SuppressWarnings("unchecked")
        String allowedUsers = String.join(",", (Iterable<String>) ConfigManager.getConfigValue("discord-oauth.allowed-users"));
        return allowedUsers.contains(userId);
    }

    public void updateConfig() {
        // TO DO
    }
}