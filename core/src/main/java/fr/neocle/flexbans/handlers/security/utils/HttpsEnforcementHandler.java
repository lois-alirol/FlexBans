package fr.neocle.flexbans.handlers.security.utils;

import fr.neocle.flexbans.configs.ConfigManager;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class HttpsEnforcementHandler extends AbstractHandler {
    private final Logger logger;

    public HttpsEnforcementHandler(Logger logger) {
        this.logger = logger;
    }

    public String getAllowedURL() {
        String url = (String) ConfigManager.getConfigValue("webserver.url");

        if (url == null) {
            logger.severe("No URL specified in the configuration. It is required to properly access the web interface.");
        }

        return url;
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean httpsEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.https"));

        if (httpsEnabled) {
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            if (forwardedProto == null || !"https".equalsIgnoreCase(forwardedProto)) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", "https://" + getAllowedURL());
                baseRequest.setHandled(true);
            }
        }
    }
};
