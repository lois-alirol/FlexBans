package fr.neocle.flexbans.velocity.commands.helpers.punishments;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.kick.IKickCommandHelper;
import fr.neocle.flexbans.velocity.commands.utils. PluginMessageUtil;

import java.util.List;
import java.util.stream. Collectors;

public class VelocityKickCommandHelper implements IKickCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityKickCommandHelper(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void sendDialogMessage(String playerName, String dialogType) {
        proxyServer.getPlayer(playerName). ifPresent(player ->
                PluginMessageUtil. sendDialogMessage(player, dialogType)
        );
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        return proxyServer.getAllPlayers().stream()
                . map(Player::getUsername)
                .collect(Collectors.toList());
    }
}