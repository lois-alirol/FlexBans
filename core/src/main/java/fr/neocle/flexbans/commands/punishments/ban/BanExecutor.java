package fr.neocle.flexbans.commands.punishments.ban;

import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class BanExecutor {
    private final BanPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;

    public BanExecutor(BanPlatformHandler platformHandler, Broadcaster broadcaster, UsernameUUIDConverters usernameUUIDConverters, DatabaseUtils databaseUtils) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.floodgateApi = isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    private boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void executeBan(String target, String sender, String duration, String reason, String serverScope, String serverOrigin, boolean silent, boolean ipScope, Consumer<String> messageSender) {
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

        String senderName = sender != null && !sender.isEmpty() ? sender : "Console";
        UUID senderUUID = Common.parseUUID(usernameUUIDConverters.usernameToUUID(senderName));
        if (senderUUID == null) {
            messageSender.accept("§cError: Could not retrieve player's UUID.");
            return;
        }

        processBan(target, targetUUID, senderName, senderUUID, duration, reason, serverScope, serverOrigin, silent, ipScope);
    }

    private void processBan(String target, UUID targetUUID, String senderName, UUID senderUUID, String duration, String reason, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {
        String banReason = reason != null && !reason.isEmpty() ? reason : LanguageManager.getMessageString("punishments.default-reason");
        long banDuration = Common.parseDuration(duration);

        platformHandler.applyBan(target, targetUUID, senderName, duration, banReason, serverScope);
        databaseUtils.getBansManager().insertBan(targetUUID, target, senderUUID, senderName, banReason, banDuration, serverScope, serverOrigin, silent, ipScope);

        broadcaster.execute("§l§aBanning " + target + " for " + (banDuration == -1 ? "permanently" : banDuration + "ms") + " Reason: " + banReason);
    }
}
