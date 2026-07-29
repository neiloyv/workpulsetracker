package com.workpulsetracker.agent.stats;

import java.time.Instant;
import java.util.Objects;

/**
 * Колонка периода в матрице статистики (день / неделя / месяц / год).
 */
public final class PeriodBucket {

    private final String label;
    private final Instant startInclusive;
    private final Instant endExclusive;

    public PeriodBucket(String label, Instant startInclusive, Instant endExclusive) {
        this.label = Objects.requireNonNull(label);
        this.startInclusive = Objects.requireNonNull(startInclusive);
        this.endExclusive = Objects.requireNonNull(endExclusive);
    }

    public String getLabel() {
        return label;
    }

    public Instant getStartInclusive() {
        return startInclusive;
    }

    public Instant getEndExclusive() {
        return endExclusive;
    }
}
