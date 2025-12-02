package fr.neocle.flexbans.common.commands.punishments.kick;

import java.util.List;

public interface IKickCommandHelper {
    void sendDialogMessage(String playerName, String dialogType);
    List<String> getOnlinePlayerNames();
}