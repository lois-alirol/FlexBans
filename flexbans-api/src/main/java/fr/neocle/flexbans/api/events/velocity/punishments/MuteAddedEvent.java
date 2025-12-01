package fr.neocle.flexbans.api.events.velocity.punishments;

import java.util.UUID;

public class MuteAddedEvent extends PunishmentAddedEvent {

    private final long duration; // in ms
    private final String serverScope;

    public MuteAddedEvent(
            UUID targetUUID,
            String targetName,
            UUID senderUUID,
            String senderName,
            String muteReason,
            long muteDuration,
            String serverScope,
            String serverOrigin,
            boolean silent,
            boolean ipScope
    ) {
        super(
                PunishmentType.MUTE,
                targetUUID,
                targetName,
                senderUUID,
                senderName,
                muteReason,
                serverOrigin,
                silent,
                ipScope
        );
        this.duration = muteDuration;
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
