package fr.neocle.flexbans.bukkit.listener;

import fr.neocle.flexbans.api.event.bukkit.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.event.bukkit.authentication.LogoutEvent;
import fr.neocle.flexbans.api.event.bukkit.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.event.bukkit.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.config.WebhooksConfigManager;
import fr.neocle.flexbans.webhook.WebhookService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DashboardEvents implements Listener {
    private final Logger logger;

    public DashboardEvents(Logger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        sendWebhookEvent("player-login", event.getPlayerName(), event.getClientIP(), event.getUserAgent());
    }

    @EventHandler
    public void onPlayerRegister(PlayerRegisterEvent event) {
        sendWebhookEvent("player-register", event.getPlayerName(), event.getClientIP(), event.getUserAgent());
    }

    @EventHandler
    public void onDiscordUserLogin(DiscordUserLoginEvent event) {
        sendWebhookEvent("discord-user-login", event.getUserId(), event.getClientIP(), event.getUserAgent());
    }

    @EventHandler
    public void onUserLogout(LogoutEvent event) {
        String userId = event.getUserId() != null ? event.getUserId() : "Null Discord ID";
        String playerName = event.getPlayerName() != null ? event.getPlayerName() : "Null In-game Name";
        sendWebhookEvent("user-logout", userId + " / " + playerName, event.getClientIP(), event.getUserAgent());
    }

    private void sendWebhookEvent(String eventType, String user, String ip, String userAgent) {
        boolean enabled = WebhooksConfigManager.getBoolean(eventType + ".enabled");
        if (!enabled) return;

        String webhookUrl = WebhooksConfigManager.getString(eventType + ".url");
        String content = (WebhooksConfigManager.getString(eventType + ".content"))
                .replace("%player%", user)
                .replace("%user%", user)
                .replace("%ip%", ip)
                .replace("%user-agents%", userAgent);

        boolean embedEnabled = WebhooksConfigManager.getBoolean(eventType + ".embed.enabled");
        String color = WebhooksConfigManager.getString(eventType + ".embed.color");
        String authorName = WebhooksConfigManager.getString(eventType + ".embed.author.name");
        String authorUrl = WebhooksConfigManager.getString(eventType + ".embed.author.url");
        String authorIcon = WebhooksConfigManager.getString(eventType + ".embed.author.image-url");
        String thumbnailUrl = (WebhooksConfigManager.getString(eventType + ".embed.thumbnail-url"))
                .replace("%player%", user).replace("%user%", user);
        String title = WebhooksConfigManager.getString(eventType + ".embed.title.text");
        String titleUrl = WebhooksConfigManager.getString(eventType + ".embed.title.url");
        String description = (WebhooksConfigManager.getString(eventType + ".embed.description"))
                .replace("%player%", user)
                .replace("%user%", user)
                .replace("%ip%", ip)
                .replace("%user-agents%", userAgent);
        String imageUrl = WebhooksConfigManager.getString(eventType + ".embed.image-url");
        String footerText = WebhooksConfigManager.getString(eventType + ".embed.footer.text");
        String footerIcon = WebhooksConfigManager.getString(eventType + ".embed.footer.icon-url");
        boolean timestamp = WebhooksConfigManager.getBoolean(eventType + ".embed.timestamp");

        List<Map<String, Object>> fields = new ArrayList<>();

        for (Object rawField : WebhooksConfigManager.getList(eventType + ".embed.fields")) {
            if (rawField instanceof Map<?, ?> map) {
                Map<String, Object> field = (Map<String, Object>) map;
                Object valueObj = field.get("value");
                if (valueObj != null) {
                    field.put("value", valueObj.toString()
                            .replace("%player%", user)
                            .replace("%user%", user));
                }
                fields.add(field);
            }
        }


        WebhookService webhookService = new WebhookService(logger);
        webhookService.sendWebhook(webhookUrl, content, embedEnabled, color, authorName, authorUrl, authorIcon,
                thumbnailUrl, title, titleUrl, description, fields, imageUrl, footerText, footerIcon, timestamp);
    }
}
