package fr.neocle.flexbans.common.adapter;

import java.util.Optional;

public interface IPlatform {
    Optional<IPlayer> getPlayer(java.util.UUID uuid);
    void sendPluginMessage(IPlayer player, String channel, byte[] data);
}
