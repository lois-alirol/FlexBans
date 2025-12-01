package fr.neocle.flexbans.api.events.velocity.punishments;

import java.util.UUID;

public class KickAddedEvent extends PunishmentAddedEvent {

    public KickAddedEvent(
            UUID targetUUID,
            String targetName,
            UUID senderUUID,
            String senderName,
            String kickReason,
            String serverOrigin,
            boolean silent,
            boolean ipScope
    ) {
        super(
                PunishmentType.KICK,
                targetUUID,
                targetName,
                senderUUID,
                senderName,
                kickReason,
                serverOrigin,
                silent,
                ipScope
        );
    }
}
