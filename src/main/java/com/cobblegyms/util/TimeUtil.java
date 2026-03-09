package com.cobblegyms.util;

import java.time.*;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;

public class TimeUtil {

    public static String formatDuration(long millis) {
        if (millis <= 0) return "0 seconds";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day" : " days");
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.isEmpty() ? "0 seconds" : sb.toString();
    }

    public static long getWeekStart() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate monday = today.with(WeekFields.ISO.dayOfWeek(), 1);
        return monday.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static boolean isExpired(long timestamp) {
        return System.currentTimeMillis() > timestamp;
    }

    public static String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);
        return String.format("%04d-%02d-%02d %02d:%02d UTC",
                zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                zdt.getHour(), zdt.getMinute());
    }

    public static long hoursToMillis(int hours) {
        return (long) hours * 3600 * 1000;
    }
}
