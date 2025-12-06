package fr.neocle.flexbans.common.command.subcommand;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class VerifyCommand implements ICommandExecutor {
    private final UserManager userManager;

    public VerifyCommand(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();

        if (!source.isPlayer()) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-only"));
            return;
        }

        if (!source.hasPermission("flexbans.verify")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands. no-permission"));
            return;
        }

        String[] args = invocation.getArguments();
        if (args.length != 2 || !args[0].equalsIgnoreCase("verify")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.usage"));
            return;
        }

        String code = args[1];
        String username = source.getPlayerName();

        if (userManager.isUserRegistered(username) && userManager.isPlayerVerified(username)) {
            boolean success = userManager.setDiscordIdFromCode(username, code);
            if (success) {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
            }
            return;
        }

        try {
            userManager.insertUsername(username, code);
            userManager.setVerifiedStatusForPlayerName(username, true);
            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                Component message = LanguageManager.getMessageComponent("commands.verify.unknown-code")
                        .replaceText(builder -> builder.matchLiteral("%code%"). replacement(Component.text(code)));
                source.sendMessage(message);
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                FlexLogger.warn(LanguageManager.getMessageString("commands.logging.verify.sql-exception"). replace("%error%", e.getMessage()));
            }
        } catch (Exception e) {
            source.sendMessage(LanguageManager.getMessageComponent("commands. verify.error"));
            FlexLogger.warn(LanguageManager.getMessageString("commands.logging.verify. exception").replace("%error%", e.getMessage()));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }
}