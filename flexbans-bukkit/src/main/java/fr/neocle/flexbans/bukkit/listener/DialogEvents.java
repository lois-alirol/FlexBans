package fr.neocle.flexbans.bukkit.listener;

import fr.neocle.flexbans.bukkit.dialog.BanDialog;
import fr.neocle.flexbans.bukkit.dialog.KickDialog;
import fr.neocle.flexbans.bukkit.dialog.MuteDialog;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class DialogEvents implements PluginMessageListener, Listener {
    private final JavaPlugin plugin;
    private static final String DIALOGS_IDENTIFIER = "flexbans:dialogs";
    private static final String BAN_CHANNEL = "flexbans:ban";

    private static final Key DIALOG_KEY = Key.key("flexbans", "ban_dialog");
    private static final Key BAN_EXECUTE_ACTION_KEY = Key.key("flexbans", "ban_execute");

    private static final String KEY_TARGET_NAME = "ban_target_name";
    private static final String KEY_REASON = "ban_reason";
    private static final String KEY_DURATION_VALUE = "ban_duration_value";

    private static final String KEY_DURATION_UNIT = "ban_duration_unit";

    private static final String KEY_PERMANENT = "ban_permanent";
    private static final String KEY_SILENT = "ban_silent";
    private static final String KEY_SERVER_SCOPE = "ban_server_scope";
    private static final String KEY_BAN_TYPE = "ban_type";

    public DialogEvents(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {

        if (!channel.equals(DIALOGS_IDENTIFIER)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

            UUID uuid = UUID.fromString(in.readUTF());
            String dialogType = in.readUTF();

            Player targetPlayer = Bukkit.getPlayer(uuid);
            if (targetPlayer == null) {
                return;
            }

            switch (dialogType.toLowerCase()) {
                case "ban":
                    BanDialog.showDialog(targetPlayer);
                    break;
                case "mute":
                    MuteDialog.showDialog(targetPlayer);
                    break;
                case "kick":
                    KickDialog.showDialog(targetPlayer);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCustomClickEvent(PlayerCustomClickEvent event) {
        System.out.println(event.getIdentifier());

        if (!(event.getCommonConnection() instanceof PlayerGameConnection playerConnection)) {
            return;
        }

        Player player = playerConnection.getPlayer();
        if (player == null) {
            return;
        }

        if (event.getIdentifier().equals(BAN_EXECUTE_ACTION_KEY)) {
            handleExecuteBan(player, event.getDialogResponseView());
        }
    }

    public void handleExecuteBan(Player player, DialogResponseView input) {

        String target = input.getText(KEY_TARGET_NAME);
        String reason = input.getText(KEY_REASON);

        float durationValue = input.getFloat(KEY_DURATION_VALUE);
        long durationValueLong = (long) durationValue;

        String unit = input.getText(KEY_DURATION_UNIT);
        boolean permanent = input.getBoolean(KEY_PERMANENT);

        boolean silent = input.getBoolean(KEY_SILENT);
        String scope = input.getText(KEY_SERVER_SCOPE);
        String banType = input.getText(KEY_BAN_TYPE);

        long durationMs;

        if (permanent) {
            durationMs = -1L;
            unit = "permanent";
        } else {
            switch (unit) {
                case "minutes": durationMs = durationValueLong * 60_000L; break;
                case "hours":   durationMs = durationValueLong * 3_600_000L; break;
                case "days":    durationMs = durationValueLong * 86_400_000L; break;
                case "months":  durationMs = durationValueLong * 30L * 86_400_000L; break;
                case "years":   durationMs = durationValueLong * 365L * 86_400_000L; break;
                default:
                    durationMs = 0;
            }
        }

        String durationString = permanent ? "permanent" : (durationValueLong + " " + unit);
        boolean ipScope = "ip".equalsIgnoreCase(banType);

        sendBanToProxy(player, target, player.getName(), durationString,
                        reason, scope, silent, ipScope);

        player.sendMessage(Component.text("Ban data sent to proxy.").color(NamedTextColor.GREEN));
    }

    private void sendBanToProxy(Player player,
                                String target,
                                String sender,
                                String duration,
                                String reason,
                                String serverScope,
                                boolean silent,
                                boolean ipScope) {

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF(target != null ? target : "");
            out.writeUTF(sender != null ? sender : "");
            out.writeUTF(duration != null ? duration : "");
            out.writeUTF(reason != null ? reason : "");
            out.writeUTF(serverScope != null ? serverScope : "");
            out.writeBoolean(silent);
            out.writeBoolean(ipScope);

            player.sendPluginMessage(plugin, BAN_CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to send ban data to proxy.").color(NamedTextColor.RED));
            e.printStackTrace();
        }
    }
}