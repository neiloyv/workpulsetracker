package com.workpulsetracker.agent.stats;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Сегмент таймлайна активности за день.
 */
public final class DayActivityTimelineSegment {

    private final String applicationName;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;

    public DayActivityTimelineSegment(
            String applicationName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        this.applicationName = Objects.requireNonNull(applicationName);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public long getDurationSeconds() {
        return Math.max(0L, java.time.Duration.between(startDateTime, endDateTime).getSeconds());
    }
}
