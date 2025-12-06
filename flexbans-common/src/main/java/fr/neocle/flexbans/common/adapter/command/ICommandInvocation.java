package fr.neocle.flexbans.common.adapter.command;

public interface ICommandInvocation {
    String getLabel();
    String[] getArguments();
    ICommandSource getSource();
}
