package fr.neocle.flexbans.commands.servers.lock;

import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class ServerLockExecutor {
    private final ServerLockPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;

    public ServerLockExecutor(ServerLockPlatformHandler platformHandler, Broadcaster broadcaster, UsernameUUIDConverters usernameUUIDConverters, DatabaseUtils databaseUtils) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void lockServer(String serverName, String sender, String duration, String reason, String serverOrigin, boolean silent, Consumer<String> messageSender) {
        String senderName = sender != null && !sender.isEmpty() ? sender : "Console";
        UUID senderUUID = Common.parseUUID(usernameUUIDConverters.usernameToUUID(senderName));
        if (senderUUID == null) {
            messageSender.accept("§cError: Could not retrieve player's UUID.");
            return;
        }

        long lockDuration = Common.parseDuration(duration);

        if (duration == null || duration.isEmpty()) {
            duration = LanguageManager.getMessageString("punishments.infinite-duration");
        }

        if (databaseUtils.getServerLocksManager().isServerLocked(serverName)) {
            messageSender.accept("§cError: This server is already locked.");
            return;
        }

        databaseUtils.getServerLocksManager().insertServerLock(serverName, reason, lockDuration, senderUUID, senderName, serverOrigin, silent);
        platformHandler.applyLock(serverName, reason, senderName, System.currentTimeMillis(), duration);
        if (!silent) {
            broadcaster.execute("Server " + serverName + " was locked by " + senderName + " for " + reason + ".");
        }
    }
}
