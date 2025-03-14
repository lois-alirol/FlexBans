package fr.neocle.litebansweb.bukkit.commands.SubCommands.PlayersWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.PlayersWhitelistCommand;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class AddPlayer implements CommandExecutor {
    private final PlayersWhitelistCommand whitelistMethods;

    public AddPlayer(LitebansWebAPI api, AuthenticationHandler authenticationHandler, Map<String, Object> config) {
        this.whitelistMethods = new PlayersWhitelistCommand(api, config, authenticationHandler) {
            @Override
            protected void sendMessage(Object sender, String message) {
                if (sender instanceof CommandSender commandSender) {
                    commandSender.sendMessage(LanguageManager.getMessageComponent(message));
                }
            }
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansweb.players.add")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.add-player.usage"));
            return true;
        }

        String playerName = args[1];
        whitelistMethods.executeCommand(sender, playerName, true);
        return true;
    }
}
