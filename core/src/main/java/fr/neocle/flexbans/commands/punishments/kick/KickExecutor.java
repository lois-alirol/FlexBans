package fr.neocle.flexbans.commands.punishments.kick;

import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class KickExecutor {
    private final KickPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;

    public KickExecutor(KickPlatformHandler platformHandler, Broadcaster broadcaster, UsernameUUIDConverters usernameUUIDConverters, DatabaseUtils databaseUtils) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeKick(String target, String sender, String reason, String serverOrigin, boolean silent, boolean ipScope, Consumer<String> messageSender) {
        UUID targetUUID = Common.resolveTargetUUID(target, floodgateApi, usernameUUIDConverters);
        if (targetUUID == null) {
            messageSender.accept("§cError: Player '" + target + "' does not exist.");
            return;
        }

        target = usernameUUIDConverters.UUIDtoUsername(targetUUID.toString());
        if (target == null || target.contains("Error")) {
            messageSender.accept("§cError: Could not retrieve username.");
            return;
        }

        if (!platformHandler.isPlayerOnline(targetUUID)) {
            messageSender.accept("§cError: Player '" + target + "' is not online.");
            return;
        }

        String senderName = sender != null && !sender.isEmpty() ? sender : "Console";
        UUID senderUUID = Common.parseUUID(usernameUUIDConverters.usernameToUUID(senderName));
        if (senderUUID == null) {
            messageSender.accept("§cError: Could not retrieve player's UUID.");
            return;
        }

        processKick(target, targetUUID, senderName, senderUUID, reason, serverOrigin, silent, ipScope);
    }

    private void processKick(String target, UUID targetUUID, String senderName, UUID senderUUID, String reason, String serverOrigin, boolean silent, boolean ipScope) {
        String kickReason = reason != null && !reason.isEmpty() ? reason : LanguageManager.getMessageString("punishments.default-reason");

        platformHandler.applyKick(target, targetUUID, senderName, kickReason);
        databaseUtils.getKicksManager().insertKick(targetUUID, target, senderUUID, senderName, kickReason, serverOrigin, silent, ipScope);
        if (!silent) {
            broadcaster.execute("§l§aKicking " + target + " - Reason: " + kickReason);
        }
    }
}