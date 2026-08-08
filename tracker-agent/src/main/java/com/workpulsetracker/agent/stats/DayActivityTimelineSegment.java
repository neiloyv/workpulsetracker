package com.workpulsetracker.agent.stats;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сегмент суточного таймлайна: ACTIVE (работа), IDLE (простой) или EXCLUDED (Track OFF).
 * Промежутки без сегментов = PC Off / агент не записывал.
 */
public final class DayActivityTimelineSegment {

    private final DayActivityState activityState;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;

    public DayActivityTimelineSegment(
            DayActivityState activityState,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        this.activityState = Objects.requireNonNull(activityState);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
    }

    public DayActivityState getActivityState() {
        return activityState;
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
