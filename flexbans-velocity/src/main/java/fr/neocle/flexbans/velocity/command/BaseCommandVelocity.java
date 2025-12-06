package fr.neocle.flexbans.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.command.AbstractBaseCommand;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.command.subcommand.HelpCommand;
import fr.neocle.flexbans.common.command.subcommand.ReloadCommand;
import fr.neocle.flexbans.common.command.subcommand.VerifyCommand;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.web.IndexHandler;
import fr.neocle.flexbans.handler.web.security.AuthenticationHandler;
import fr.neocle.flexbans.handler.web.security.oauth. DiscordOAuthHandler;
import fr.neocle.flexbans.util.JettyReloader;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.subcommand.*;

import java.nio.file.Path;
import java.util.List;

public class BaseCommandVelocity extends AbstractBaseCommand implements SimpleCommand {

    public BaseCommandVelocity(FlexBansAPI api, ProxyServer proxyServer, Path dataFolder,
                               AuthenticationHandler authenticationHandler, DiscordOAuthHandler discordOAuthHandler,
                               IndexHandler indexHandler, JettyReloader jettyReloader, DatabaseUtils databaseUtils,
                               String version) {
        registerSubCommand("help", new HelpCommand());
        registerSubCommand("reload", new ReloadCommand(dataFolder, authenticationHandler, indexHandler, jettyReloader));
        registerSubCommand("verify", new VerifyCommand(databaseUtils.getUserManager()));
        registerSubCommand("players", new PlayersWhitelist(api, proxyServer, authenticationHandler));
        registerSubCommand("discord", new DiscordWhitelist(api, proxyServer, discordOAuthHandler));
        registerSubCommand("dump", new Dump(proxyServer, version));
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        ICommandInvocation wrappedInvocation = new VelocityCommandInvocation(invocation, source);
        super.execute(wrappedInvocation);
    }

    @Override
    public List<String> suggest(SimpleCommand. Invocation invocation) {
        CommandSource source = invocation.source();
        ICommandInvocation wrappedInvocation = new VelocityCommandInvocation(invocation, source);
        return super.suggest(wrappedInvocation);
    }
}