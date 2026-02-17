package fr.neocle.flexbans.web.security;

import fr.neocle.flexbans.web.auth.TokenManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;

public class SessionManager {
    private final SecureRandom secureRandom;

    public SessionManager() {
        this.secureRandom = new SecureRandom();
    }

    public String getSessionId(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : req.getCookies()) {
                if ("sessionId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String token = extractBearerToken(req);
        if (token != null) {
            return TokenManager.getTokenId(token);
        }

        return null;
    }

    public String getOrCreateSessionId(HttpServletRequest req, HttpServletResponse resp) {
        String sessionId = getSessionId(req);
        if (sessionId == null) {
            sessionId = generateSessionId();
            setSessionCookie(resp, sessionId);
        }
        return sessionId;
    }

    private String generateSessionId() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void setSessionCookie(HttpServletResponse resp, String sessionId) {
        StringBuilder cookie = new StringBuilder();
        cookie.append("sessionId=").append(sessionId);
        cookie.append("; Path=/; HttpOnly; SameSite=Strict; Max-Age=86400");
        if (isProduction()) {
            cookie.append("; Secure");
        }
        resp.addHeader("Set-Cookie", cookie.toString());
    }

    public void setSecureCsrfCookie(HttpServletResponse resp, String token) {
        StringBuilder cookie = new StringBuilder();
        cookie.append("XSRF-TOKEN=").append(token);
        cookie.append("; Path=/; SameSite=Strict; Max-Age=3600");
        cookie.append("; Secure");

        resp.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractBearerToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim();
        }

        if (req.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : req.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
                if ("tempToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private boolean isProduction() {
        return "production".equals(System.getenv("APP_ENV"));
    }
}