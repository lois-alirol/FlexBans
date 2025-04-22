package fr.neocle.flexbans.bungee.commands.SubCommands;

import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Logger;

public class Verify extends Command {
    private final Logger logger;
    private final UserManager userManager;

    public Verify(Logger logger, DatabaseUtils databaseUtils) {
        super("verify", "flexbans.verify");
        this.logger = logger;
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.players-only"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("flexbans.verify")) {
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.usage"));
            return;
        }

        String code = args[0];
        String username = player.getName();

        if (userManager.isUserRegistered(username) && userManager.isPlayerVerified(username)) {
            boolean success = userManager.setDiscordIdFromCode(username, code);
            if (success) {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.success"));
            } else {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
            }
            return;
        }

        try {
            userManager.insertUsername(username, code);
            userManager.setVerifiedStatusForPlayerName(username, true);
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.success"));
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                player.sendMessage(Arrays.stream(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.unknown-code"))
                        .map(component -> component instanceof TextComponent ? new TextComponent(((TextComponent) component).getText().replace("%code%", code)) : component)
                        .toArray(BaseComponent[]::new));
            } else {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
                logger.warning(LanguageManager.getMessageString("commands.logging.verify.sql-exception").replace("%error%", e.getMessage()));
            }
        } catch (Exception e) {
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.error"));
            logger.warning(LanguageManager.getMessageString("commands.logging.verify.exception").replace("%error%", e.getMessage()));
        }
    }
}
