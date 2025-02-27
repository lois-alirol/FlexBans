package fr.neocle.litebansweb.bungee.commands.SubCommands.PlayersWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.PlayersWhitelistCommand;
import fr.neocle.litebansweb.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.nio.file.Path;
import java.util.logging.Logger;

public class AddPlayer extends Command {
    private final PlayersWhitelistCommand whitelistMethods;

    public AddPlayer(LitebansWebAPI api, Path dataFolder, Logger logger) {
        super("add");
        this.whitelistMethods = new PlayersWhitelistCommand(api, dataFolder.resolve("config.yml"), logger) {
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
        if (!sender.hasPermission("litebansweb.players.add")) {
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
