package com.workpulsetracker.agent.util;

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

    /**
     * Формат для статистики: часы:минуты без секунд.
     */
    public static String formatHoursMinutes(long totalSeconds) {
        long safeSeconds = Math.max(totalSeconds, 0L);
        long hours = safeSeconds / 3600L;
        long minutes = (safeSeconds % 3600L) / 60L;
        return String.format("%d:%02d", hours, minutes);
    }

    /**
     * Формат ячейки статистики: {@code H:MM (N%)} или только {@code H:MM}.
     */
    public static String formatStatisticsCell(
            long durationSeconds,
            long totalActiveSeconds,
            boolean showPercentages
    ) {
        if (durationSeconds <= 0L) {
            return "—";
        }
        if (showPercentages) {
            return formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
        }
        return formatHoursMinutes(durationSeconds);
    }

    /**
     * Формат ячейки статистики: {@code H:MM (N%)}.
     */
    public static String formatHoursMinutesWithPercent(long durationSeconds, long totalActiveSeconds) {
        int percentage = PercentageCalculator.calculatePercentage(durationSeconds, totalActiveSeconds);
        return formatHoursMinutes(durationSeconds) + " (" + percentage + "%)";
    }
}
