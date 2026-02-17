package fr.neocle.flexbans.velocity.command.adapter.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CommandSource;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;

import java.util.List;

public class VelocityCommandInvocation implements ICommandInvocation {
    private final String label;
    private final String[] arguments;
    private final ICommandSource source;

    public VelocityCommandInvocation(CommandContext<CommandSource> ctx) {
        this.source = new VelocityCommandSource(ctx.getSource());

        List<CommandNode<CommandSource>> nodes = ctx.getNodes()
                .stream()
                .map(ParsedCommandNode::getNode)
                .toList();

        this.label = nodes.isEmpty() ? "" : nodes.getFirst().getName();

        String input = ctx.getInput().trim();
        String withoutLabel = input.startsWith(label)
                ? input.substring(label.length()).trim()
                : input;

        this.arguments = withoutLabel.isEmpty() ? new String[0] : withoutLabel.split("\\s+");
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

    @Override
    public ICommandSource getSource() {
        return source;
    }
}