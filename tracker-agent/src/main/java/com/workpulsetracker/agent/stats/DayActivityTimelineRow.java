package com.workpulsetracker.agent.stats;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Строка многодневного таймлайна: дата дня и сегменты 00:00–24:00.
 */
public final class DayActivityTimelineRow {

    private final LocalDate dayDate;
    private final DayActivityTimeline dayActivityTimeline;

    public DayActivityTimelineRow(LocalDate dayDate, DayActivityTimeline dayActivityTimeline) {
        this.dayDate = Objects.requireNonNull(dayDate);
        this.dayActivityTimeline = Objects.requireNonNull(dayActivityTimeline);
    }

    public LocalDate getDayDate() {
        return dayDate;
    }

    public DayActivityTimeline getDayActivityTimeline() {
        return dayActivityTimeline;
    }
}
