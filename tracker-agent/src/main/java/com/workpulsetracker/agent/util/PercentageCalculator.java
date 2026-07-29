package com.workpulsetracker.agent.util;

/**
 * Проценты для UI (только целые числа).
 */
public final class PercentageCalculator {

    private PercentageCalculator() {
    }

    public static int calculatePercentage(long partSeconds, long totalSeconds) {
        if (totalSeconds <= 0L || partSeconds <= 0L) {
            return 0;
        }
        return (int) Math.round((partSeconds * 100.0d) / totalSeconds);
    }
}
