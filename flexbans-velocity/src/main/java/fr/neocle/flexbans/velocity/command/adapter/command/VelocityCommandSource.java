package fr.neocle.flexbans.velocity.command.adapter.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class VelocityCommandSource implements ICommandSource {
    private final CommandSource source;
    private final String origin;

    public VelocityCommandSource(CommandSource source) {
        this.source = source;
        this.origin = (source instanceof Player player)
                ? player.getCurrentServer()
                .map(server -> server.getServer().getServerInfo().getName())
                .orElse("Proxy")
                : "Proxy";
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public void sendMessage(Component message) {
        source.sendMessage(message);
    }

    @Override
    public void sendPlainMessage(String message) {
        source.sendPlainMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public String getName() {
        return isPlayer() ? ((Player) source). getUsername() : "Console";
    }

    @Override
    public boolean isPlayer() {
        return source instanceof Player;
    }

    @Override
    public String getPlayerName() {
        return isPlayer() ? ((Player) source).getUsername() : null;
    }

    @Override
    public UUID getPlayerUUID() {
        return isPlayer() ? ((Player) source).getUniqueId() : null;
    }
}