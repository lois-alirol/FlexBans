package fr.neocle.flexbans.handler;

import fr.neocle.flexbans.handler.error.NotFoundError;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class PlayerHeadHandler extends AbstractHandler {
    private final Path dataFolder;
    private final NotFoundError notFoundError;

    public PlayerHeadHandler(Path dataFolder, NotFoundError notFoundError) {
        this.notFoundError = notFoundError;
        this.dataFolder = dataFolder;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/player-heads/")) {
            String filename = target.replace("/player-heads/", "");
            File playerHeadFile = new File(dataFolder.toFile(), "cache/heads/" + filename + ".png");

            if (playerHeadFile.exists() && playerHeadFile.isFile()) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("image/png");

                try (FileInputStream fis = new FileInputStream(playerHeadFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                    }
                }
                baseRequest.setHandled(true);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                notFoundError.handle(request, response);
                baseRequest.setHandled(true);
            }
        }
    }
}
