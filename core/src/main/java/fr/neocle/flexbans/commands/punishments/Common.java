package fr.neocle.flexbans.commands.punishments;

import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import org.geysermc.floodgate.api.FloodgateApi;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Common {

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
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                String response = new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                JSONObject json = new JSONObject(response);
                return UUID.fromString(json.getString("id").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (IOException ignored) {}
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
}
