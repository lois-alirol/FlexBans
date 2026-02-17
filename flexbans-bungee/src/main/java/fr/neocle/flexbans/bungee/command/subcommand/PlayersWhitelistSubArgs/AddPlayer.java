package fr.neocle.flexbans.bungee.command.subcommand.PlayersWhitelistSubArgs;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.command.whitelist.PlayersWhitelistCommand;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class AddPlayer extends Command {
    private final PlayersWhitelistCommand whitelistMethods;

    public AddPlayer() {
        super("add");
        this.whitelistMethods = new PlayersWhitelistCommand() {
            @Override
            protected void sendMessage(Object sender, String message) {
                if (sender instanceof CommandSender commandSender) {
                    commandSender.sendMessage(LanguageManager.getBungeeMessageComponent(commandSender, message));
                }
            }
        };
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("flexbans.players.add")) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.players-whitelist.add-player.usage"));
            return;
        }

        String playerName = args[1];

        whitelistMethods.executeCommand(sender, playerName, true);
    }
}
