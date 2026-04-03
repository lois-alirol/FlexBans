package fr.neocle.flexbans.common.messaging;

public final class Channel {

    private Channel() {}

    private static final String NAMESPACE = "flexbans:";

    public static final String DIALOGS = NAMESPACE + "dialogs";
    public static final String BAN = NAMESPACE + "ban";
    public static final String MUTE = NAMESPACE + "mute";
    public static final String KICK = NAMESPACE + "kick";
    public static final String WARNING = NAMESPACE + "warning";

    public static final String MUTED = NAMESPACE + "muted";
    public static final String MUTED_QUERY = NAMESPACE + "muted_query";
    public static final String MUTED_RESPONSE = NAMESPACE + "muted_response";

}
