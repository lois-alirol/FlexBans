package fr.neocle.flexbans.velocity.handler.punishment;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class WarningHandler implements WarningPlatformHandler {
    private final ProxyServer proxy;
    private final FlexBansAPI api;

    public WarningHandler(ProxyServer proxy) {
        this.proxy = proxy;
        this.api = FlexBansAPI.getInstance();
    }

    @Override
    public void applyWarning(String target, UUID uuid, String issuer_name, String reason, String serverScope) {
        Player player = uuid != null
                ? proxy.getPlayer(uuid).orElse(null)
                : proxy.getPlayer(target).orElse(null);

        if (player != null) {

            if (serverScope != null && !serverScope.isEmpty() && !serverScope.equalsIgnoreCase("global")) {
                if (!player.getCurrentServer().isPresent() ||
                        !player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(serverScope)) {
                    return;
                }
            }

            String rawMessage = LanguageManager.getMessageString("punishments.warning.alert-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuer_name)
                    .replace("%date%", LocalDate.now().toString());

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.sendMessage(Component.text("punishments.warning.alert-message"));
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);

            player.sendMessage(formattedMessage);
        }
    }

    @Override
    public void executeWarningAction(String target, UUID uuid, int warningCount, String issuer_name,
                                     String serverScope, String serverOrigin, boolean ipScope) {
        Map<String, Object> actionsConfig = ConfigManager.getMap(
                "punishments-system.built-in.warnings.actions"
        );

        if (actionsConfig.isEmpty()) {
            return;
        }

        boolean enabled = ConfigManager.getBoolean("punishments-system.built-in.warnings.actions.enabled");
        if (!enabled) {
            return;
        }

        Map<String, Object> thresholds = ConfigManager.getMap(
                "punishments-system.built-in.warnings.actions.action-thresholds"
        );
        if (thresholds.isEmpty()) {
            return;
        }

        Map<String, Object> actionConfig = ConfigManager.getMap(
                "punishments-system.built-in.warnings.actions.action-thresholds." + warningCount
        );
        if (actionConfig.isEmpty()) {
            return;
        }

        String actionType = ConfigManager.getString(
                "punishments-system.built-in.warnings.actions.action-thresholds." + warningCount + ".type"
        );
        if (actionType.isEmpty()) {
            return;
        }

        String duration = actionConfig.getOrDefault("duration", "PERMANENT").toString();
        boolean broadcast = Boolean.parseBoolean(actionConfig.getOrDefault("broadcast", true).toString());

        String reason = "Reached " + warningCount + " warnings";

        switch (actionType.toUpperCase()) {
            case "MUTE" -> api.getMuteExecutor().executeMute(
                    target, issuer_name, duration, reason, serverScope, serverOrigin, !broadcast, false,
                    message -> {
                        Player p = uuid != null ? proxy.getPlayer(uuid).orElse(null) : proxy.getPlayer(target).orElse(null);
                        if (p != null) p.sendMessage(Component.text(message));
                    }
            );

            case "BAN" -> api.getBanExecutor().executeBan(
                    target, issuer_name, duration, reason, serverScope, serverOrigin, !broadcast, false,
                    message -> {
                        Player p = uuid != null ? proxy.getPlayer(uuid).orElse(null) : proxy.getPlayer(target).orElse(null);
                        if (p != null) p.sendMessage(Component.text(message));
                    }
            );

            case "KICK" -> api.getKickExecutor().executeKick(
                    target, issuer_name, reason, serverOrigin, !broadcast, false,
                    message -> {
                        Player p = uuid != null ? proxy.getPlayer(uuid).orElse(null) : proxy.getPlayer(target).orElse(null);
                        if (p != null) p.sendMessage(Component.text(message));
                    }
            );
        }
    }
}