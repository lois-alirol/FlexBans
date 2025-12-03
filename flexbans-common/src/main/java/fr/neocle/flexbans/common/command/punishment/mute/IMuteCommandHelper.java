package fr.neocle.flexbans.common.command.punishment.mute;

public interface IMuteCommandHelper {
    void sendDialogMessage(String playerName, String dialogType);
    boolean isServerRegistered(String serverName);
}
