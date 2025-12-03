package fr.neocle.flexbans.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Webhook {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final String url;
    private String content;
    private boolean tts = false;
    private final List<EmbedObject> embeds = new ArrayList<>();

    public Webhook(String url) {
        this.url = url;
    }

    public Webhook setContent(String content) {
        this.content = content;
        return this;
    }

    public Webhook setTts(boolean tts) {
        this.tts = tts;
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

        WebhookPayload payload = new WebhookPayload(content, tts, embeds);

        String json = GSON.toJson(payload);

        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("User-Agent", "Java-DiscordWebhook");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(json.getBytes("UTF-8"));
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_NO_CONTENT) {

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    throw new IOException("Discord webhook error: " + responseCode + " - " + response);
                }
            }
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
        }

        connection.disconnect();
    }


    private static class WebhookPayload {
        private final String content;
        private final boolean tts;
        private final List<EmbedObject> embeds;

        public WebhookPayload(String content, boolean tts, List<EmbedObject> embeds) {
            this.content = content;
            this.tts = tts;
            this.embeds = embeds;
        }
    }

    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private String timestamp;
        private Integer color;

        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private final List<Field> fields = new ArrayList<>();

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EmbedObject setColor(Color color) {
            if (color != null)
                this.color = color.getRGB() & 0xFFFFFF;
            return this;
        }

        public EmbedObject setFooter(String text, String iconUrl) {
            this.footer = new Footer(text, iconUrl);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String iconUrl) {
            this.author = new Author(name, url, iconUrl);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }
    }

    public static class Footer {
        private final String text;
        @SerializedName("icon_url")
        private final String iconUrl;

        public Footer(String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
        }
    }

    public static class Thumbnail {
        private final String url;

        public Thumbnail(String url) {
            this.url = url;
        }
    }

    public static class Image {
        private final String url;

        public Image(String url) {
            this.url = url;
        }
    }

    public static class Author {
        private final String name;
        private final String url;
        @SerializedName("icon_url")
        private final String iconUrl;

        public Author(String name, String url, String iconUrl) {
            this.name = name;
            this.url = url;
            this.iconUrl = iconUrl;
        }
    }

    public static class Field {
        private final String name;
        private final String value;
        private final boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}
