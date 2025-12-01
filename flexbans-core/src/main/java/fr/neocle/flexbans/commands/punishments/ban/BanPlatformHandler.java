package fr.neocle.flexbans.commands.punishments.ban;

import java.util.Optional;
import java.util.UUID;

public interface BanPlatformHandler {
    void applyBan(String target, UUID uuid, String issuer_name, String duration,
                  String reason, String serverScope);
}

