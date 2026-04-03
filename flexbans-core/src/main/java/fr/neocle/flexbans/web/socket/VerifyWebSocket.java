package fr.neocle.flexbans.web.socket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class VerifyWebSocket {
    private static final Map<String, Session> waitingSessions = new ConcurrentHashMap<>();

    private String username;

    public static void notifyVerified(String username) {
        Session session = waitingSessions.remove(username);
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString("{\"type\":\"verified\"}");
                session.close();
            } catch (Exception ignored) {}
        }
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        String query = session.getUpgradeRequest().getQueryString();
        if (query != null && query.startsWith("username=")) {
            this.username = query.substring("username=".length());
            session.setIdleTimeout(Duration.ofMinutes(10).toMillis());
            waitingSessions.put(this.username, session);
        } else {
            try { session.close(1008, "missing username"); } catch (Exception ignored) {}
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int code, String reason) {
        if (username != null) waitingSessions.remove(username);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        if (username != null) waitingSessions.remove(username);
    }
}