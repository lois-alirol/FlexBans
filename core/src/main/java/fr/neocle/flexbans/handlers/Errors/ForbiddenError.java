package fr.neocle.flexbans.handlers.Errors;

import fr.neocle.flexbans.utils.ResourceLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class ForbiddenError {
    private final Logger logger;
    private final Map<String, Object> config;

    public ForbiddenError(Map<String, Object> config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/errors/403.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for 403 Forbidden error.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server-display");
        String serverIcon = String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png"));
        String serverFavicon = String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png"));
        String serverLogo = String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png"));
        String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
        String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker-color", "#207dd2"));
        String serverName = String.valueOf(serverDisplaySettings.getOrDefault("name", "Example"));

        String pageContent = htmlTemplate
                .replace("{{server_icon}}", serverIcon)
                .replace("{{server_name}}", serverName)
                .replace("{{server_favicon}}", serverFavicon)
                .replace("{{server_color}}", serverColor)
                .replace("{{server_color_hover}}", serverColorDarker)
                .replace("{{server_logo}}", serverLogo);

        response.getWriter().write(pageContent);


    }
}
