package fr.neocle.flexbans.handlers.Errors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.utils.ResourceLoader;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class NotFoundError {
    private final Logger logger;

    public NotFoundError(Logger logger) {
        this.logger = logger;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/errors/404.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for 404 Not Found error.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }

        String serverIcon = (String) ConfigManager.getConfigValue("server-display.icon");
        String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
        String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
        String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
        String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
        String serverName = (String) ConfigManager.getConfigValue("server-display.name");

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
