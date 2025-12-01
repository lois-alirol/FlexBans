package fr.neocle.flexbans.commands.punishments.ban;

import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.api.PunishmentSSEHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.broadcast.Broadcaster;
import fr.neocle.flexbans.utils.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;
import fr.neocle.flexbans.commands.punishments.Common.PlayerInfo;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BanExecutor implements fr.neocle.flexbans.api.punishments.BanExecutor {
    private final BanPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;
    private final PunishmentSSEHandler punishmentSSEHandler;

    private final static String SILENT_PERMISSION = "flexbans.ban.silent";

    public BanExecutor(BanPlatformHandler platformHandler,
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

    public void executeBan(String target,
                           String sender,
                           String duration,
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

        handleExistingBan(targetPlayer.uuid(), serverScope, messageSender);
        processBan(targetPlayer, senderPlayer, duration, reason, serverScope, serverOrigin, silent, ipScope);
    }

    private void handleExistingBan(UUID targetUUID, String serverScope, Consumer<String> messageSender) {
        if (databaseUtils.getBansManager().isPlayerBanned(targetUUID, serverScope)) {
            databaseUtils.getBansManager().updatePreviousBanStatus(targetUUID);
            messageSender.accept("Previous active ban for this player was removed.");
        }
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

        platformHandler.applyBan(target.name(), target.uuid(), sender.name(), displayDuration, banReason, serverScope);

        databaseUtils.getBansManager().insertBan(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                banReason, banDuration,
                serverScope, serverOrigin,
                silent, ipScope
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