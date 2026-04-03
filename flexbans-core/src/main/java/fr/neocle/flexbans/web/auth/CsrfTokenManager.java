package fr.neocle.flexbans.web.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util. Map;
import java.util.concurrent.ConcurrentHashMap;

public class CsrfTokenManager {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    private static final Map<String, CsrfTokenData> CSRF_TOKENS = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRY = 3600000; // 1 hour

    private static class CsrfTokenData {
        String token;
        long createdAt;

        CsrfTokenData(String token) {
            this.token = token;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TOKEN_EXPIRY;
        }
    }

    /**
     * Generate a new CSRF token for a session/user
     */
    public static String generateToken(String sessionId) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom. nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        CSRF_TOKENS.put(sessionId, new CsrfTokenData(token));
        return token;
    }

    /**
     * Validate a CSRF token for a session/user
     */
    public static boolean validateToken(String sessionId, String token) {
        if (sessionId == null || token == null || token.isEmpty()) {
            return false;
        }

        CsrfTokenData storedData = CSRF_TOKENS.get(sessionId);
        if (storedData == null || storedData.isExpired()) {
            CSRF_TOKENS.remove(sessionId);
            return false;
        }

        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(storedData.token, token);
    }

    /**
     * Rotate token after successful validation (optional but recommended)
     */
    public static String rotateToken(String sessionId) {
        return generateToken(sessionId);
    }

    /**
     * Remove token on logout
     */
    public static void invalidateToken(String sessionId) {
        CSRF_TOKENS. remove(sessionId);
    }

    /**
     * Cleanup expired tokens periodically
     */
    public static void cleanupExpiredTokens() {
        CSRF_TOKENS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}