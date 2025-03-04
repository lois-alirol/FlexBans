package fr.neocle.litebansweb.webhooks;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Webhook {

    private final String url;
    private String content;
    private final List<EmbedObject> embeds = new ArrayList<>();

    public Webhook(String url) {
        this.url = url;
    }

    public Webhook setContent(String content) {
        this.content = content;
        return this;
    }

    public Webhook addEmbed(EmbedObject embed) {
        embeds.add(embed);
        return this;
    }

    public void execute() throws IOException {
        if (content == null && embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
        }
    
        JSONObject json = new JSONObject();
    
        if (content != null) {
            json.put("content", content);
        }
        json.put("tts", false);
    
        if (!embeds.isEmpty()) {
            JSONArray embedArray = new JSONArray();
    
            for (EmbedObject embed : embeds) {
                JSONObject jsonEmbed = new JSONObject();
                jsonEmbed.put("title", embed.getTitle());
                jsonEmbed.put("description", embed.getDescription());
                jsonEmbed.put("url", embed.getUrl());
    
                if (embed.getColor() != null) {
                    jsonEmbed.put("color", embed.getColor().getRGB() & 0xFFFFFF);
                }
    
                if (embed.getFooter() != null) {
                    JSONObject footer = new JSONObject();
                    footer.put("text", embed.getFooter().getText());
                    footer.put("icon_url", embed.getFooter().getIconUrl());
                    jsonEmbed.put("footer", footer);
                }
    
                if (embed.getThumbnail() != null) {
                    JSONObject thumbnail = new JSONObject();
                    thumbnail.put("url", embed.getThumbnail().getUrl());
                    jsonEmbed.put("thumbnail", thumbnail);
                }
    
                if (embed.getImage() != null) {
                    JSONObject image = new JSONObject();
                    image.put("url", embed.getImage().getUrl());
                    jsonEmbed.put("image", image);
                }
    
                if (embed.getAuthor() != null) {
                    JSONObject author = new JSONObject();
                    author.put("name", embed.getAuthor().getName());
                    author.put("url", embed.getAuthor().getUrl());
                    author.put("icon_url", embed.getAuthor().getIconUrl());
                    jsonEmbed.put("author", author);
                }
    
                if (!embed.getFields().isEmpty()) {
                    JSONArray fieldsArray = new JSONArray();
                    for (Field field : embed.getFields()) {
                        JSONObject jsonField = new JSONObject();
                        jsonField.put("name", field.getName());
                        jsonField.put("value", field.getValue());
                        jsonField.put("inline", field.isInline());
                        fieldsArray.put(jsonField);
                    }
                    jsonEmbed.put("fields", fieldsArray);
                }

                if (embed.getTimestamp() != null) {
                    jsonEmbed.put("timestamp", embed.getTimestamp());
                }
    
                embedArray.put(jsonEmbed);
            }
    
            json.put("embeds", embedArray);
        }
    
        @SuppressWarnings("deprecation")
        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
    
        byte[] outputBytes = json.toString().getBytes("UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(outputBytes.length));
    
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(outputBytes);
            outputStream.flush();
        }
    
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
        } else {
        }
    
        connection.getInputStream().close();
        connection.disconnect();
    }
}
