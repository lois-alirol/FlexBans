package fr.neocle.litebansweb.handlers.Security.OAuthHandlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.utils.DatabaseUtils;

public class DiscordOAuthHandler {
    private final Map<String, Object> oauthConfig;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;
    private final boolean oauthEnabled;

    public DiscordOAuthHandler(Map<String, Object> oauthConfig, DatabaseUtils databaseUtils, EventDispatcher eventDispatcher) {
        this.oauthConfig = oauthConfig;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.oauthEnabled = Boolean.parseBoolean(oauthConfig.getOrDefault("enabled", "false").toString());
    }

    public void initiateOAuthFlow(HttpServletResponse response) throws IOException {
        if (!oauthEnabled) {
            return;
        }

        String redirectUri = encodeUri((String) oauthConfig.get("redirect-uri"));
        String authUrl = "https://discord.com/api/oauth2/authorize?client_id=" + oauthConfig.get("client-id") +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=" + oauthConfig.get("scope");
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
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized: You do not have access.");
                return;
            }
            
            request.getSession().setAttribute("userId", userId);
            request.getSession().setAttribute("accessToken", accessToken);
            
            databaseUtils.insertDiscordId(userId);
            eventDispatcher.discordUserLoginEvent(userId, ipAddress, userAgent);

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

        String redirectUri = encodeUri((String) oauthConfig.get("redirect-uri"));
        String params = "client_id=" + oauthConfig.get("client-id") +
                "&client_secret=" + oauthConfig.get("client-secret") +
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
        String allowedUsers = String.join(",", (Iterable<String>) oauthConfig.get("allowed-users"));
        return allowedUsers.contains(userId);
    }
    
}