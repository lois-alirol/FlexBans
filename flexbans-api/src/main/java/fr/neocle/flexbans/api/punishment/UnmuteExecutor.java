package fr.neocle.flexbans.api.punishment;

import java.util.function.Consumer;

public interface UnmuteExecutor {
    void executeUnmute(String target, String sender, String reason, String serverScope,
                       boolean silent, Consumer<String> messageSender);
}
