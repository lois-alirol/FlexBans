package fr.neocle.flexbans.command.punishment;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.platform.handler.BanPlatformHandler;
import fr.neocle.flexbans.api.punishment.BanExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager.PunishmentType;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;
import fr.neocle.flexbans.command.Common.PlayerInfo;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BanExecutorImpl implements BanExecutor {
    private final BanPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UuidUsernameResolver resolver;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private final static String SILENT_PERMISSION = "flexbans.ban.silent";

    public BanExecutorImpl(BanPlatformHandler platformHandler,
                           Broadcaster broadcaster,
                           DatabaseUtils databaseUtils,
                           EventDispatcher eventDispatcher) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;

        this.resolver = UuidUsernameResolver.get();
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeBan(String target,
                           String sender,
                           String duration,
                           String reason,
                           String serverScope,
                           String serverOrigin,
                           boolean silent,
                           boolean ipScope,
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
                .isPlayerPunished(PunishmentType.BAN, targetPlayer.uuid(), serverScope)
                .thenCompose(isPunished -> {
                    if (!isPunished) return CompletableFuture.completedFuture(false);
                    return databaseUtils.getPunishmentsManager().removePunishment(
                            PunishmentType.BAN,
                            targetPlayer.uuid(),
                            senderPlayer.uuid(),
                            senderPlayer.name(),
                            "Overridden",
                            serverScope
                    );
                })
                .thenAccept(removed -> {
                    if (removed) {
                        messageSender.accept("Previous active ban for this player was removed.");
                    }
                });

        processBan(targetPlayer, senderPlayer, duration, reason, serverScope, serverOrigin, silent, ipScope);
    }

    private void processBan(PlayerInfo target,
                            PlayerInfo sender,
                            String duration,
                            String reason,
                            String serverScope,
                            String serverOrigin,
                            boolean silent,
                            boolean ipScope) {

        String banReason = resolveReason(reason);
        long banDuration = Common.parseDuration(duration);
        String displayDuration = resolveDuration(duration);

        applyBanToSystem(target, sender, displayDuration, banReason, banDuration, serverScope, serverOrigin, silent, ipScope);

        broadcastBan(target.name(), sender.name(), banReason, displayDuration, silent);
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-reason");
    }

    private String resolveDuration(String duration) {
        return (duration != null && !duration.isEmpty())
                ? duration
                : LanguageManager.getMessageString("punishments.infinite-duration");
    }

    private void applyBanToSystem(PlayerInfo target,
                                  PlayerInfo sender,
                                  String displayDuration,
                                  String banReason,
                                  long banDuration,
                                  String serverScope,
                                  String serverOrigin,
                                  boolean silent,
                                  boolean ipScope) {

        platformHandler.applyBan(target.name(), target.uuid(), sender.name(), displayDuration, banReason, serverScope, ipScope);

        databaseUtils.getProfilesManager()
                .getIp(target.name())
                .thenCompose(targetIp ->
                        databaseUtils.getPunishmentsManager().insertPunishment(
                                PunishmentType.BAN,
                                target.uuid(),
                                targetIp,
                                sender.uuid(),
                                sender.name(),
                                banReason,
                                banDuration,
                                serverScope,
                                serverOrigin,
                                silent,
                                ipScope
                        )
                );

        eventDispatcher.banAddedEvent(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                banReason, banDuration,
                serverScope, serverOrigin,
                silent, ipScope
        );
    }

    private void broadcastBan(String targetName, String senderName, String reason, String duration, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.ban.silent-broadcast-message"
                                : "punishments.ban.broadcast-message"
                )
                .replace("%target%", targetName)
                .replace("%reason%", reason)
                .replace("%moderator%", senderName)
                .replace("%duration%", duration);

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