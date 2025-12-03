package fr.neocle.flexbans.handler.error;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.util.ResourceLoader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

        String serverIcon = ConfigManager.getString("server-display.icon");
        String serverFavicon = ConfigManager.getString("server-display.favicon");
        String serverLogo = ConfigManager.getString("server-display.logo");
        String serverColor = ConfigManager.getString("server-display.color");
        String serverColorDarker = ConfigManager.getString("server-display.darker-color");
        String serverName = ConfigManager.getString("server-display.name");

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
