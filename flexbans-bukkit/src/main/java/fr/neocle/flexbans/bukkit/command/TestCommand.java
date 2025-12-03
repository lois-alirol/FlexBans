package fr.neocle.flexbans.bukkit.command;

import fr.neocle.flexbans.bukkit.dialog.BanDialog;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.key.Key;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class TestCommand implements CommandExecutor, Listener {

    private static final Key BAN_KEY = Key.key("flexbans:ban_player");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        Dialog banDialog = BanDialog.createBanDialog();
        BanDialog.showDialog(player);

        return true;
    }
}