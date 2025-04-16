package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.Punishments.BansManager;
import fr.neocle.flexbans.database.Punishments.HistoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AltCommand implements SimpleCommand {
    private final ProxyServer server;
    private final HistoryManager historyManager;
    private final BansManager bansManager;

    public AltCommand(ProxyServer server, DatabaseUtils databaseUtils) {
        this.server = server;
        this.historyManager = databaseUtils.getHistoryManager();
        this.bansManager = databaseUtils.getBansManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("flexbans.command.ipconnections")) {
            source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /ipconnections <player>").color(NamedTextColor.RED));
            return;
        }

        String playerName = args[0];
        Optional<Player> optPlayer = server.getPlayer(playerName);

        if (optPlayer.isEmpty()) {
            String ip = historyManager.getPlayerIP(playerName);
            if (ip == null) {
                source.sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED));
                return;
            }

            displayPlayersWithSameIP(source, ip);
        } else {
            Player player = optPlayer.get();
            String ip = player.getRemoteAddress().getAddress().getHostAddress();

            displayPlayersWithSameIP(source, ip);
        }
    }

    private void displayPlayersWithSameIP(CommandSource source, String ip) {
        source.sendMessage(Component.text("Players connected from IP: " + ip).color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true));

        List<String[]> playersWithSameIP = historyManager.getPlayersWithSameIP(ip);

        if (playersWithSameIP.isEmpty()) {
            source.sendMessage(Component.text("No players found with this IP.").color(NamedTextColor.RED));
            return;
        }

        String origin = (source instanceof Player)
                ? ((Player) source).getCurrentServer()
                .map(server -> server.getServer().getServerInfo().getName())
                .orElse("Proxy")
                : "Proxy";

        for (String[] playerInfo : playersWithSameIP) {
            String playerUuid = playerInfo[0];
            String playerName = playerInfo[1];
            String playerIp = playerInfo[2];

            NamedTextColor color;
            String status;

            if (bansManager.isIpBanned(playerIp, origin)) {
                color = NamedTextColor.RED;
                status = "[IP BANNED]";
            } else if (bansManager.isPlayerBanned(UUID.fromString(playerUuid), origin)) {
                color = NamedTextColor.GOLD;
                status = "[BANNED]";
            } else if (server.getPlayer(UUID.fromString(playerUuid)).isPresent()) {
                color = NamedTextColor.GREEN;
                status = "[ONLINE]";
            } else {
                color = NamedTextColor.GRAY;
                status = "[OFFLINE]";
            }

            source.sendMessage(Component.text(status + " " + playerName).color(color));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            for (Player player : server.getAllPlayers()) {
                if (player.getUsername().toLowerCase().startsWith(partialName)) {
                    suggestions.add(player.getUsername());
                }
            }

            return suggestions;
        }

        return List.of();
    }
}