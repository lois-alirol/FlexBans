package fr.neocle.flexbans.command.server;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.server.ServerUnlockExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.command.Common.PlayerInfo;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Optional;
import java.util.function.Consumer;

public class ServerUnlockExecutorImpl implements ServerUnlockExecutor {
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UuidUsernameResolver resolver;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private static final String SILENT_PERMISSION = "flexbans.serverunlock.silent";

    public ServerUnlockExecutorImpl(Broadcaster broadcaster,
                                    DatabaseUtils databaseUtils,
                                    EventDispatcher eventDispatcher) {
        this.broadcaster = broadcaster;
        this.databaseUtils = databaseUtils;
        this.eventDispatcher = eventDispatcher;

        this.resolver = UuidUsernameResolver.get();
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    @Override
    public void unlockServer(String serverName,
                             String sender,
                             boolean silent,
                             Consumer<String> messageSender) {

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi);
        if (senderInfo.isEmpty()) return;

        PlayerInfo senderPlayer = senderInfo.get();

        handleNoLock(serverName, messageSender);
        processServerUnlock(serverName, senderPlayer, silent);
    }

    private void handleNoLock(String serverName, Consumer<String> messageSender) {
        if (!databaseUtils.getServerLocksManager().isServerLocked(serverName)) {
            messageSender.accept("§cError: This server is not locked.");
        }
    }

    private void processServerUnlock(String serverName,
                                     PlayerInfo sender,
                                     boolean silent) {

        applyUnlockToSystem(serverName, sender, silent);
        broadcastUnlock(serverName, sender.name(), silent);
    }

    private void applyUnlockToSystem(String serverName,
                                     PlayerInfo sender,
                                     boolean silent) {

        databaseUtils.getServerLocksManager().unlockServer(
                serverName,
                sender.uuid(),
                sender.name(),
                ""
        );

        /*eventDispatcher.serverUnlockAddedEvent(
                serverName,
                sender.uuid(),
                sender.name(),
                silent
        );*/
    }

    private void broadcastUnlock(String serverName, String senderName, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "server-unlocks.silent-broadcast-message"
                                : "server-unlocks.broadcast-message"
                )
                .replace("%server%", serverName)
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