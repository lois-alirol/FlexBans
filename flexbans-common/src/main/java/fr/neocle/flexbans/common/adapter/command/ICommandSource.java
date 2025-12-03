package fr.neocle.flexbans.common.adapter.command;

import net.kyori.adventure.text.Component;

public interface ICommandSource {
    String getOrigin();
    void sendMessage(Component message);
    boolean hasPermission(String permission);
    String getName();
    boolean isPlayer();
    String getPlayerName();
}