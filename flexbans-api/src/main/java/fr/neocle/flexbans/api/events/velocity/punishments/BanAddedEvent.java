package fr.neocle.flexbans.api.events.velocity.punishments;

import java.util.UUID;

public class BanAddedEvent extends PunishmentAddedEvent {

    private final long duration;
    private final String serverScope;

    public BanAddedEvent(
            UUID targetUUID,
            String targetName,
            UUID senderUUID,
            String senderName,
            String banReason,
            long banDuration,
            String serverScope,
            String serverOrigin,
            boolean silent,
            boolean ipScope
    ) {
        super(
                PunishmentType.BAN,
                targetUUID,
                targetName,
                senderUUID,
                senderName,
                banReason,
                serverOrigin,
                silent,
                ipScope
        );
        this.duration = banDuration;
        this.serverScope = serverScope;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isPermanent() {
        return duration <= 0;
    }

    public String getServerScope() {
        return serverScope;
    }
}