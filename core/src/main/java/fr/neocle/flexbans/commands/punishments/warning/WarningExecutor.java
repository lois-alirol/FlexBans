package fr.neocle.flexbans.commands.punishments.warning;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.api.PunishmentSSEHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.broadcast.Broadcaster;
import fr.neocle.flexbans.utils.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;
import fr.neocle.flexbans.commands.punishments.Common.PlayerInfo;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WarningExecutor implements fr.neocle.flexbans.api.punishments.WarningExecutor {
    private final WarningPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;
    private final PunishmentSSEHandler punishmentSSEHandler;

    private final static String SILENT_PERMISSION = "flexbans.warning.silent";

    public WarningExecutor(WarningPlatformHandler platformHandler,
                           Broadcaster broadcaster,
                           UsernameUUIDConverters usernameUUIDConverters,
                           DatabaseUtils databaseUtils,
                           EventDispatcher eventDispatcher,
                           PunishmentSSEHandler punishmentSSEHandler) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.punishmentSSEHandler = punishmentSSEHandler;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeWarning(String target,
                               String sender,
                               String reason,
                               String serverScope,
                               String serverOrigin,
                               boolean silent,
                               boolean ipScope,
                               Consumer<String> messageSender) {

        Optional<PlayerInfo> targetInfo = Common.resolveTarget(target, messageSender, floodgateApi, usernameUUIDConverters);
        if (targetInfo.isEmpty()) {
            return;
        }

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi, usernameUUIDConverters);
        if (senderInfo.isEmpty()) {
            return;
        }

        PlayerInfo targetPlayer = targetInfo.get();
        PlayerInfo senderPlayer = senderInfo.get();

        processWarning(targetPlayer, senderPlayer, reason, serverScope, serverOrigin, silent, ipScope, messageSender);
    }

    private void processWarning(PlayerInfo target,
                                PlayerInfo sender,
                                String reason,
                                String serverScope,
                                String serverOrigin,
                                boolean silent,
                                boolean ipScope,
                                Consumer<String> messageSender) {

        String warningReason = resolveReason(reason);

        int warningCount = databaseUtils.getWarningsManager().getWarningCount(target.uuid(), serverScope) + 1;

        applyWarningToSystem(target, sender, warningReason, serverScope, serverOrigin, silent, ipScope, warningCount);
        broadcastWarning(target.name(), sender.name(), warningReason, warningCount + 1, silent);

        messageSender.accept("§aWarning issued successfully. " + target.name() + " now has " + (warningCount + 1) + " active warning(s).");
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-reason");
    }

    private void applyWarningToSystem(PlayerInfo target,
                                      PlayerInfo sender,
                                      String warningReason,
                                      String serverScope,
                                      String serverOrigin,
                                      boolean silent,
                                      boolean ipScope,
                                      int warningCount) {

        platformHandler.applyWarning(target.name(), target.uuid(), sender.name(), warningReason, serverScope);
        platformHandler.executeWarningAction(target.name(), target.uuid(), warningCount, sender.name(), serverScope, serverOrigin, silent);

        int expiresAfterDays = ConfigManager.getInt("punishments-system.built-in.warnings.expires-after-days");
        long duration = TimeUnit.DAYS.toMillis(expiresAfterDays);

        databaseUtils.getWarningsManager().insertWarning(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                warningReason, duration,
                serverScope, serverOrigin,
                silent, ipScope
        );

        /*eventDispatcher.warningAddedEvent(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                warningReason,
                serverScope, serverOrigin,
                silent, ipScope
        );*/
    }

    private void broadcastWarning(String targetName, String senderName, String reason, int warningCount, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.warning.silent-broadcast-message"
                                : "punishments.warning.broadcast-message"
                )
                .replace("%target%", targetName)
                .replace("%reason%", reason)
                .replace("%moderator%", senderName)
                .replace("%count%", String.valueOf(warningCount));

        if (rawMessage != null && !rawMessage.isEmpty()) {
            for (String line : rawMessage.split("\n")) {
                if (silent) {
                    broadcaster.execute(line, SILENT_PERMISSION);
                } else {
                    broadcaster.execute(line);
                }
            }
        }
    }
}