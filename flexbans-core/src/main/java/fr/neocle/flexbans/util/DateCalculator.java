package fr.neocle.flexbans.util;

import java.util.concurrent.TimeUnit;

public class DateCalculator {
    public static String formatDuration(long duration) {
        if (duration <= 0) {
            return "Permanent";
        }

        StringBuilder sb = new StringBuilder();

        long days = TimeUnit.MILLISECONDS.toDays(duration);
        duration -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        duration -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        duration -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);

        boolean hasAppended = false;

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
            hasAppended = true;
        }

        if (hours > 0) {
            if (hasAppended) {
                sb.append(", ");
            }
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            hasAppended = true;
        }

        if (minutes > 0) {
            if (hasAppended) {
                sb.append(", ");
            }
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            hasAppended = true;
        }

        if (seconds > 0 || (!hasAppended && duration > 0)) {
            if (hasAppended) {
                sb.append(", ");
            }
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    public static String formatExpiration(long banTime, long duration) {
        if (duration <= 0) {
            return "∞";
        }

        long expirationTime = banTime + duration;
        long currentTime = System.currentTimeMillis();

        if (currentTime >= expirationTime) {
            return "Expired";
        }

        long timeRemaining = expirationTime - currentTime;
        return formatDuration(timeRemaining);
    }

    public static String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }
}