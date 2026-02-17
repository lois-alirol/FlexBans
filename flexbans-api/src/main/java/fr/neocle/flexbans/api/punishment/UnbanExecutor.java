package fr.neocle.flexbans.api.punishment;

import java.util.function.Consumer;

public interface UnbanExecutor {
    void executeUnban(String target, String remover, String scope, String reason,
                      boolean silent, Consumer<String> messageSender);

    void executeUnban(int id, String remover, String scope, String reason,
                      boolean silent, Consumer<String> messageSender);
}
