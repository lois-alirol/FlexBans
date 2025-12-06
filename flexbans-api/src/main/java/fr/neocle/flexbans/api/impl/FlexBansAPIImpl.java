package fr.neocle.flexbans.api.impl;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.punishment.*;
import fr.neocle.flexbans.api.server.ServerLockExecutor;
import fr.neocle.flexbans.api.server.ServerUnlockExecutor;
import fr.neocle.flexbans.api.whitelist.DiscordWhitelist;
import fr.neocle.flexbans.api.whitelist.PlayersWhitelist;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class FlexBansAPIImpl implements FlexBansAPI {
    private static Path configFile;
    private static Logger logger;
    private static EventDispatcher eventDispatcher;
    private final PlayersWhitelist playersWhitelist;
    private final DiscordWhitelist discordWhitelist;
    private static FlexBansAPI instance;
    private static BanExecutor banExecutor;
    private static MuteExecutor muteExecutor;
    private static KickExecutor kickExecutor;
    private static WarningExecutor warningExecutor;
    private static UnbanExecutor unbanExecutor;
    private static UnmuteExecutor unmuteExecutor;
    private static ServerLockExecutor serverLockExecutor;
    private static ServerUnlockExecutor serverUnlockExecutor;

    private FlexBansAPIImpl() {
        this.playersWhitelist = new PlayersWhitelist(configFile, logger, eventDispatcher);
        this.discordWhitelist = new DiscordWhitelist(configFile, logger, eventDispatcher);
    }

    public static synchronized void initialize(Path configFile, Logger logger, EventDispatcher eventDispatcher) {
        if (instance == null) {
            FlexBansAPIImpl.configFile = configFile;
            FlexBansAPIImpl.logger = logger;
            FlexBansAPIImpl.eventDispatcher = eventDispatcher;
            instance = new FlexBansAPIImpl();
        }
    }

    public static FlexBansAPI getSingletonInstance() {
        if (instance == null) {
            throw new IllegalStateException("FlexBans API is not initialized");
        }
        return instance;
    }

    public static void setBanExecutor(BanExecutor executor) {
        FlexBansAPIImpl.banExecutor = executor;
    }

    public static void setMuteExecutor(MuteExecutor executor) {
        FlexBansAPIImpl.muteExecutor = executor;
    }

    public static void setKickExecutor(KickExecutor executor) {
        FlexBansAPIImpl.kickExecutor = executor;
    }

    public static void setWarningExecutor(WarningExecutor executor) { FlexBansAPIImpl.warningExecutor = executor; }

    public static void setUnbanExecutor(UnbanExecutor executor) {
        FlexBansAPIImpl.unbanExecutor = executor;
    }

    public static void setUnmuteExecutor(UnmuteExecutor executor) { FlexBansAPIImpl.unmuteExecutor = executor; }

    public static void setServerLockExecutor(ServerLockExecutor executor) {
        FlexBansAPIImpl.serverLockExecutor = executor;
    }

    public static void setServerUnlockExecutor(ServerUnlockExecutor executor) {
        FlexBansAPIImpl.serverUnlockExecutor = executor;
    }

    @Override
    public boolean addPlayerToWhitelist(String username) {
        return playersWhitelist.addPlayer(username);
    }

    @Override
    public boolean removePlayerFromWhitelist(String username) {
        return playersWhitelist.removePlayer(username);
    }

    @Override
    public List<String> getWhitelistedPlayers() {
        return playersWhitelist.getWhitelistedPlayers();
    }

    @Override
    public boolean isPlayerWhitelisted(String username) {
        return playersWhitelist.isPlayerWhitelisted(username);
    }

    @Override
    public PlayersWhitelist getPlayersWhitelist() {
        return playersWhitelist;
    }

    @Override
    public boolean addUserToWhitelist(String userId) {
        return discordWhitelist.addUser(userId);
    }

    @Override
    public boolean removeUserFromWhitelist(String userId) {
        return discordWhitelist.removeUser(userId);
    }

    @Override
    public List<String> getWhitelistedUsers() {
        return discordWhitelist.getWhitelistedUsers();
    }

    @Override
    public boolean isUserWhitelisted(String userId) {
        return discordWhitelist.isUserWhitelisted(userId);
    }

    @Override
    public DiscordWhitelist getDiscordWhitelist() {
        return discordWhitelist;
    }

    @Override
    public BanExecutor getBanExecutor() {
        if (banExecutor == null) {
            throw new IllegalStateException("BanExecutor has not been initialized");
        }
        return banExecutor;
    }

    @Override
    public MuteExecutor getMuteExecutor() {
        if (muteExecutor == null) {
            throw new IllegalStateException("BanExecutor has not been initialized");
        }
        return muteExecutor;
    }

    @Override
    public KickExecutor getKickExecutor() {
        if (kickExecutor == null) {
            throw new IllegalStateException("KickExecutor has not been initialized");
        }
        return kickExecutor;
    }

    @Override
    public WarningExecutor getWarningExecutor() {
        if (warningExecutor == null) {
            throw new IllegalStateException("WarningExecutor has not been initialized");
        }
        return warningExecutor;
    }

    @Override
    public UnbanExecutor getUnbanExecutor() {
        if (unbanExecutor == null) {
            throw new IllegalStateException("UnbanExecutor has not been initialized");
        }
        return unbanExecutor;
    }

    @Override
    public UnmuteExecutor getUnmuteExecutor() {
        if (unmuteExecutor == null) {
            throw new IllegalStateException("UnmuteExecutor has not been initialized");
        }
        return unmuteExecutor;
    }

    @Override
    public ServerLockExecutor getServerLockExecutor() {
        if (serverLockExecutor == null) {
            throw new IllegalStateException("ServerLockExecutor has not been initialized");
        }
        return serverLockExecutor;
    }

    @Override
    public ServerUnlockExecutor getServerUnlockExecutor() {
        if (serverUnlockExecutor == null) {
            throw new IllegalStateException("ServerUnlockExecutor has not been initialized");
        }
        return serverUnlockExecutor;
    }
}
