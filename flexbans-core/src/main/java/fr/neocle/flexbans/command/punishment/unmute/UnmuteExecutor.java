package fr.neocle.flexbans.command.punishment.unmute;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.command.punishment.Common;
import fr.neocle.flexbans.command.punishment.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.web.api.PunishmentSSEHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.function.Consumer;

public class UnmuteExecutor implements fr.neocle.flexbans.api.punishment.UnmuteExecutor {
    private final Broadcaster broadcaster;
    private final DatabaseUtils databaseUtils;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final FloodgateApi floodgateApi;
    private final EventDispatcher eventDispatcher;
    private final PunishmentSSEHandler punishmentSSEHandler;

    private static final String SILENT_PERMISSION = "flexbans.unmute.silent";

    public UnmuteExecutor(Broadcaster broadcaster,
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

    public void executeUnmute(String target,
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

        if (!databaseUtils.getMutesManager().isPlayerMuted(targetPlayer.uuid(), serverScope)) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unmute.not-muted")
                    .replace("%target%", targetPlayer.name()));
            return;
        }

        processUnmute(targetPlayer, senderPlayer, reason, serverScope, silent);
    }

    private void processUnmute(PlayerInfo target,
                               PlayerInfo sender,
                               String reason,
                               String serverScope,
                               boolean silent) {

        String unmuteReason = resolveReason(reason);

        databaseUtils.getMutesManager().removeMute(target.uuid(), sender.uuid(), sender.name(), unmuteReason);

        /*eventDispatcher.unmuteAddedEvent(target.uuid(), target.name(), sender.uuid(), sender.name(), unmuteReason, serverScope, silent);
        punishmentSSEHandler.sendUnmuteUpdate(target.uuid(), sender.name(), unmuteReason, serverScope);*/

        broadcastUnmute(target.name(), sender.name(), unmuteReason, silent);
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-unmute-reason");
    }

    private void broadcastUnmute(String targetName, String senderName, String reason, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.unmute.silent-broadcast-message"
                                : "punishments.unmute.broadcast-message"
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
