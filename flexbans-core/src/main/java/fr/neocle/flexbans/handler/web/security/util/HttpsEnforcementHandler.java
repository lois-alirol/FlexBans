package fr.neocle.flexbans.handler.web.security.util;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.logger.FlexLogger;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpsEnforcementHandler extends AbstractHandler {

    public HttpsEnforcementHandler() {}

    public String getAllowedURL() {
        String url = ConfigManager.getString("webserver.url");

        if (url == null) {
            FlexLogger.error("No URL specified in the configuration. It is required to properly access the web interface.");
        }

        return url;
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean httpsEnabled = ConfigManager.getBoolean("webserver.https");

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
