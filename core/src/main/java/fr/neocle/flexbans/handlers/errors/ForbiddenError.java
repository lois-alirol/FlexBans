package fr.neocle.flexbans.handlers.errors;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.utils.ResourceLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class ForbiddenError {
    private final Logger logger;

    public ForbiddenError(Logger logger) {
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

        String serverIcon = ConfigManager.getString("server-display.icon");
        String serverFavicon = ConfigManager.getString("server-display.favicon");
        String serverLogo = ConfigManager.getString("server-display.logo");
        String serverColor = ConfigManager.getString("server-display.color");
        String serverColorDarker = ConfigManager.getString("server-display.darker-color");
        String serverName = ConfigManager.getString("server-display.name");

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
