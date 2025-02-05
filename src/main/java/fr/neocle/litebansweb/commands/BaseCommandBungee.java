package fr.neocle.litebansweb.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.chat.TextComponent;

import fr.neocle.litebansweb.commands.SubCommands.Bungee.Reload;
import fr.neocle.litebansweb.commands.SubCommands.Bungee.Verify;
import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BaseCommandBungee extends Command implements TabExecutor {
    private final Map<String, Command> subCommands = new HashMap<>();

    public BaseCommandBungee(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, DatabaseUtils databaseUtils, Logger logger) {
        super("litebansweb", "litebansweb.verify", "lw", "lbw");

        subCommands.put("reload", new Reload(dataFolder, oauth2Handler, indexHandler, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent("Available subcommands: reload, verify"));
            return;
        }
    
        String subCommand = args[0].toLowerCase();
        Command command = subCommands.get(subCommand);
    
        if (command != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            command.execute(sender, subArgs);
        } else {
            sender.sendMessage(new TextComponent("Unknown subcommand. Available: reload, verify"));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands.keySet()) {
                if (subCommand.startsWith(input)) {
                    suggestions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            Command subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand instanceof TabExecutor) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                Iterable<String> subSuggestions = ((TabExecutor) subCommand).onTabComplete(sender, subArgs);
                if (subSuggestions != null) {
                    subSuggestions.forEach(suggestions::add);
                }
            }
        }

        return suggestions;
    }

}
