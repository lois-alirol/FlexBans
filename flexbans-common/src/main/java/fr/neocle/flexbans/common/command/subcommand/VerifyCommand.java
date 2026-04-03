package fr.neocle.flexbans.common.command.subcommand;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.web.socket.VerifyWebSocket;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VerifyCommand implements ICommandExecutor {
    private final UserManager userManager;
    private static final FlexLogger LOGGER = FlexLogger.get(VerifyCommand.class);

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

        String[] args = invocation.getArguments();
        if (args.length != 1) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.usage"));
            return;
        }

        String code = args[0];
        String username = source.getPlayerName();
        UUID uuid = source.getPlayerUUID();

        try {
            boolean isRegistered = userManager.isUserRegistered(username).join();
            boolean isVerified = userManager.isUserVerified(username).join();

            if (isRegistered && isVerified) {
                boolean discordSet = userManager.setDiscordId(username, code).join();
                if (discordSet) {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
                } else {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                }
                return;
            }

            if (!isRegistered) {
                boolean created = userManager.createUserForVerification(username, uuid).join();
                if (!created) {
                    source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                    return;
                }
            }

            boolean verified = userManager.verifyUser(username, uuid).join();
            if (!verified) {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return;
            }

            boolean discordSet = userManager.setDiscordId(username, code).join();
            if (!discordSet) {
                source.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return;
            }

            // Optional: this second call is probably redundant now, but kept for behavior parity
            userManager.verifyUser(username, uuid).join();
            VerifyWebSocket.notifyVerified(username);

            source.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
        } catch (Exception e) {
            Component message = LanguageManager.getMessageComponent("commands.verify.error");
            source.sendMessage(message);
            LOGGER.warn(
                    LanguageManager.getMessageString("commands.logging.verify.exception")
                            .replace("%error%", e.getMessage() != null ? e.getMessage() : "Unknown error")
            );
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }
}