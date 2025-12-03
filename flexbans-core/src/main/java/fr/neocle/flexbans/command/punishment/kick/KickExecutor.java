package fr.neocle.flexbans.command.punishment.kick;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.command.punishment.Common;
import fr.neocle.flexbans.command.punishment.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.function.Consumer;

public class KickExecutor implements fr.neocle.flexbans.api.punishment.KickExecutor {
    private final KickPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private final static String SILENT_PERMISSION = "flexbans.kick.silent";

    public KickExecutor(KickPlatformHandler platformHandler,
                        Broadcaster broadcaster,
                        UsernameUUIDConverters usernameUUIDConverters,
                        DatabaseUtils databaseUtils,
                        EventDispatcher eventDispatcher) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeKick(String target,
                            String sender,
                            String reason,
                            String serverOrigin,
                            boolean silent,
                            boolean ipScope,
                            Consumer<String> messageSender) {

        Optional<PlayerInfo> targetInfo = Common.resolveTarget(target, messageSender, floodgateApi, usernameUUIDConverters);
        if (targetInfo.isEmpty()) {
            return;
        }

        PlayerInfo targetPlayer = targetInfo.get();

        if (!platformHandler.isPlayerOnline(targetPlayer.uuid())) {
            messageSender.accept("§cError: Player '" + targetPlayer.name() + "' is not online.");
            return;
        }

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi, usernameUUIDConverters);
        if (senderInfo.isEmpty()) {
            return;
        }

        PlayerInfo senderPlayer = senderInfo.get();
        processKick(targetPlayer, senderPlayer, reason, serverOrigin, silent, ipScope);
    }

    private void processKick(PlayerInfo target,
                             PlayerInfo sender,
                             String reason,
                             String serverOrigin,
                             boolean silent,
                             boolean ipScope) {

        String kickReason = resolveReason(reason);

        applyKickToSystem(target, sender, kickReason, serverOrigin, silent, ipScope);

        broadcastKick(target.name(), sender.name(), kickReason, silent);
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-reason");
    }

    private void applyKickToSystem(PlayerInfo target,
                                   PlayerInfo sender,
                                   String kickReason,
                                   String serverOrigin,
                                   boolean silent,
                                   boolean ipScope) {

        platformHandler.applyKick(target.name(), target.uuid(), sender.name(), kickReason);

        databaseUtils.getKicksManager().insertKick(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                kickReason, serverOrigin,
                silent, ipScope
        );

        eventDispatcher.kickAddedEvent(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                kickReason, serverOrigin,
                silent, ipScope
        );
    }

    private void broadcastKick(String targetName, String senderName, String reason, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.kick.silent-broadcast-message"
                                : "punishments.kick.broadcast-message"
                )
                .replace("%target%", targetName)
                .replace("%reason%", reason)
                .replace("%moderator%", senderName);

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