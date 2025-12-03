package fr.neocle.flexbans.api.punishment;

import java.util.function.Consumer;

public interface MuteExecutor {
    void executeMute(String target, String sender, String duration, String reason,
                    String serverScope, String serverOrigin, boolean silent, boolean ipScope,
                    Consumer<String> messageSender);
}


