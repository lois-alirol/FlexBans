package fr.neocle.flexbans.api.server;

import java.util.function.Consumer;

public interface ServerLockExecutor {
    void lockServer(String serverName, String sender, String duration, String reason,
                    String serverOrigin, boolean silent, Consumer<String> messageSender);
}
