package fr.neocle.flexbans.bungee.command.subcommand;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.bungee.command.subcommand.DiscordWhitelistSubArgs.AddUser;
import fr.neocle.flexbans.bungee.command.subcommand.DiscordWhitelistSubArgs.RemoveUser;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordWhitelist extends Command implements TabExecutor {
    private final Map<String, Command> subCommands = new HashMap<>();

    public DiscordWhitelist() {
        super("discordwhitelist");
        subCommands.put("add", new AddUser());
        subCommands.put("remove", new RemoveUser());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.discord-whitelist.usage"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        Command command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(sender, args);
        } else {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.discord-whitelist.usage"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
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
