package com.workpulsetracker.agent.stats;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Таймлайн активности за день: диапазон оси и сегменты приложений.
 */
public final class DayActivityTimeline {

    private final LocalDateTime rangeStartDateTime;
    private final LocalDateTime rangeEndDateTime;
    private final List<DayActivityTimelineSegment> segments;

    public DayActivityTimeline(
            LocalDateTime rangeStartDateTime,
            LocalDateTime rangeEndDateTime,
            List<DayActivityTimelineSegment> segments
    ) {
        this.rangeStartDateTime = Objects.requireNonNull(rangeStartDateTime);
        this.rangeEndDateTime = Objects.requireNonNull(rangeEndDateTime);
        this.segments = Objects.isNull(segments) ? List.of() : List.copyOf(segments);
    }

    public static DayActivityTimeline empty(LocalDateTime rangeStartDateTime, LocalDateTime rangeEndDateTime) {
        return new DayActivityTimeline(rangeStartDateTime, rangeEndDateTime, Collections.emptyList());
    }

    public LocalDateTime getRangeStartDateTime() {
        return rangeStartDateTime;
    }

    public LocalDateTime getRangeEndDateTime() {
        return rangeEndDateTime;
    }

    public List<DayActivityTimelineSegment> getSegments() {
        return segments;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }
}
