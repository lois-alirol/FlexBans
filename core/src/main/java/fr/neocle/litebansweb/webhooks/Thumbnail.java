package fr.neocle.litebansweb.webhooks;

public class Thumbnail {
    private final String url;

    public Thumbnail(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}