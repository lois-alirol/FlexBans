package fr.neocle.litebansweb.handlers;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.handler.AbstractHandler;

import fr.neocle.litebansweb.utils.ResourceLoader;

public class HomeHandler extends AbstractHandler {
    private final Map<String, Object> config;

    public HomeHandler(Map<String, Object> config) {
        this.config = config;
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

            @SuppressWarnings("unchecked")
            Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server-display");
            String pageContent = htmlTemplate
                    .replace("{{server_name}}", String.valueOf(serverDisplaySettings.getOrDefault("name", "Example")))
                    .replace("{{server_description}}", String.valueOf(serverDisplaySettings.getOrDefault("description", "ExampleServer")))
                    .replace("{{server_icon}}", String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png")))
                    .replace("{{server_favicon}}", String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png")))
                    .replace("{{server_color}}", String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7")))
                    .replace("{{server_color_hover}}", String.valueOf(serverDisplaySettings.getOrDefault("darker-color", "#207dd2")))
                    .replace("{{server_logo}}", String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png")));

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(pageContent);
            baseRequest.setHandled(true);
        }
    }
}
