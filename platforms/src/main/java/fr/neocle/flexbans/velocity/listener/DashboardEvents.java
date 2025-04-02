package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.flexbans.api.events.velocity.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.LogoutEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.webhooks.WebhookService;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DashboardEvents {
    private final Logger logger;

    public DashboardEvents(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerLogin(PlayerLoginEvent event) {
        sendWebhookEvent("player-login", event.getPlayerName(), event.getClientIP(), event.getUserAgent());
    }

    @Subscribe
    public void onPlayerRegister(PlayerRegisterEvent event) {
        sendWebhookEvent("player-register", event.getPlayerName(), event.getClientIP(), event.getUserAgent());
    }

    @Subscribe
    public void onDiscordUserLogin(DiscordUserLoginEvent event) {
        sendWebhookEvent("discord-user-login", event.getUserId(), event.getClientIP(), event.getUserAgent());
    }

    @Subscribe
    public void onUserLogout(LogoutEvent event) {
        String userId = event.getUserId() != null ? event.getUserId() : "Null Discord ID";
        String playerName = event.getPlayerName() != null ? event.getPlayerName() : "Null In-game Name";
        sendWebhookEvent("user-logout", userId + " / " + playerName, event.getClientIP(), event.getUserAgent());
    }

    private void sendWebhookEvent(String eventType, String user, String ip, String userAgent) {
        boolean enabled = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".enabled"));
        if (!enabled) return;

        String webhookUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".url");
        String content = ((String) WebhooksConfigManager.getConfigValue(eventType + ".content"))
                .replace("%player%", user)
                .replace("%user%", user)
                .replace("%ip%", ip)
                .replace("%user-agents%", userAgent);

        boolean embedEnabled = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.enabled"));
        String color = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.color");
        String authorName = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.name");
        String authorUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.url");
        String authorIcon = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.image-url");
        String thumbnailUrl = ((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.thumbnail-url"))
                .replace("%player%", user).replace("%user%", user);
        String title = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.title.text");
        String titleUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.title.url");
        String description = ((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.description"))
                .replace("%player%", user)
                .replace("%user%", user)
                .replace("%ip%", ip)
                .replace("%user-agents%", userAgent);
        String imageUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.image-url");
        String footerText = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.footer.text");
        String footerIcon = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.footer.icon-url");
        boolean timestamp = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.timestamp"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) WebhooksConfigManager.getConfigValue(eventType + ".embed.fields");
        if (fields != null) {
            for (Map<String, Object> field : fields) {
                field.put("value", ((String) field.get("value")).replace("%player%", user).replace("%user%", user));
            }
        }

        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon,
                thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, footerIcon, timestamp);
    }
}
