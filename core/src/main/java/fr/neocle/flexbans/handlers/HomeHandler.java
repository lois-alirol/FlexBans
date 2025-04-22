package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.utils.ResourceLoader;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class HomeHandler extends AbstractHandler {

    public HomeHandler() {
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if ("/home.html".equals(target) || "/".equals(target)) {
            String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/home.html");
            if (htmlTemplate == null) {
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
            String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
            String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
            String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
            String serverName = (String) ConfigManager.getConfigValue("server-display.name");
            String serverDescription = (String) ConfigManager.getConfigValue("server-display.description");

            String pageContent = htmlTemplate
                    .replace("{{server_name}}", serverName)
                    .replace("{{server_description}}", serverDescription)
                    .replace("{{server_favicon}}", serverFavicon)
                    .replace("{{server_color}}", serverColor)
                    .replace("{{server_color_hover}}", serverColorDarker)
                    .replace("{{server_logo}}", serverLogo);

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(pageContent);
            baseRequest.setHandled(true);
        }
    }
}
