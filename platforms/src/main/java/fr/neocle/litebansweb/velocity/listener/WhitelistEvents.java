package fr.neocle.litebansweb.velocity.listener;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;

import fr.neocle.litebansweb.api.events.velocity.VelocityUserWhitelistedEvent;
import fr.neocle.litebansweb.webhooks.WebhookService;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerUnwhitelistedEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerWhitelistedEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityUserUnwhitelistedEvent;

@SuppressWarnings("unchecked")
public class WhitelistEvents {
    private final Map<String, Object> webhooksConfig;
    private final Logger logger;
    
    public WhitelistEvents(Map<String, Object> webhooksConfig, Logger logger) {
        this.webhooksConfig = webhooksConfig;
        this.logger = logger;
    }

    @Subscribe
    public void onUserWhitelisted(VelocityUserWhitelistedEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("discord-user-added-to-whitelist");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));
    
        if (!enabled) return;
    
        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%user%", event.getUserId());
    
        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));
    
        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%user%", event.getUserId());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%user%", event.getUserId());
        String imageUrl = String.valueOf(embedConfig.get("image-url"));
        String footerText = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("text"));
        String footerIcon = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("icon-url"));
        boolean timestamp = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("timestamp", "false")));
    
        List<Map<String, Object>> fields = (List<Map<String, Object>>) embedConfig.get("fields");
        for (Map<String, Object> field : fields) {
            field.put("value", String.valueOf(field.get("value")).replace("%user%", event.getUserId()));
        }
    
        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon, 
                                   thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, 
                                   footerIcon, timestamp);
    }
    
    @Subscribe
    public void onUserUnwhitelisted(VelocityUserUnwhitelistedEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("discord-user-removed-from-whitelist");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));
    
        if (!enabled) return;
    
        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%user%", event.getUserId());
    
        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));
    
        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%user%", event.getUserId());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%user%", event.getUserId());
        String imageUrl = String.valueOf(embedConfig.get("image-url"));
        String footerText = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("text"));
        String footerIcon = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("icon-url"));
        boolean timestamp = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("timestamp", "false")));
    
        List<Map<String, Object>> fields = (List<Map<String, Object>>) embedConfig.get("fields");
        for (Map<String, Object> field : fields) {
            field.put("value", String.valueOf(field.get("value")).replace("%user%", event.getUserId()));
        }
    
        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon, 
                                   thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, 
                                   footerIcon, timestamp);
    }

    @Subscribe
    public void onPlayerWhitelisted(VelocityPlayerWhitelistedEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("player-added-to-whitelist");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));
    
        if (!enabled) return;
    
        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%player%", event.getPlayerName());
    
        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));
    
        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%player%", event.getPlayerName());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%player%", event.getPlayerName());
        String imageUrl = String.valueOf(embedConfig.get("image-url"));
        String footerText = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("text"));
        String footerIcon = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("icon-url"));
        boolean timestamp = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("timestamp", "false")));
    
        List<Map<String, Object>> fields = (List<Map<String, Object>>) embedConfig.get("fields");
        for (Map<String, Object> field : fields) {
            field.put("value", String.valueOf(field.get("value")).replace("%player%", event.getPlayerName()));
        }
    
        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon, 
                                   thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, 
                                   footerIcon, timestamp);
    }

    @Subscribe
    public void onPlayerUnwhitelisted(VelocityPlayerUnwhitelistedEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("player-removed-from-whitelist");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));
    
        if (!enabled) return;
    
        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%player%", event.getPlayerName());
    
        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));
    
        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%player%", event.getPlayerName());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%player%", event.getPlayerName());
        String imageUrl = String.valueOf(embedConfig.get("image-url"));
        String footerText = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("text"));
        String footerIcon = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("icon-url"));
        boolean timestamp = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("timestamp", "false")));
    
        List<Map<String, Object>> fields = (List<Map<String, Object>>) embedConfig.get("fields");
        for (Map<String, Object> field : fields) {
            field.put("value", String.valueOf(field.get("value")).replace("%player%", event.getPlayerName()));
        }
    
        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon, 
                                   thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, 
                                   footerIcon, timestamp);
    }
}
