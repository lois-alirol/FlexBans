package fr.neocle.flexbans.command.punishment.ban.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VelocityBan implements BanPlatformHandler {
    private final ProxyServer proxy;
    private final ProfilesManager profilesManager;

    public VelocityBan(ProxyServer proxy, ProfilesManager profilesManager) {
        this.proxy = proxy;
        this.profilesManager = profilesManager;
    }

    @Override
    public void applyBan(String target, UUID uuid, String issuer_name, String duration,
                         String reason, String serverScope, boolean isIpBan) {

        try {
            Component banMsg = buildBanMessage(reason, issuer_name, duration);

            if (uuid != null) {
                proxy.getPlayer(uuid).ifPresent(player -> {
                    if (isInServerScope(player, serverScope)) {
                        player.disconnect(banMsg);
                    }
                });
            } else if (target != null && !target.isEmpty()) {
                proxy.getPlayer(target).ifPresent(player -> {
                    if (isInServerScope(player, serverScope)) {
                        player.disconnect(banMsg);
                    }
                });
            }

            UUID resolvedUuid = uuid;
            if (resolvedUuid == null && target != null && !target.isEmpty()) {
                proxy.getPlayer(target).ifPresent(p -> {
                });

                Player p = proxy.getPlayer(target).orElse(null);
                if (p != null) resolvedUuid = p.getUniqueId();
            }

            if (resolvedUuid != null) {
                List<String> bannedIps = profilesManager.getAllIps(resolvedUuid);
                if (bannedIps != null && !bannedIps.isEmpty()) {
                    for (Player online : proxy.getAllPlayers()) {
                        try {
                            String onlineIp = online.getRemoteAddress().getAddress().getHostAddress();
                            for (String bannedIp : bannedIps) {
                                if (bannedIp != null && bannedIp.equals(onlineIp)) {
                                    if (isInServerScope(online, serverScope)) {
                                        online.disconnect(banMsg);
                                    }
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isInServerScope(Player player, String serverScope) {
        if (serverScope == null || serverScope.isEmpty() || serverScope.equalsIgnoreCase("global")) {
            return true;
        }

        return player.getCurrentServer()
                .map(s -> s.getServerInfo().getName().equalsIgnoreCase(serverScope))
                .orElse(false);
    }

    private Component buildBanMessage(String reason, String issuer, String duration) {
        String raw = LanguageManager.getMessageString("punishments.ban.disconnect-message");
        if (raw == null || raw.isEmpty()) {
            return Component.text("You are banned.");
        }

        raw = raw
                .replace("%reason%", reason != null ? reason : "No reason specified")
                .replace("%moderator%", issuer != null ? issuer : "Console")
                .replace("%date%", LocalDate.now().toString())
                .replace("%duration%", duration != null ? duration : "Permanent");

        return MiniMessage.miniMessage().deserialize(raw);
    }
}
