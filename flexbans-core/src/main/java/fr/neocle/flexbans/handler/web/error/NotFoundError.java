package fr.neocle.flexbans.handler.web.error;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.ResourceLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class NotFoundError {

    public NotFoundError() {}

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/errors/404.html");
        if (htmlTemplate == null) {
            FlexLogger.warn("Unable to load HTML template for 404 Not Found error.");
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
