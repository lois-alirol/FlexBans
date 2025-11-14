package fr.neocle.flexbans.api.server;

import java.util.function.Consumer;

public interface ServerUnlockExecutor {
    void unlockServer(String serverName, String sender, boolean silent, Consumer<String> messageSender);
}
