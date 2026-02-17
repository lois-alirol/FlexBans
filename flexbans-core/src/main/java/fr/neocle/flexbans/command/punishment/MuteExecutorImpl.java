package fr.neocle.flexbans.command.punishment;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.api.punishment.MuteExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.command.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager.PunishmentType;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;

import java.net.InetAddress;
import java.util.Optional;
import java.util.function.Consumer;

public class MuteExecutorImpl implements MuteExecutor {
    private final MutePlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private final UuidUsernameResolver resolver;

    private final static String SILENT_PERMISSION = "flexbans.mute.silent";

    public MuteExecutorImpl(MutePlatformHandler platformHandler,
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

    public void executeMute(String target,
                            String sender,
                            String duration,
                            String reason,
                            String serverScope,
                            String serverOrigin,
                            boolean silent,
                            boolean ipScope,
                            Consumer<String> messageSender) {

        Optional<PlayerInfo> targetInfo = Common.resolveTarget(target, messageSender, floodgateApi);
        if (targetInfo.isEmpty()) return;

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi);
        if (senderInfo.isEmpty()) return;

        PlayerInfo targetPlayer = targetInfo.get();
        PlayerInfo senderPlayer = senderInfo.get();

        if (databaseUtils.getPunishmentsManager().isPlayerPunished(PunishmentType.MUTE, targetPlayer.uuid(), serverScope)) {
            databaseUtils.getPunishmentsManager().removePunishment(
                    PunishmentType.MUTE,
                    targetPlayer.uuid(),
                    senderPlayer.uuid(),
                    senderPlayer.name(),
                    "Overridden",
                    serverScope
            );
            messageSender.accept("Previous active mute for this player was removed.");
        }

        processMute(targetPlayer, senderPlayer, duration, reason, serverScope, serverOrigin, silent, ipScope);
    }

    private void processMute(PlayerInfo target,
                             PlayerInfo sender,
                             String duration,
                             String reason,
                             String serverScope,
                             String serverOrigin,
                             boolean silent,
                             boolean ipScope) {

        String muteReason = resolveReason(reason);
        long muteDuration = Common.parseDuration(duration);
        String displayDuration = resolveDuration(duration);

        applyMuteToSystem(target, sender, displayDuration, muteReason, muteDuration, serverScope, serverOrigin, silent, ipScope);

        broadcastMute(target.name(), sender.name(), muteReason, displayDuration, silent);
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

    private void applyMuteToSystem(PlayerInfo target,
                                   PlayerInfo sender,
                                   String displayDuration,
                                   String muteReason,
                                   long muteDuration,
                                   String serverScope,
                                   String serverOrigin,
                                   boolean silent,
                                   boolean ipScope) {

        platformHandler.applyMute(target.name(), target.uuid(), sender.name(), displayDuration, muteReason, serverScope);

        InetAddress targetIp = databaseUtils.getProfilesManager().getIp(target.name());

        databaseUtils.getPunishmentsManager().insertPunishment(
                PunishmentType.MUTE,
                target.uuid(), targetIp,
                sender.uuid(), sender.name(),
                muteReason, muteDuration,
                serverScope, serverOrigin,
                silent, ipScope
        );

        eventDispatcher.muteAddedEvent(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                muteReason, muteDuration,
                serverScope, serverOrigin,
                silent, ipScope
        );
    }

    private void broadcastMute(String targetName, String senderName, String reason, String duration, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.mute.silent-broadcast-message"
                                : "punishments.mute.broadcast-message"
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
