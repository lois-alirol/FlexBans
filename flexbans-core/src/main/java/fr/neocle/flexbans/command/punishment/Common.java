package fr.neocle.flexbans.command.punishment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.util.player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Common {

    private static final Gson GSON = new Gson();
    private static final String CONSOLE_NAME = "Console";

    public static boolean isFloodgateLoaded() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static UUID parseUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static UUID resolveTargetUUID(String target, FloodgateApi floodgateApi, UsernameUUIDConverters usernameUUIDConverters) {
        UUID targetUUID = parseUUID(target);
        if (targetUUID != null) return targetUUID;

        if (floodgateApi != null && target.startsWith(floodgateApi.getPlayerPrefix())) {
            return parseUUID(usernameUUIDConverters.usernameToUUID(target));
        }

        return fetchUUIDFromMojang(target);
    }

    public static UUID fetchUUIDFromMojang(String playerName) {
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json.has("id")) {
                        String rawId = json.get("id").getAsString();

                        String formatted = rawId.replaceFirst(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                                "$1-$2-$3-$4-$5"
                        );
                        return UUID.fromString(formatted);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static long parseDuration(String duration) {
        try {
            if (duration.endsWith("mo")) {
                long time = Long.parseLong(duration.substring(0, duration.length() - 2));
                return TimeUnit.DAYS.toMillis(time * 30);
            }
            char unit = duration.charAt(duration.length() - 1);
            long time = Long.parseLong(duration.substring(0, duration.length() - 1));

            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(time);
                case 'm' -> TimeUnit.MINUTES.toMillis(time);
                case 'h' -> TimeUnit.HOURS.toMillis(time);
                case 'd' -> TimeUnit.DAYS.toMillis(time);
                case 'w' -> TimeUnit.DAYS.toMillis(time * 7);
                case 'y' -> TimeUnit.DAYS.toMillis(time * 365);
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }

    public static Optional<PlayerInfo> resolveTarget(String target, Consumer<String> messageSender,
                                                                 FloodgateApi floodgateApi, UsernameUUIDConverters converters) {
        UUID targetUUID = resolveTargetUUID(target, floodgateApi, converters);
        if (targetUUID == null) {
            messageSender.accept("§cError: Player '" + target + "' does not exist.");
            return Optional.empty();
        }

        String targetName = converters.UUIDtoUsername(targetUUID.toString());
        if (targetName == null || targetName.contains("Error")) {
            messageSender.accept("§cError: Could not retrieve username.");
            return Optional.empty();
        }

        return Optional.of(new PlayerInfo(targetUUID, targetName));
    }

    public static Optional<PlayerInfo> resolveSender(String sender, Consumer<String> messageSender,
                                                                 FloodgateApi floodgateApi, UsernameUUIDConverters converters) {
        String senderName = (sender != null && !sender.isEmpty()) ? sender : CONSOLE_NAME;
        UUID senderUUID = Common.parseUUID(converters.usernameToUUID(senderName));

        if (senderUUID == null) {
            messageSender.accept("§cError: Sender must be an actual player.");
            return Optional.empty();
        }

        return Optional.of(new PlayerInfo(senderUUID, senderName));
    }

    public record PlayerInfo(UUID uuid, String name) {}
}
