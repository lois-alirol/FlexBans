package fr.neocle.flexbans.handler.web.api;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class PunishmentSSEHandler extends AbstractHandler {
    private final CopyOnWriteArrayList<ServletOutputStream> clients = new CopyOnWriteArrayList<>();

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!target.equals("/live/punishments")) return;

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        ServletOutputStream out = response.getOutputStream();
        clients.add(out);
        baseRequest.setHandled(true);

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(30000);
                out.print("event: ping\ndata: keepalive\n\n");
                out.flush();
            }
        } catch (InterruptedException ignored) {
        } finally {
            clients.remove(out);
        }
    }

    public void broadcastUpdate(String jsonUpdate) {
        clients.forEach(out -> {
            try {
                out.print("event: update\ndata: " + jsonUpdate + "\n\n");
                out.flush();
            } catch (IOException e) {
                clients.remove(out);
            }
        });
    }
}
