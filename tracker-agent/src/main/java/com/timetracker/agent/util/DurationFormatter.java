package com.timetracker.agent.util;

/**
 * Форматирование длительности для UI.
 */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    public static String formatSeconds(long totalSeconds) {
        long safeSeconds = Math.max(totalSeconds, 0L);
        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        long seconds = safeSeconds % 60L;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
}
