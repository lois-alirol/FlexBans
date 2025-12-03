package fr.neocle.flexbans.handler;

import fr.neocle.flexbans.handler.error.NotFoundError;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JavascriptHandler extends AbstractHandler {
    private final NotFoundError notFoundError;

    public JavascriptHandler(NotFoundError notFoundError) {
        this.notFoundError = notFoundError;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/js/")) {
            String resourcePath = "/web/js" + target.substring(3);
            try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    notFoundError.handle(request, response);
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/javascript");
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
