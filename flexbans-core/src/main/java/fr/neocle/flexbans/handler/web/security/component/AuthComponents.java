package fr.neocle.flexbans.handler.web.security.component;

import fr.neocle.flexbans.handler.web.security.CodeVerificationHandler;
import fr.neocle.flexbans.handler.web.security.LoginHandler;
import fr.neocle.flexbans.handler.web.security.RegisterHandler;
import fr.neocle.flexbans.handler.web.security.oauth.DiscordOAuthHandler;

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