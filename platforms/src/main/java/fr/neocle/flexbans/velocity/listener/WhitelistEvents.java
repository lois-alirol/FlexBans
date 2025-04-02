package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserWhitelistedEvent;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.webhooks.WebhookService;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WhitelistEvents {
    private final Logger logger;

    public WhitelistEvents(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onUserWhitelisted(UserWhitelistedEvent event) {
        sendWebhookEvent("discord-user-added-to-whitelist", event.getUserId());
    }

    @Subscribe
    public void onUserUnwhitelisted(UserUnwhitelistedEvent event) {
        sendWebhookEvent("discord-user-removed-from-whitelist", event.getUserId());
    }

    @Subscribe
    public void onPlayerWhitelisted(PlayerWhitelistedEvent event) {
        sendWebhookEvent("player-added-to-whitelist", event.getPlayerName());
    }

    @Subscribe
    public void onPlayerUnwhitelisted(PlayerUnwhitelistedEvent event) {
        sendWebhookEvent("player-removed-from-whitelist", event.getPlayerName());
    }

    private void sendWebhookEvent(String eventType, String user) {
        boolean enabled = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".enabled"));
        if (!enabled) return;

        String webhookUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".url");
        String content = ((String) WebhooksConfigManager.getConfigValue(eventType + ".content"))
                .replace("%user%", user)
                .replace("%player%", user);

        boolean embedEnabled = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.enabled"));
        String color = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.color");
        String authorName = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.name");
        String authorUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.url");
        String authorIcon = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.author.image-url");
        String thumbnailUrl = ((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.thumbnail-url"))
                .replace("%user%", user)
                .replace("%player%", user);
        String title = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.title.text");
        String titleUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.title.url");
        String description = ((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.description"))
                .replace("%user%", user)
                .replace("%player%", user);
        String imageUrl = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.image-url");
        String footerText = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.footer.text");
        String footerIcon = (String) WebhooksConfigManager.getConfigValue(eventType + ".embed.footer.icon-url");
        boolean timestamp = Boolean.parseBoolean((String) WebhooksConfigManager.getConfigValue(eventType + ".embed.timestamp"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) WebhooksConfigManager.getConfigValue(eventType + ".embed.fields");
        if (fields != null) {
            for (Map<String, Object> field : fields) {
                field.put("value", ((String) field.get("value"))
                        .replace("%user%", user)
                        .replace("%player%", user));
            }
        }

        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon,
                thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, footerIcon, timestamp);
    }
}
