package fr.neocle.flexbans.handlers.Security.Utils;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.handler.AbstractHandler;

public class HttpsEnforcementHandler extends AbstractHandler {
    private final Map<String, Object> config;
    private final Logger logger;

    public HttpsEnforcementHandler(Map<String, Object> config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public String getAllowedURL() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) config.get("webserver");
        String url = String.valueOf(webserverConfig.getOrDefault("url", null));

        if (url == null) {
            logger.severe("No URL specified in the configuration. It is required to properly access the web interface.");
        }

        return url;
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) config.get("webserver");
        boolean httpsEnabled = (boolean) webserverConfig.getOrDefault("https", false);

        if (httpsEnabled) {
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            if (forwardedProto == null || !"https".equalsIgnoreCase(forwardedProto)) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", "https://" + getAllowedURL());
                baseRequest.setHandled(true);
                return;
            }
        }
    }
};
