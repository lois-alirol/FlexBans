package fr.neocle.flexbans.bukkit.listener;

import fr.neocle.flexbans.bukkit.dialog.DialogFactory;
import fr.neocle.flexbans.bukkit.dialog.DialogKeys;
import fr.neocle.flexbans.common.messaging.Channel;
import fr.neocle.flexbans.logger.FlexLogger;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.UUID;

public class DialogEvents implements PluginMessageListener, Listener {
    private final JavaPlugin plugin;

    public DialogEvents(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(Channel.DIALOGS)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            UUID uuid = UUID.fromString(in.readUTF());
            String type = in.readUTF().toLowerCase();

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) return;

            switch (type) {
                case "ban" -> player.showDialog(DialogFactory.createPunishDialog("Ban", DialogKeys.BAN_EXECUTE));
                case "mute" -> player.showDialog(DialogFactory.createPunishDialog("Mute", DialogKeys.MUTE_EXECUTE));
                case "kick" -> player.showDialog(DialogFactory.createKickDialog());
                case "warning" -> player.showDialog(DialogFactory.createWarningDialog());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCustomClickEvent(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;

        Key id = event.getIdentifier();
        DialogResponseView view = event.getDialogResponseView();

        if (id.equals(DialogKeys.BAN_EXECUTE)) handlePunishment(conn.getPlayer(), view, Channel.BAN, "ban");
        else if (id.equals(DialogKeys.MUTE_EXECUTE)) handlePunishment(conn.getPlayer(), view, Channel.MUTE, "mute");
        else if (id.equals(DialogKeys.KICK_EXECUTE)) handleKick(conn.getPlayer(), view);
        else if (id.equals(DialogKeys.WARNING_EXECUTE)) handleWarning(conn.getPlayer(), view);
    }

    private void handlePunishment(Player player, DialogResponseView view, String channel, String type) {
        String target = view.getText("target_name");
        String reason = view.getText("reason");
        boolean permanent = view.getBoolean("permanent");

        String durationUnit = view.getText("duration_unit");
        String duration = permanent ? "Permanent" : (view.getFloat("duration_value") + " " + (durationUnit != null ? durationUnit : "minutes"));

        boolean ipScope = view.getText("ip_scope").contains("ip");

        sendToProxy(
                player,
                channel,
                target,
                duration,
                reason,
                view.getText("server_scope"),
                view.getBoolean("silent"),
                ipScope
        );
    }

    private void handleKick(Player player, DialogResponseView view) {
        sendToProxy(
                player,
                Channel.KICK,
                view.getText("target_name"),
                "N/A",
                view.getText("reason"),
                "local",
                view.getBoolean("silent"),
                false
        );
    }

    private void handleWarning(Player player, DialogResponseView view) {
        sendToProxy(
                player,
                Channel.KICK,
                view.getText("target_name"),
                "N/A",
                view.getText("reason"),
                view.getText("server_scope"),
                view.getBoolean("silent"),
                view.getText("ip_scope").contains("ip")
        );
    }

    private void sendToProxy(
            Player player,
            String channel,
            String target,
            String duration,
            String reason,
            String scope,
            boolean silent,
            boolean ipScope
    ) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF(target);
            out.writeUTF(player.getName());
            out.writeUTF(duration);
            out.writeUTF(reason);
            out.writeUTF(scope);
            out.writeBoolean(silent);
            out.writeBoolean(ipScope);

            player.sendPluginMessage(plugin, channel, b.toByteArray());
        } catch (IOException e) {
            player.sendMessage(Component.text("Error sending data to proxy.")
                    .color(NamedTextColor.RED));
        }
    }
}