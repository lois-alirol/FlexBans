package fr.neocle.flexbans.handlers.Security.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class DomainFilter extends AbstractHandler {
    private final Logger logger;
    private final Map<String, Object> config;

    public DomainFilter(Map<String, Object> config, Logger logger) {
        this.logger = logger;
        this.config = config;
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
        String hostHeader = request.getHeader("Host");

        if (hostHeader == null || !hostHeader.equalsIgnoreCase(getAllowedURL())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Cannot access web interface from this url.");
            baseRequest.setHandled(true);
            return;
        }
    }
}
