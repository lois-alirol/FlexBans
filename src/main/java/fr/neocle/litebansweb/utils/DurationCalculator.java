package fr.neocle.litebansweb.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class DurationCalculator {

    public String calculateDuration(long startTime, long endTime) {
        if (endTime == -1 || endTime == 0) {
            return "Permanent";
        }

        Instant startInstant = Instant.ofEpochMilli(startTime);
        Instant endInstant = Instant.ofEpochMilli(endTime);

        LocalDateTime startDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault());

        long years = ChronoUnit.YEARS.between(startDateTime, endDateTime);
        startDateTime = startDateTime.plusYears(years);

        long months = ChronoUnit.MONTHS.between(startDateTime, endDateTime);
        startDateTime = startDateTime.plusMonths(months);

        long days = ChronoUnit.DAYS.between(startDateTime, endDateTime);
        startDateTime = startDateTime.plusDays(days);

        long hours = ChronoUnit.HOURS.between(startDateTime, endDateTime);
        startDateTime = startDateTime.plusHours(hours);

        long minutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime);
        startDateTime = startDateTime.plusMinutes(minutes);

        long seconds = ChronoUnit.SECONDS.between(startDateTime, endDateTime);

        StringBuilder durationBuilder = new StringBuilder();
        if (years > 0) durationBuilder.append(years).append(" years ");
        if (months > 0) durationBuilder.append(months).append(" months ");
        if (days > 0) durationBuilder.append(days).append(" days ");
        if (hours > 0) durationBuilder.append(hours).append(" hours ");
        if (minutes > 0) durationBuilder.append(minutes).append(" minutes ");
        if (seconds > 0) durationBuilder.append(seconds).append(" seconds");

        return durationBuilder.toString().trim();
    }
}
