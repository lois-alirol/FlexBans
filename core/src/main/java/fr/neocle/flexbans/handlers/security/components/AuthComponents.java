package fr.neocle.flexbans.handlers.security.components;

import fr.neocle.flexbans.handlers.security.CodeVerificationHandler;
import fr.neocle.flexbans.handlers.security.LoginHandler;
import fr.neocle.flexbans.handlers.security.RegisterHandler;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;

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