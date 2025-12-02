package fr.neocle.flexbans.common.commands.server.unlock;

import fr.neocle.flexbans.api.server.ServerUnlockExecutor;
import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr. neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ServerUnlockCommandExecutor implements ICommandExecutor {
    private final ServerUnlockExecutor serverUnlockExecutor;
    private final IServerUnlockCommandHelper helper;

    public ServerUnlockCommandExecutor(ServerUnlockExecutor serverUnlockExecutor, IServerUnlockCommandHelper helper) {
        this.serverUnlockExecutor = serverUnlockExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        String serverName = null;
        String sender = null;
        boolean silent = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.startsWith("-sender=")) {
                sender = arg.substring(8);
            } else if (serverName == null) {
                serverName = arg;
            }
        }

        if (serverName == null || serverName.isEmpty()) {
            serverName = helper.getDefaultServerName(source);
        }

        if (!serverName.equalsIgnoreCase("Global") && ! helper.isServerRegistered(serverName)) {
            source.sendMessage(Component.text("§cError: The specified server '" + serverName + "' is not registered. "));
            return;
        }

        if (sender == null || sender.isEmpty()) {
            sender = source. getName();
        }

        serverUnlockExecutor.unlockServer(serverName, sender, silent,
                message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            String currentArg = args. length > 0 ? args[args.length - 1]. toLowerCase() : "";
            return filterSuggestions(helper.getRegisteredServers(), currentArg);
        }

        String currentArg = args[args.length - 1].toLowerCase();

        if (currentArg.startsWith("-sender=")) {
            String prefix = "-sender=";
            return helper.getOnlinePlayerNames(). stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .toList();
        }

        if (currentArg.startsWith("-")) {
            List<String> flags = new ArrayList<>();
            flags.add("-s");
            flags.add("-sender=");
            return filterSuggestions(flags, currentArg);
        }

        List<String> allSuggestions = new ArrayList<>();
        allSuggestions.add("-s");
        allSuggestions.add("-sender=");
        return filterSuggestions(allSuggestions, currentArg);
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .toList();
    }
}