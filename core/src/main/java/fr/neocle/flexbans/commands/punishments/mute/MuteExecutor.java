package fr.neocle.flexbans.commands.punishments.mute;

import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class MuteExecutor implements fr.neocle.flexbans.api.punishments.MuteExecutor {
    private final MutePlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;

    public MuteExecutor(MutePlatformHandler platformHandler, Broadcaster broadcaster, UsernameUUIDConverters usernameUUIDConverters, DatabaseUtils databaseUtils) {
        this.platformHandler = platformHandler;
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeMute(String target, String sender, String duration, String reason, String serverScope, String serverOrigin, boolean silent, boolean ipScope, Consumer<String> messageSender) {
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

        processMute(target, targetUUID, senderName, senderUUID, duration, reason, serverScope, serverOrigin, silent, ipScope);
    }

    private void processMute(String target, UUID targetUUID, String senderName, UUID senderUUID, String duration, String reason, String serverScope, String serverOrigin, boolean silent, boolean ipScope) {
        String muteReason = reason != null && !reason.isEmpty() ? reason : LanguageManager.getMessageString("punishments.default-reason");
        long muteDuration = Common.parseDuration(duration);

        if (duration == null || duration.isEmpty()) {
            duration = LanguageManager.getMessageString("punishments.infinite-duration");
        }

        platformHandler.applyMute(target, targetUUID, senderName, duration, muteReason, serverScope);
        databaseUtils.getMutesManager().insertMute(targetUUID, target, senderUUID, senderName, muteReason, muteDuration, serverScope, serverOrigin, silent, ipScope);

        if (!silent) {
            broadcaster.execute("§l§aMuting " + target + " for " + (muteDuration == -1 ? "permanently" : muteDuration + "ms") + " Reason: " + muteReason);
        }
    }
}