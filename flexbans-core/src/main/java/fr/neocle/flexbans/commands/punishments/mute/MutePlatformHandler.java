package fr.neocle.flexbans.commands.punishments.mute;

import java.util.UUID;

public interface MutePlatformHandler {
    void applyMute(String target, UUID uuid, String issuer_name, String duration,
                   String reason, String serverScope);
}

