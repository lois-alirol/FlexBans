package fr.neocle.flexbans.webhook;

import fr.neocle.flexbans.logger.FlexLogger;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WebhookService {

    public WebhookService() {
    }

    public void sendWebhook(String url, String content, boolean embedEnabled, String color, String authorName,
                            String authorUrl, String authorIcon, String thumbnailUrl, String title, String titleUrl,
                            String description, List<Map<String, Object>> fields, String imageUrl, String footerText,
                            String footerIcon, boolean timestamp) {

        if (url == null || url.isEmpty()) {
            FlexLogger.warn("Webhook URL is not set.");
            return;
        }

        Webhook webhook = new Webhook(url);

        if (content != null && !content.isEmpty()) {
            webhook.setContent(content);
        }

        if (embedEnabled) {
            Webhook.EmbedObject embed = new Webhook.EmbedObject()
                    .setTitle(title)
                    .setUrl(titleUrl)
                    .setDescription(description);

            if (color != null && !color.isEmpty()) {
                try {
                    embed.setColor(Color.decode(color.startsWith("#") ? color : "#" + color));
                } catch (NumberFormatException e) {
                    FlexLogger.warn("Invalid color format: " + color);
                }
            }

            if (authorName != null || authorUrl != null || authorIcon != null) {
                embed.setAuthor(authorName, authorUrl, authorIcon);
            }

            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                embed.setThumbnail(thumbnailUrl);
            }

            if (imageUrl != null && !imageUrl.isEmpty()) {
                embed.setImage(imageUrl);
            }

            if (footerText != null || footerIcon != null) {
                embed.setFooter(footerText, footerIcon);
            }

            if (fields != null && !fields.isEmpty()) {
                for (Map<String, Object> field : fields) {
                    String fieldName = (String) field.get("name");
                    String fieldValue = (String) field.get("value");
                    boolean fieldInline = Boolean.parseBoolean(String.valueOf(field.getOrDefault("inline", "false")));
                    embed.addField(fieldName, fieldValue, fieldInline);
                }
            }

            if (timestamp) {
                embed.setTimestamp(java.time.OffsetDateTime.now().toString());
            }

            webhook.addEmbed(embed);
        }

        try {
            webhook.execute();
        } catch (IOException e) {
            FlexLogger.warn("Failed to send webhook: " + e.getMessage());
        }
    }
}
