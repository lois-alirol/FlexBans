package fr.neocle.flexbans.command.punishment;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.punishment.UnbanExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.command.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UnbanExecutorImpl implements UnbanExecutor {
    private final Broadcaster broadcaster;
    private final DatabaseUtils databaseUtils;
    private final UuidUsernameResolver resolver;
    private final FloodgateApi floodgateApi;
    private final EventDispatcher eventDispatcher;

    private static final String SILENT_PERMISSION = "flexbans.unban.silent";

    public UnbanExecutorImpl(Broadcaster broadcaster,
                             DatabaseUtils databaseUtils,
                             EventDispatcher eventDispatcher) {
        this.broadcaster = broadcaster;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;

        this.resolver = UuidUsernameResolver.get();
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeUnban(String target,
                             String sender,
                             String serverScope,
                             String reason,
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

        databaseUtils.getPunishmentsManager()
                .isPlayerPunished(PunishmentsManager.PunishmentType.BAN, targetPlayer.uuid(), serverScope)
                .thenAccept(isPunished -> {
                    if (!isPunished) {
                        messageSender.accept(
                                LanguageManager.getMessageString("punishments.unban.not-banned")
                                        .replace("%target%", targetPlayer.name())
                        );
                    }
                });

        processUnban(targetPlayer, senderPlayer, reason, serverScope, silent);
    }

    public void executeUnban(int punishmentId,
                             String sender,
                             String serverScope,
                             String reason,
                             boolean silent,
                             Consumer<String> messageSender) {

        Optional<PlayerInfo> senderInfo =
                Common.resolveSender(sender, messageSender, floodgateApi);

        if (senderInfo.isEmpty()) return;

        PlayerInfo senderPlayer = senderInfo.get();

        databaseUtils.getPunishmentsManager()
                .getActivePunishmentById(punishmentId)
                .thenCompose(punishmentOpt -> {
                    if (punishmentOpt.isEmpty()) {
                        messageSender.accept(LanguageManager.getMessageString("punishments.unban.invalid-id"));
                        return CompletableFuture.completedFuture(false);
                    }

                    PunishmentsManager.PunishmentInfo punishment = punishmentOpt.get();
                    String unbanReason = resolveReason(reason);

                    return databaseUtils.getPunishmentsManager()
                            .removePunishmentById(
                                    punishmentId,
                                    senderPlayer.uuid(),
                                    senderPlayer.name(),
                                    unbanReason
                            )
                            .thenApply(success -> {
                                if (!success) {
                                    messageSender.accept(LanguageManager.getMessageString("punishments.unban.failed"));
                                    return false;
                                }

                                // Event ou broadcast
                                broadcastUnban(
                                        resolver.uuidToUsername(punishment.targetUuid),
                                        senderPlayer.name(),
                                        unbanReason,
                                        silent
                                );

                                return true;
                            });
                });
    }

    private void processUnban(PlayerInfo target,
                              PlayerInfo sender,
                              String reason,
                              String serverScope,
                              boolean silent) {

        String unbanReason = resolveReason(reason);

        databaseUtils.getPunishmentsManager().removePunishment(
                PunishmentsManager.PunishmentType.BAN,
                target.uuid(),
                sender.uuid(),
                sender.name(),
                unbanReason,
                serverScope
        );

        /*eventDispatcher.unbanAddedEvent(target.uuid(), target.name(), sender.uuid(), sender.name(), unbanReason, serverScope, silent);
        */

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
