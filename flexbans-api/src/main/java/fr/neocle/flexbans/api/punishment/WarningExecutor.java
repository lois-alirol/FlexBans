package fr.neocle.flexbans.api.punishment;

import java.util.function.Consumer;

public interface WarningExecutor {
    void executeWarning(String target, String sender, String reason, String serverScope,
                        String serverOrigin, boolean silent, boolean ipScope, Consumer<String> messageSender);
}
