package fr.neocle.flexbans.api.platform.handler;

import java.util.UUID;

public interface WarningPlatformHandler {
    void applyWarning(String target, UUID uuid, String issuer_name, String reason, String serverScope);

    void executeWarningAction(String target, UUID uuid, int warningCount, String issuer_name, String serverScope, String serverOrigin, boolean ipScope);
}
