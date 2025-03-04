package fr.neocle.litebansweb.velocity.listener;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import fr.neocle.litebansweb.api.events.velocity.VelocityDiscordUserLoginEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityLogoutEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerLoginEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerRegisterEvent;
import fr.neocle.litebansweb.webhooks.WebhookService;

@SuppressWarnings("unchecked")
public class DashboardEvents {
    private final Map<String, Object> webhooksConfig;
    private final Logger logger;

    public DashboardEvents(Map<String, Object> webhooksConfig, Logger logger) {
        this.webhooksConfig = webhooksConfig;
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerLogin(VelocityPlayerLoginEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("player-login");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));

        if (!enabled) return;

        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%player%", event.getPlayerName())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());

        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));

        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%player%", event.getPlayerName());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%player%", event.getPlayerName())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());
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
    public void onPlayerRegister(VelocityPlayerRegisterEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("player-register");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));

        if (!enabled) return;

        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%player%", event.getPlayerName())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());

        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));

        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%player%", event.getPlayerName());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%player%", event.getPlayerName())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());
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
    public void onDiscordUserLogin(VelocityDiscordUserLoginEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("discord-user-login");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));

        if (!enabled) return;

        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%user%", event.getUserId())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());

        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));

        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%user%", event.getUserId());
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%user%", event.getUserId())
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());
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
    public void onUserLogout(VelocityLogoutEvent event) {
        Map<String, Object> config = (Map<String, Object>) webhooksConfig.get("user-logout");
        boolean enabled = Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));

        if (!enabled) return;

        String userId = event.getUserId() != null ? event.getUserId() : "Null Discord ID";
        String playerName = event.getPlayerName() != null ? event.getPlayerName() : "Null In-game Name";

        String webhookUrl = String.valueOf(config.get("url"));
        String content = String.valueOf(config.get("content")).replace("%user%", userId + " / " + playerName)
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());

        Map<String, Object> embedConfig = (Map<String, Object>) config.get("embed");
        boolean embedEnabled = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("enabled", "false")));

        String color = String.valueOf(embedConfig.get("color"));
        String authorName = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("name"));
        String authorUrl = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("url"));
        String authorIcon = String.valueOf(((Map<String, Object>) embedConfig.get("author")).get("image-url"));
        String thumbnailUrl = String.valueOf(embedConfig.get("thumbnail-url")).replace("%user%", userId + " / " + playerName);
        String title = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("text"));
        String titleUrl = String.valueOf(((Map<String, Object>) embedConfig.get("title")).get("url"));
        String description = String.valueOf(embedConfig.get("description")).replace("%user%", userId + " / " + playerName)
                .replace("%ip%", event.getClientIP())
                .replace("%user-agents%", event.getUserAgent());
        String imageUrl = String.valueOf(embedConfig.get("image-url"));
        String footerText = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("text"));
        String footerIcon = String.valueOf(((Map<String, Object>) embedConfig.get("footer")).get("icon-url"));
        boolean timestamp = Boolean.parseBoolean(String.valueOf(embedConfig.getOrDefault("timestamp", "false")));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) embedConfig.get("fields");
        for (Map<String, Object> field : fields) {
            field.put("value", String.valueOf(field.get("value")).replace("%user%", userId + " / " + playerName));
        }

        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon,
                thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText,
                footerIcon, timestamp);
    }
}
