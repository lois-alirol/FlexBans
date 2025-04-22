package fr.neocle.flexbans.handlers.Errors;

import fr.neocle.flexbans.configs.ConfigManager;
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

    public InternalServerError(Logger logger) {
        this.logger = logger;
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int statusCode = response.getStatus();
        Throwable cause = (Throwable) request.getAttribute("javax.servlet.error.exception");

        if (cause == null) {
            cause = (Throwable) request.getAttribute("fr.neocle.flexbans.exception");
        }

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

        String serverIcon = (String) ConfigManager.getConfigValue("server-display.icon");
        String serverFavicon = (String) ConfigManager.getConfigValue("server-display.favicon");
        String serverLogo = (String) ConfigManager.getConfigValue("server-display.logo");
        String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
        String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
        String serverName = (String) ConfigManager.getConfigValue("server-display.name");

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
