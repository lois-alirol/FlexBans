package fr.neocle.flexbans.common.command.subcommand;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

        // Optional permission check
        // if (!source.hasPermission("flexbans.verify")) {
        //     source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
        //     return;
        // }

        String[] args = invocation.getArguments();
        if (args.length != 2 || !args[0].equalsIgnoreCase("verify")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.usage"));
            return;
        }

        String code = args[1];
        String username = source.getPlayerName();
        UUID uuid = source.getPlayerUUID();

        try {
            boolean isRegistered = userManager.isUserRegistered(username);
            boolean isVerified = userManager.isUserVerified(username);

            if (isRegistered && isVerified) {
                // Already verified → just link Discord
                if (userManager.setDiscordId(username, code)) {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
                } else {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                }
                return;
            }

            if (!isRegistered) {
                // Create user for verification
                if (!userManager.createUserForVerification(username, uuid)) {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                    return;
                }
            }

            // Verify the user
            if (!userManager.verifyUser(username, uuid)) {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return;
            }

            // Link Discord ID
            if (!userManager.setDiscordId(username, code)) {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return;
            }

            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));

        } catch (Exception e) {
            Component message = LanguageManager.getMessageComponent("commands.verify.error");
            source.sendMessage(message);
            FlexLogger.warn(LanguageManager.getMessageString("commands.logging.verify.exception")
                    .replace("%error%", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }
}
