package fr.neocle.flexbans.web.security;

import fr.neocle.flexbans.web.auth.CsrfTokenManager;

import javax.servlet.http.HttpServletRequest;

public class CsrfValidator {
    private final SessionManager sessionManager;

    public CsrfValidator(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean verify(HttpServletRequest req) {
        String sessionId = sessionManager.getSessionId(req);

        if (sessionId == null) {
            return false;
        }

        String headerToken = req.getHeader("X-CSRF-Token");
        String cookieToken = getCsrfTokenFromCookie(req);

        if (!CsrfTokenManager.validateToken(sessionId, headerToken)) {
            return false;
        }

        if (cookieToken != null && !constantTimeEquals(headerToken, cookieToken)) {
            return false;
        }

        return true;
    }

    private String getCsrfTokenFromCookie(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : req.getCookies()) {
                if ("XSRF-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean constantTimeEquals(String a, String b) {
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