package fr.neocle.flexbans.api.punishment;

import java.util.function.Consumer;

public interface KickExecutor {
    void executeKick(String target, String sender, String reason,
                     String serverOrigin, boolean silent, boolean ipScope,
                     Consumer<String> messageSender);
}
