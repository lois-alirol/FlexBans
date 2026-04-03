package fr.neocle.flexbans.common.messaging;

public final class Channel {

    private Channel() {}

    private static final String NAMESPACE = "flexbans:";

    public static final String DIALOGS = NAMESPACE + "dialogs";
    public static final String BAN = NAMESPACE + "ban";
}
