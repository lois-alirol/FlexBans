package fr.neocle.flexbans.api.events.velocity.punishments;

import java.util.UUID;

public abstract class PunishmentAddedEvent {
    private final UUID targetUUID;
    private final String targetName;
    private final UUID senderUUID;
    private final String senderName;
    private final String reason;
    private final String serverOrigin;
    private final boolean silent;
    private final boolean ipScope;
    private final PunishmentType type;

    protected PunishmentAddedEvent(
            PunishmentType type,
            UUID targetUUID,
            String targetName,
            UUID senderUUID,
            String senderName,
            String reason,
            String serverOrigin,
            boolean silent,
            boolean ipScope
    ) {
        this.type = type;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.reason = reason;
        this.serverOrigin = serverOrigin;
        this.silent = silent;
        this.ipScope = ipScope;
    }

    public PunishmentType getType() {
        return type;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReason() {
        return reason;
    }

    public String getServerOrigin() {
        return serverOrigin;
    }

    public boolean isSilent() {
        return silent;
    }

    public boolean isIpScope() {
        return ipScope;
    }

}
