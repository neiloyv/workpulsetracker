package com.workpulsetracker.agent.stats;

import java.time.LocalDate;

/**
 * Суммарное рабочее время за один день.
 */
public final class DailyUsageSummary {

    private final LocalDate date;
    private final long durationSeconds;

    public DailyUsageSummary(LocalDate date, long durationSeconds) {
        this.date = date;
        this.durationSeconds = durationSeconds;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }
}
