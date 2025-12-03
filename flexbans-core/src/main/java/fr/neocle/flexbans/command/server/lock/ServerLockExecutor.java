package fr.neocle.flexbans.command.server.lock;

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

public class ServerLockExecutor implements fr.neocle.flexbans.api.server.ServerLockExecutor {
    private final ServerLockPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private static final String SILENT_PERMISSION = "flexbans.serverlock.silent";

    public ServerLockExecutor(ServerLockPlatformHandler platformHandler,
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

    @Override
    public void lockServer(String serverName,
                           String sender,
                           String duration,
                           String reason,
                           String serverOrigin,
                           boolean silent,
                           Consumer<String> messageSender) {

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi, usernameUUIDConverters);
        if (senderInfo.isEmpty()) return;

        PlayerInfo senderPlayer = senderInfo.get();

        handleExistingLock(serverName, messageSender);
        processServerLock(serverName, senderPlayer, duration, reason, serverOrigin, silent);
    }

    private void handleExistingLock(String serverName, Consumer<String> messageSender) {
        if (databaseUtils.getServerLocksManager().isServerLocked(serverName)) {
            messageSender.accept("This server is already locked. Overriding");
        }
    }

    private void processServerLock(String serverName,
                                   PlayerInfo sender,
                                   String duration,
                                   String reason,
                                   String serverOrigin,
                                   boolean silent) {

        String lockReason = resolveReason(reason);
        long lockDuration = Common.parseDuration(duration);
        String displayDuration = resolveDuration(duration);

        applyLockToSystem(serverName, sender, lockReason, lockDuration, displayDuration, serverOrigin, silent);
        broadcastLock(serverName, sender.name(), lockReason, displayDuration, silent);
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

    private void applyLockToSystem(String serverName,
                                   PlayerInfo sender,
                                   String reason,
                                   long duration,
                                   String displayDuration,
                                   String serverOrigin,
                                   boolean silent) {

        platformHandler.applyLock(serverName, reason, sender.name(), System.currentTimeMillis(), displayDuration);

        databaseUtils.getServerLocksManager().insertServerLock(
                serverName, reason, duration,
                sender.uuid(), sender.name(),
                serverOrigin, silent
        );

        /*eventDispatcher.serverLockAddedEvent(
                serverName, reason, duration,
                sender.uuid(), sender.name(),
                serverOrigin, silent
        );*/
    }

    private void broadcastLock(String serverName, String senderName, String reason, String duration, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "server-locks.silent-broadcast-message"
                                : "server-locks.broadcast-message"
                )
                .replace("%server%", serverName)
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
