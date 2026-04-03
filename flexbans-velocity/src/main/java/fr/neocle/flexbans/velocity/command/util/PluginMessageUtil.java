package fr.neocle.flexbans.velocity.command.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.common.messaging.Channel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PluginMessageUtil {
    private static final MinecraftChannelIdentifier DIALOGS_IDENTIFIER = MinecraftChannelIdentifier.from(Channel.DIALOGS);

    public static void sendDialogMessage(Player player, String action) {
        player.getCurrentServer().ifPresent(server -> {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream);

                out.writeUTF(player.getUniqueId().toString());
                out.writeUTF(action);

                server.sendPluginMessage(DIALOGS_IDENTIFIER, byteStream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
