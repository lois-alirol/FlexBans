package fr.neocle.flexbans.commands.punishments.unban;

import fr.neocle.flexbans.commands.punishments.Common;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.Broadcast.Broadcaster;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.Consumer;

public class UnbanExecutor {
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DatabaseUtils databaseUtils;

    public UnbanExecutor(Broadcaster broadcaster, UsernameUUIDConverters usernameUUIDConverters, DatabaseUtils databaseUtils) {
        this.broadcaster = broadcaster;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.databaseUtils = databaseUtils;
        this.floodgateApi = Common.isFloodgateLoaded() ? FloodgateApi.getInstance() : null;
    }

    public void executeUnban(String target, String remover, String reason, boolean silent, Consumer<String> messageSender) {
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

        String removerName = remover != null && !remover.isEmpty() ? remover : "Console";
        UUID removerUUID = Common.parseUUID(usernameUUIDConverters.usernameToUUID(removerName));
        if (removerUUID == null) {
            messageSender.accept("§cError: Could not retrieve player's UUID.");
            return;
        }

        if (databaseUtils.getBansManager().isPlayerBanned(targetUUID)) {
            processUnban(target, targetUUID, removerName, removerUUID, reason, silent);
        } else {
            messageSender.accept("§cThis player is not banned");
        }
    }

    private void processUnban(String target, UUID targetUUID, String removerName, UUID removerUUID, String reason, boolean silent) {
        String unbanReason = reason != null && !reason.isEmpty() ? reason : LanguageManager.getMessageString("punishments.default-unban-reason");

        databaseUtils.getBansManager().removeBan(targetUUID, removerUUID, removerName, unbanReason);
        if (!silent) {
            broadcaster.execute("§l§aUnbanning " + target + ". Reason: " + unbanReason);
        }
    }
}
