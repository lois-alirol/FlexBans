package fr.neocle.flexbans.common.adapter.command;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public interface ICommandSource {
    String getOrigin();
    void sendMessage(Component message);
    void sendPlainMessage(String message);
    boolean hasPermission(String permission);
    String getName();
    boolean isPlayer();
    String getPlayerName();
    UUID getPlayerUUID();
}