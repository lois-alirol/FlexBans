package fr.neocle.flexbans.commands.punishments.unban;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.commands.punishments.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.api.PunishmentSSEHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.broadcast.Broadcaster;
import fr.neocle.flexbans.utils.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class UnbanExecutor implements fr.neocle.flexbans.api.punishments.UnbanExecutor {
    private final Broadcaster broadcaster;
    private final DatabaseUtils databaseUtils;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final FloodgateApi floodgateApi;
    private final EventDispatcher eventDispatcher;
    private final PunishmentSSEHandler punishmentSSEHandler;

    private static final String SILENT_PERMISSION = "flexbans.unban.silent";

    public UnbanExecutor(Broadcaster broadcaster,
                         UsernameUUIDConverters usernameUUIDConverters,
                         DatabaseUtils databaseUtils,
                         EventDispatcher eventDispatcher,
                         PunishmentSSEHandler punishmentSSEHandler) {
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.punishmentSSEHandler = punishmentSSEHandler;

        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeUnban(String target,
                             String sender,
                             String reason,
                             String serverScope,
                             boolean silent,
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

        if (!databaseUtils.getBansManager().isPlayerBanned(targetPlayer.uuid(), serverScope)) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unban.not-banned")
                    .replace("%target%", targetPlayer.name()));
            return;
        }

        processUnban(targetPlayer, senderPlayer, reason, serverScope, silent);
    }

    private void processUnban(PlayerInfo target,
                              PlayerInfo sender,
                              String reason,
                              String serverScope,
                              boolean silent) {

        String unbanReason = resolveReason(reason);

        databaseUtils.getBansManager().removeBan(target.uuid(), sender.uuid(), sender.name(), unbanReason);

        /*eventDispatcher.unbanAddedEvent(target.uuid(), target.name(), sender.uuid(), sender.name(), unbanReason, serverScope, silent);
        punishmentSSEHandler.sendUnbanUpdate(target.uuid(), sender.name(), unbanReason, serverScope);*/

        broadcastUnban(target.name(), sender.name(), unbanReason, silent);
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-unban-reason");
    }

    private void broadcastUnban(String targetName, String senderName, String reason, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.unban.silent-broadcast-message"
                                : "punishments.unban.broadcast-message"
                )
                .replace("%target%", targetName)
                .replace("%moderator%", senderName)
                .replace("%reason%", reason);

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
