package fr.neocle.flexbans.handler.web;

import fr.neocle.flexbans.handler.web.error.NotFoundError;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CssHandler extends AbstractHandler {
    private final NotFoundError notFoundError;

    public CssHandler(NotFoundError notFoundError) {
        this.notFoundError = notFoundError;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/css/")) {
            String resourcePath = "/web/css" + target.substring(4);
            try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    notFoundError.handle(request, response);
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("text/css");
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }
                baseRequest.setHandled(true);
            }
        }
    }
}
