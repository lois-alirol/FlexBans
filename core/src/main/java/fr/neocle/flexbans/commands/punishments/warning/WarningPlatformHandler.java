package fr.neocle.flexbans.commands.punishments.warning;

import java.util.UUID;
import java.util.function.Consumer;

public interface WarningPlatformHandler {
    void applyWarning(String target, UUID uuid, String issuer_name, String reason, String serverScope);

    void executeWarningAction(String target, UUID uuid, int warningCount, String issuer_name, String serverScope, String serverOrigin, boolean ipScope);
}
