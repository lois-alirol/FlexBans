package fr.neocle.litebansweb.bukkit.commands.SubCommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.DiscordWhitelistSubArgs.AddUser;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.DiscordWhitelistSubArgs.RemoveUser;
import fr.neocle.litebansweb.locale.LanguageManager;

public class DiscordWhitelist implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new HashMap<>();

    public DiscordWhitelist(LitebansWebAPI api, Path dataFolder, Logger logger) {
        subCommands.put("add", new AddUser(api, dataFolder, logger));
        subCommands.put("remove", new RemoveUser(api, dataFolder, logger));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        CommandExecutor executor = subCommands.get(subCommand);

        if (executor != null) {
            return executor.onCommand(sender, command, label, args);
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.usage"));
            return true;
        }
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
            CommandExecutor subExecutor = subCommands.get(args[0].toLowerCase());
            if (subExecutor instanceof TabCompleter) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                List<String> subSuggestions = ((TabCompleter) subExecutor).onTabComplete(sender, command, alias, subArgs);
                if (subSuggestions != null) {
                    suggestions.addAll(subSuggestions);
                }
            }
        }

        return suggestions;
    }
}
