package fr.neocle.flexbans.handlers.security.utils;

import fr.neocle.flexbans.configs.ConfigManager;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

public class DomainFilter extends AbstractHandler {
    private final Logger logger;

    public DomainFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String hostHeader = request.getHeader("Host");
        String allowedHost = getAllowedHost();

        if (hostHeader == null || !hostHeader.split(":")[0].equalsIgnoreCase(allowedHost)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Cannot access web interface from this url.");
            baseRequest.setHandled(true);
            return;
        }
    }

    public String getAllowedHost() {
        String url = ConfigManager.getString("webserver.url");

        if (url == null) {
            logger.severe("No URL specified in the configuration. It is required to properly access the web interface.");
            return "";
        }

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            logger.severe("Invalid URL format in configuration: " + url);
            return "";
        }
    }
}
