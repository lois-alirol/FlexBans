package fr.neocle.flexbans.handlers.Errors;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import fr.neocle.flexbans.utils.ResourceLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Logger;

public class InternalServerError extends ErrorHandler {
    private final Logger logger;
    private final Map<String, Object> config;

    public InternalServerError(Map<String, Object> config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int statusCode = response.getStatus();
        Throwable cause = (Throwable) request.getAttribute("javax.servlet.error.exception");

        if (statusCode == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            baseRequest.setHandled(true);

        } else {
            super.handle(target, baseRequest, request, response);
        }

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/errors/500.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for 500 Internal Server Error.");
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

        String stackTrace = "";
        if (cause != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            cause.printStackTrace(pw);
            stackTrace = sw.toString();
        }

        String pageContent = htmlTemplate
                .replace("{{stack_trace}}", escapeHtml(stackTrace))
                .replace("{{server_name}}", serverName)
                .replace("{{server_icon}}", serverIcon)
                .replace("{{server_favicon}}", serverFavicon)
                .replace("{{server_color}}", serverColor)
                .replace("{{server_color_hover}}", serverColorDarker)
                .replace("{{server_logo}}", serverLogo);

        response.getWriter().write(pageContent);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
