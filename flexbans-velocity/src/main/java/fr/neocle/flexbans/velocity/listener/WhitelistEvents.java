package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.flexbans.api.event.velocity.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.event.velocity.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.event.velocity.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.event.velocity.whitelist.UserWhitelistedEvent;
import fr.neocle.flexbans.config.WebhooksConfigManager;
import fr.neocle.flexbans.webhook.WebhookService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WhitelistEvents {

    public WhitelistEvents() {}

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
        if (!WebhooksConfigManager.getBoolean(eventType + ".enabled")) return;

        String webhookUrl = WebhooksConfigManager.getString(eventType + ".url");
        String content = WebhooksConfigManager.getString(eventType + ".content")
                .replace("%user%", user)
                .replace("%player%", user);

        boolean embedEnabled = WebhooksConfigManager.getBoolean(eventType + ".embed.enabled");
        String color = WebhooksConfigManager.getString(eventType + ".embed.color");
        String authorName = WebhooksConfigManager.getString(eventType + ".embed.author.name");
        String authorUrl = WebhooksConfigManager.getString(eventType + ".embed.author.url");
        String authorIcon = WebhooksConfigManager.getString(eventType + ".embed.author.image-url");
        String thumbnailUrl = WebhooksConfigManager.getString(eventType + ".embed.thumbnail-url")
                .replace("%user%", user)
                .replace("%player%", user);
        String title = WebhooksConfigManager.getString(eventType + ".embed.title.text");
        String titleUrl = WebhooksConfigManager.getString(eventType + ".embed.title.url");
        String description = WebhooksConfigManager.getString(eventType + ".embed.description")
                .replace("%user%", user)
                .replace("%player%", user);
        String imageUrl = WebhooksConfigManager.getString(eventType + ".embed.image-url");
        String footerText = WebhooksConfigManager.getString(eventType + ".embed.footer.text");
        String footerIcon = WebhooksConfigManager.getString(eventType + ".embed.footer.icon-url");
        boolean timestamp = WebhooksConfigManager.getBoolean(eventType + ".embed.timestamp");

        List<Map<String, Object>> fields = new ArrayList<>();
        for (Object rawField : WebhooksConfigManager.getList(eventType + ".embed.fields")) {
            if (rawField instanceof Map<?, ?> map) {
                Map<String, Object> field = (Map<String, Object>) map;
                Object value = field.get("value");
                if (value != null) {
                    field.put("value", value.toString()
                            .replace("%user%", user)
                            .replace("%player%", user));
                }
                fields.add(field);
            }
        }

        WebhookService webhookService = new WebhookService();
        webhookService.sendWebhook(
                webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon,
                thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, footerIcon, timestamp
        );
    }
}
