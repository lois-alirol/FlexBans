package fr.neocle.flexbans.common.commands;

public interface ICommandInvocation {
    String[] getArguments();
    ICommandSource getSource();
}
