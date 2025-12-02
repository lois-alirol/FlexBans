package fr.neocle.flexbans.common.commands.subcommands;

import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr.neocle.flexbans.locale.LanguageManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayersWhitelistCommand implements ICommandExecutor {
    private final Map<String, ICommandExecutor> subCommands = new HashMap<>();

    public PlayersWhitelistCommand() {
    }

    protected void registerSubCommand(String name, ICommandExecutor command) {
        subCommands.put(name.toLowerCase(), command);
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();
        ICommandSource source = invocation.getSource();

        if (args.length == 1) {
            source. sendMessage(LanguageManager. getMessageComponent("commands.players-whitelist.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();
        ICommandExecutor command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.usage"));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();

        if (args. length == 2) {
            return subCommands.keySet(). stream().toList();
        }

        if (args.length == 3) {
            String input = args[2].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }

        return Collections.emptyList();
    }
}