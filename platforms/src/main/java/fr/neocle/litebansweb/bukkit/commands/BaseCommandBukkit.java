package fr.neocle.litebansweb.bukkit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import fr.neocle.litebansweb.bukkit.commands.SubCommands.DiscordWhitelist;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.PlayersWhitelist;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.Reload;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.Verify;
import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.api.LitebansWebAPI;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BaseCommandBukkit implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new HashMap<>();

    public BaseCommandBukkit(LitebansWebAPI api, Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, DatabaseUtils databaseUtils, Logger logger) {
        subCommands.put("reload", new Reload(dataFolder, oauth2Handler, indexHandler, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
        subCommands.put("players", new PlayersWhitelist(api, dataFolder, logger));
        subCommands.put("discord", new DiscordWhitelist(api, dataFolder, logger));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        CommandExecutor subCommand = subCommands.get(subCommandName);

        if (subCommand != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            return subCommand.onCommand(sender, command, label, subArgs);
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands.keySet()) {
                if (subCommand.startsWith(input)) {
                    suggestions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            CommandExecutor subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand instanceof TabCompleter) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                return ((TabCompleter) subCommand).onTabComplete(sender, command, alias, subArgs);
            }
        }

        return suggestions;
    }
}
