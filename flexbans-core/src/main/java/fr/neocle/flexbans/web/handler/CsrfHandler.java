package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonObject;
import fr.neocle.flexbans.web.auth.CsrfTokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.security.SessionManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CsrfHandler {
    private final SessionManager sessionManager;

    public CsrfHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ApiResponse<?> getCsrfToken(HttpServletRequest req, HttpServletResponse resp) {
        String sessionId = sessionManager.getOrCreateSessionId(req, resp);
        String csrfToken = CsrfTokenManager.generateToken(sessionId);

        sessionManager.setSecureCsrfCookie(resp, csrfToken);

        JsonObject response = new JsonObject();
        response.addProperty("csrfToken", csrfToken);
        return ApiResponse.success(response);
    }
}