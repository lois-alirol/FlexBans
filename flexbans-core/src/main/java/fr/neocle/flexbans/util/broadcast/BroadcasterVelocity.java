package fr.neocle.flexbans.util.broadcast;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.util.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class BroadcasterVelocity implements Broadcaster {
    private final ProxyServer server;
    private final MiniMessage miniMessage;

    public BroadcasterVelocity(ProxyServer server) {
        this.server = server;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(String message) {
        Component formattedMessage = miniMessage.deserialize(message);

        for (Player player : server.getAllPlayers()) {
            player.sendMessage(formattedMessage);
        }

        CommandSource console = server.getConsoleCommandSource();
        console.sendPlainMessage(ColorUtils.toAnsi(formattedMessage));
    }

    @Override
    public void execute(String message, String permission) {
        Component formattedMessage = miniMessage.deserialize(message);

        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(formattedMessage);
            }
        }

        CommandSource console = server.getConsoleCommandSource();
        console.sendPlainMessage(ColorUtils.toAnsi(formattedMessage));
    }
}
