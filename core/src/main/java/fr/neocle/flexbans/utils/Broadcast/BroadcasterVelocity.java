package fr.neocle.flexbans.utils.Broadcast;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public class BroadcasterVelocity implements Broadcaster {
    private final ProxyServer server;

    public BroadcasterVelocity(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(String message) {
        Component formattedMessage = Component.text(message);

        for (Player player : server.getAllPlayers()) {
            player.sendMessage(formattedMessage);
        }

        CommandSource console = server.getConsoleCommandSource();
        console.sendMessage(formattedMessage);
    }
}
