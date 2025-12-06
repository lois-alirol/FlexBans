package fr.neocle.flexbans.velocity.command.adapter.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;

public class VelocityCommandInvocation implements ICommandInvocation {
    private final SimpleCommand.Invocation invocation;
    private final ICommandSource source;

    public VelocityCommandInvocation(SimpleCommand. Invocation invocation, CommandSource velocitySource) {
        this. invocation = invocation;
        this.source = new VelocityCommandSource(velocitySource);
    }

    @Override
    public String getLabel() {
        return invocation.alias();
    }

    @Override
    public String[] getArguments() {
        return invocation.arguments();
    }

    @Override
    public ICommandSource getSource() {
        return source;
    }
}