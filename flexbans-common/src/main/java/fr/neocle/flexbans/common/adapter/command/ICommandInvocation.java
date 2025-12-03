package fr.neocle.flexbans.common.adapter.command;

public interface ICommandInvocation {
    String[] getArguments();
    ICommandSource getSource();
}
