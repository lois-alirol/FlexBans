package fr.neocle.flexbans.bukkit.commands.SubCommands.PlayersWhitelistSubArgs;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.PlayersWhitelistCommand;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AddPlayer implements CommandExecutor {
    private final PlayersWhitelistCommand whitelistMethods;

    public AddPlayer(FlexBansAPI api, AuthenticationHandler authenticationHandler) {
        this.whitelistMethods = new PlayersWhitelistCommand(api, authenticationHandler) {
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
        if (!sender.hasPermission("flexbans.players.add")) {
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
