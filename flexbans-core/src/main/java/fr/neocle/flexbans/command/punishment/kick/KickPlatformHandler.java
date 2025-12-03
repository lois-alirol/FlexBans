package fr.neocle.flexbans.command.punishment.kick;

import java.util.UUID;

public interface KickPlatformHandler {
    boolean isPlayerOnline(UUID uuid);

    void applyKick(String username, UUID uuid, String sender, String reason);
}