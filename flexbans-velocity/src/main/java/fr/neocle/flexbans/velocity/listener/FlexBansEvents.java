package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.flexbans.api.events.velocity.punishments.BanAddedEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.KickAddedEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.MuteAddedEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.PunishmentAddedEvent;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.api.PunishmentSSEHandler;
import fr.neocle.flexbans.handlers.cache.CountsCache;

public class FlexBansEvents {
    private final DatabaseUtils databaseUtils;
    private final PunishmentSSEHandler punishmentSSEHandler;

    public FlexBansEvents(DatabaseUtils databaseUtils, PunishmentSSEHandler punishmentSSEHandler) {
        this.databaseUtils = databaseUtils;
        this.punishmentSSEHandler = punishmentSSEHandler;

        CountsCache.update(databaseUtils, true, false);
    }

    @Subscribe
    public void onAnyPunishment(PunishmentAddedEvent event) {
        CountsCache.update(databaseUtils, true, false);

        String countsJson = String.format(
                "{\"type\":\"counts\",\"bans\":%d,\"mutes\":%d,\"kicks\":%d,\"warnings\":%d}",
                CountsCache.bansCount, CountsCache.mutesCount, CountsCache.kicksCount, CountsCache.warningsCount
        );
        punishmentSSEHandler.broadcastUpdate(countsJson);
    }

    @Subscribe
    public void onBan(BanAddedEvent event) {
        try {
            String jsonUpdate = String.format(
                    "{\"type\":\"ban\",\"player\":\"%s\",\"uuid\":\"%s\",\"moderator\":\"%s\",\"reason\":\"%s\",\"duration\":\"%s\",\"server\":\"%s\"}",
                    event.getTargetName(),
                    event.getTargetUUID(),
                    event.getSenderName(),
                    event.getReason().replace("\"", "'"),
                    (event.getDuration() == -1 ? "Permanent" : event.getDuration() + "ms"),
                    event.getServerScope()
            );
            punishmentSSEHandler.broadcastUpdate(jsonUpdate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onMute(MuteAddedEvent event) {
        try {
            String jsonUpdate = String.format(
                    "{\"type\":\"mute\",\"player\":\"%s\",\"uuid\":\"%s\",\"moderator\":\"%s\",\"reason\":\"%s\",\"duration\":\"%s\",\"server\":\"%s\"}",
                    event.getTargetName(),
                    event.getTargetUUID(),
                    event.getSenderName(),
                    event.getReason().replace("\"", "'"),
                    (event.getDuration() == -1 ? "Permanent" : event.getDuration() + "ms"),
                    event.getServerScope()
            );
            punishmentSSEHandler.broadcastUpdate(jsonUpdate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onKick(KickAddedEvent event) {
        try {
            String jsonUpdate = String.format(
                    "{\"type\":\"mute\",\"player\":\"%s\",\"uuid\":\"%s\",\"moderator\":\"%s\",\"reason\":\"%s\"}",
                    event.getTargetName(),
                    event.getTargetUUID(),
                    event.getSenderName(),
                    event.getReason().replace("\"", "'")
            );
            punishmentSSEHandler.broadcastUpdate(jsonUpdate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
