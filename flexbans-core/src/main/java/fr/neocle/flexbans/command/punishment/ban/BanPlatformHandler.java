package fr.neocle.flexbans.command.punishment.ban;

import java.util.UUID;

public interface BanPlatformHandler {
    void applyBan(String target, UUID uuid, String issuer_name, String duration,
                  String reason, String serverScope, boolean isIpScope);
}

