package fr.neocle.flexbans.common.adapter;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

public interface IPlayer {
    UUID getUniqueId();
    String getUsername();
    InetAddress getInetAddress();
    Optional<String> getCurrentServer();
    void disconnect(Object formattedMessage);
    void sendMessage(Object formattedMessage);
    boolean hasPermission(String permission);
}