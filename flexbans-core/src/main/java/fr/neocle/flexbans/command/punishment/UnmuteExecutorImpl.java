package fr.neocle.flexbans.command.punishment;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.punishment.UnmuteExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.command.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.function.Consumer;

public class UnmuteExecutorImpl implements UnmuteExecutor {
    private final Broadcaster broadcaster;
    private final DatabaseUtils databaseUtils;
    private final UuidUsernameResolver resolver;
    private final FloodgateApi floodgateApi;
    private final EventDispatcher eventDispatcher;

    private static final String SILENT_PERMISSION = "flexbans.unmute.silent";

    public UnmuteExecutorImpl(Broadcaster broadcaster,
                              DatabaseUtils databaseUtils,
                              EventDispatcher eventDispatcher) {
        this.broadcaster = broadcaster;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;

        this.resolver = UuidUsernameResolver.get();
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeUnmute(String target,
                              String sender,
                              String reason,
                              String serverScope,
                              boolean silent,
                              Consumer<String> messageSender) {

        Optional<PlayerInfo> targetInfo = Common.resolveTarget(target, messageSender, floodgateApi);
        if (targetInfo.isEmpty()) {
            return;
        }

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi);
        if (senderInfo.isEmpty()) {
            return;
        }

        PlayerInfo targetPlayer = targetInfo.get();
        PlayerInfo senderPlayer = senderInfo.get();

        if (!databaseUtils.getPunishmentsManager().isPlayerPunished(PunishmentsManager.PunishmentType.MUTE, targetPlayer.uuid(), serverScope)) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unmute.not-muted")
                    .replace("%target%", targetPlayer.name()));
            return;
        }

        processUnmute(targetPlayer, senderPlayer, reason, serverScope, silent);
    }

    public void executeUnmute(int punishmentId,
                              String sender,
                              String reason,
                              String serverScope,
                              boolean silent,
                              Consumer<String> messageSender) {

        Optional<PlayerInfo> senderInfo =
                Common.resolveSender(sender, messageSender, floodgateApi);
        if (senderInfo.isEmpty()) return;

        PlayerInfo senderPlayer = senderInfo.get();

        Optional<PunishmentsManager.PunishmentInfo> punishmentOpt =
                databaseUtils.getPunishmentsManager().getActivePunishmentById(punishmentId);

        if (punishmentOpt.isEmpty()) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unmute.invalid-id"));
            return;
        }

        PunishmentsManager.PunishmentInfo punishment = punishmentOpt.get();

        if (punishment.type != PunishmentsManager.PunishmentType.MUTE) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unmute.invalid-id"));
            return;
        }

        String unmuteReason = resolveReason(reason);

        boolean success = databaseUtils.getPunishmentsManager().removePunishmentById(
                punishmentId,
                senderPlayer.uuid(),
                senderPlayer.name(),
                unmuteReason
        );

        if (!success) {
            messageSender.accept(LanguageManager.getMessageString("punishments.unmute.failed"));
            return;
        }

        broadcastUnmute(
                resolver.uuidToUsername(punishment.targetUuid),
                senderPlayer.name(),
                unmuteReason,
                silent
        );
    }

    private void processUnmute(PlayerInfo target,
                               PlayerInfo sender,
                               String reason,
                               String serverScope,
                               boolean silent) {

        String unmuteReason = resolveReason(reason);

        databaseUtils.getPunishmentsManager().removePunishment(PunishmentsManager.PunishmentType.MUTE, target.uuid(), sender.uuid(), sender.name(), unmuteReason, serverScope);

        /*eventDispatcher.unmuteAddedEvent(target.uuid(), target.name(), sender.uuid(), sender.name(), unmuteReason, serverScope, silent);
        */

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
