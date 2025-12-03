package fr.neocle.flexbans.common.command;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractBaseCommand implements ICommandExecutor {
    protected final Map<String, ICommandExecutor> subCommands = new HashMap<>();

    protected void registerSubCommand(String name, ICommandExecutor command) {
        subCommands. put(name. toLowerCase(), command);
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();
        ICommandSource source = invocation.getSource();

        if (args.length == 0) {
            source.sendMessage(Component.text("§cUnknown command.  Use /help for available commands."));
            return;
        }

        String subCommand = args[0].toLowerCase();
        ICommandExecutor command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(Component.text("§cUnknown command. Use /help for available commands."));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();

        if (args.length == 0) {
            return new java.util.ArrayList<>(subCommands.keySet());
        }

        if (args.length == 1) {
            String input = args[0]. toLowerCase();
            return subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(input))
                    . toList();
        }

        String subCommand = args[0].toLowerCase();
        ICommandExecutor command = subCommands.get(subCommand);

        if (command != null) {
            return command.suggest(invocation);
        }

        return Collections.emptyList();
    }
}