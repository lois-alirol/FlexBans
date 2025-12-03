package fr.neocle.flexbans.velocity.command.helper.punishment;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.punishment.mute.IMuteCommandHelper;
import fr.neocle.flexbans.velocity.command.util. PluginMessageUtil;

public class VelocityMuteCommandHelper implements IMuteCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityMuteCommandHelper(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void sendDialogMessage(String playerName, String dialogType) {
        proxyServer.getPlayer(playerName).ifPresent(player ->
                PluginMessageUtil.sendDialogMessage(player, dialogType)
        );
    }

    @Override
    public boolean isServerRegistered(String serverName) {
        return proxyServer.getServer(serverName).isPresent();
    }
}