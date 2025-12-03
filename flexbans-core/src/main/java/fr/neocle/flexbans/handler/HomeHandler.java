package fr.neocle.flexbans.handler;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.util.ResourceLoader;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

            String serverFavicon = ConfigManager.getString("server-display.favicon");
            String serverLogo = ConfigManager.getString("server-display.logo");
            String serverColor = ConfigManager.getString("server-display.color");
            String serverColorDarker = ConfigManager.getString("server-display.darker-color");
            String serverName = ConfigManager.getString("server-display.name");
            String serverDescription = ConfigManager.getString("server-display.description");

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
