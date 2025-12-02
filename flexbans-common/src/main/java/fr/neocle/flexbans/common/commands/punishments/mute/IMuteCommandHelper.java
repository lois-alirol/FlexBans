package fr.neocle.flexbans.common.commands.punishments.mute;

public interface IMuteCommandHelper {
    void sendDialogMessage(String playerName, String dialogType);
    boolean isServerRegistered(String serverName);
}
