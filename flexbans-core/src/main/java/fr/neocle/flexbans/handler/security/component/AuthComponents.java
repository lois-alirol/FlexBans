package fr.neocle.flexbans.handler.security.component;

import fr.neocle.flexbans.handler.security.CodeVerificationHandler;
import fr.neocle.flexbans.handler.security.LoginHandler;
import fr.neocle.flexbans.handler.security.RegisterHandler;
import fr.neocle.flexbans.handler.security.oauth.DiscordOAuthHandler;

public class AuthComponents {
    private final CodeVerificationHandler codeVerificationHandler;
    private final RegisterHandler registerHandler;
    private final LoginHandler loginHandler;
    private final DiscordOAuthHandler discordOAuthHandler;

    public AuthComponents(
            CodeVerificationHandler codeVerificationHandler,
            RegisterHandler registerHandler,
            LoginHandler loginHandler,
            DiscordOAuthHandler discordOAuthHandler) {
        this.codeVerificationHandler = codeVerificationHandler;
        this.registerHandler = registerHandler;
        this.loginHandler = loginHandler;
        this.discordOAuthHandler = discordOAuthHandler;
    }

    public CodeVerificationHandler getCodeVerificationHandler() {
        return codeVerificationHandler;
    }

    public RegisterHandler getRegisterHandler() {
        return registerHandler;
    }

    public LoginHandler getLoginHandler() {
        return loginHandler;
    }

    public DiscordOAuthHandler getDiscordOAuthHandler() {
        return discordOAuthHandler;
    }
}