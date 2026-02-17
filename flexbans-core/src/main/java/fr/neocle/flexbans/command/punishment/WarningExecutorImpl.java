package fr.neocle.flexbans.command.punishment;

import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.punishment.WarningExecutor;
import fr.neocle.flexbans.command.Common;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import org.geysermc.floodgate.api.FloodgateApi;
import fr.neocle.flexbans.command.Common.PlayerInfo;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WarningExecutorImpl implements WarningExecutor {
    private final fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler platformHandler;
    private final Broadcaster broadcaster;
    private final FloodgateApi floodgateApi;
    private final UuidUsernameResolver resolver;
    private final DatabaseUtils databaseUtils;
    private final EventDispatcher eventDispatcher;

    private final static String SILENT_PERMISSION = "flexbans.warning.silent";

    public WarningExecutorImpl(fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler platformHandler,
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

    public void executeWarning(String target,
                               String sender,
                               String reason,
                               String serverScope,
                               String serverOrigin,
                               boolean silent,
                               boolean ipScope,
                               Consumer<String> messageSender) {

        Optional<PlayerInfo> targetInfo = Common.resolveTarget(target, messageSender, floodgateApi);
        if (targetInfo.isEmpty()) {
            return;
        }

        Optional<PlayerInfo> senderInfo = Common.resolveSender(sender, messageSender, floodgateApi);
        if (senderInfo.isEmpty()) {
            return;
        }

        PlayerInfo targetPlayer = targetInfo.get();
        PlayerInfo senderPlayer = senderInfo.get();

        processWarning(targetPlayer, senderPlayer, reason, serverScope, serverOrigin, silent, ipScope, messageSender);
    }

    private void processWarning(PlayerInfo target,
                                PlayerInfo sender,
                                String reason,
                                String serverScope,
                                String serverOrigin,
                                boolean silent,
                                boolean ipScope,
                                Consumer<String> messageSender) {

        String warningReason = resolveReason(reason);

        int warningCount = databaseUtils.getPunishmentsManager().getActiveWarningCount(target.uuid(), serverScope) + 1;

        applyWarningToSystem(target, sender, warningReason, serverScope, serverOrigin, silent, ipScope, warningCount);
        broadcastWarning(target.name(), sender.name(), warningReason, warningCount + 1, silent);

        messageSender.accept("§aWarning issued successfully. " + target.name() + " now has " + (warningCount + 1) + " active warning(s).");
    }

    private String resolveReason(String reason) {
        return (reason != null && !reason.isEmpty())
                ? reason
                : LanguageManager.getMessageString("punishments.default-reason");
    }

    private void applyWarningToSystem(PlayerInfo target,
                                      PlayerInfo sender,
                                      String warningReason,
                                      String serverScope,
                                      String serverOrigin,
                                      boolean silent,
                                      boolean ipScope,
                                      int warningCount) {

        platformHandler.applyWarning(target.name(), target.uuid(), sender.name(), warningReason, serverScope);
        platformHandler.executeWarningAction(target.name(), target.uuid(), warningCount, sender.name(), serverScope, serverOrigin, silent);

        int expiresAfterDays = ConfigManager.getInt("punishments-system.built-in.warnings.expires-after-days");
        long duration = TimeUnit.DAYS.toMillis(expiresAfterDays);

        InetAddress targetIp = databaseUtils.getProfilesManager().getIp(target.name());

        databaseUtils.getPunishmentsManager().insertPunishment(
                PunishmentsManager.PunishmentType.WARNING,
                target.uuid(), targetIp,
                sender.uuid(), sender.name(),
                warningReason, duration,
                serverScope, serverOrigin,
                silent, ipScope
        );

        /*eventDispatcher.warningAddedEvent(
                target.uuid(), target.name(),
                sender.uuid(), sender.name(),
                warningReason,
                serverScope, serverOrigin,
                silent, ipScope
        );*/
    }

    private void broadcastWarning(String targetName, String senderName, String reason, int warningCount, boolean silent) {
        String rawMessage = LanguageManager.getMessageString(
                        silent
                                ? "punishments.warning.silent-broadcast-message"
                                : "punishments.warning.broadcast-message"
                )
                .replace("%target%", targetName)
                .replace("%reason%", reason)
                .replace("%moderator%", senderName)
                .replace("%count%", String.valueOf(warningCount));

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