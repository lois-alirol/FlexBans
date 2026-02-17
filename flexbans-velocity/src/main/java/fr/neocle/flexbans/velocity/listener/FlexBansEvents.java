package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.flexbans.api.event.velocity.punishment.BanAddedEvent;
import fr.neocle.flexbans.api.event.velocity.punishment.KickAddedEvent;
import fr.neocle.flexbans.api.event.velocity.punishment.MuteAddedEvent;
import fr.neocle.flexbans.api.event.velocity.punishment.PunishmentAddedEvent;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.web.cache.PunishmentCache;
import fr.neocle.flexbans.web.provider.PunishmentDataProvider;

public class FlexBansEvents {
    private final DatabaseUtils databaseUtils;
    private final PunishmentDataProvider punishmentDataProvider;

    public FlexBansEvents(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
        this.punishmentDataProvider = PunishmentDataProvider.getInstance(databaseUtils);
    }

    @Subscribe
    public void onAnyPunishment(PunishmentAddedEvent event) {
    }

    @Subscribe
    public void onBan(BanAddedEvent event) {
        PunishmentCache.addPunishment(
                event.getTargetName(),
                event.getSenderName(),
                event.getReason(),
                event.getDuration(),
                event.getServerScope(),
                "BAN"
        );
    }

    @Subscribe
    public void onMute(MuteAddedEvent event) {
        PunishmentCache.addPunishment(
                event.getTargetName(),
                event.getSenderName(),
                event.getReason(),
                event.getDuration(),
                event.getServerScope(),
                "MUTE"
        );
    }

    @Subscribe
    public void onKick(KickAddedEvent event) {
        PunishmentCache.addPunishment(
                event.getTargetName(),
                event.getSenderName(),
                event.getReason(),
                0,
                "",
                "KICK"
        );
    }
}
