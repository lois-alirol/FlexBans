package fr.neocle.flexbans.handlers.Security.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.neocle.flexbans.configs.ConfigManager;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class DomainFilter extends AbstractHandler {
    private final Logger logger;

    public DomainFilter(Logger logger) {
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
        String hostHeader = request.getHeader("Host");

        if (hostHeader == null || !hostHeader.equalsIgnoreCase(getAllowedURL())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Cannot access web interface from this url.");
            baseRequest.setHandled(true);
            return;
        }
    }
}
