package fr.neocle.flexbans.web.security;

import fr.neocle.flexbans.config.ConfigManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SecurityHeadersManager {

    public void setHeaders(HttpServletResponse resp, HttpServletRequest req) {
        String allowedOrigin = ConfigManager.getString("webserver.url");

        resp.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-CSRF-Token");
        resp.setHeader("Access-Control-Max-Age", "3600");

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; connect-src 'self'");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }
}