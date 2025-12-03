package fr.neocle.flexbans.common.adapter.command;

import java.util.Collections;
import java.util.List;

public interface ICommandExecutor {
    void execute(ICommandInvocation invocation);

    default List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }

    default boolean hasPermission(ICommandInvocation invocation) {
        return true;
    }
}
