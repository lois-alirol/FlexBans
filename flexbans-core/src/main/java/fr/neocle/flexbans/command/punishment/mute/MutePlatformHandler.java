package fr.neocle.flexbans.command.punishment.mute;

import java.util.UUID;

public interface MutePlatformHandler {
    void applyMute(String target, UUID uuid, String issuer_name, String duration,
                   String reason, String serverScope);
}

