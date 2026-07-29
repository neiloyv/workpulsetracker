package com.workpulsetracker.agent.stats;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Матрица использования приложений по колонкам периода.
 */
public final class ApplicationUsageMatrix {

    private final StatsPeriod statsPeriod;
    private final List<PeriodBucket> periodBuckets;
    private final List<String> applicationNames;
    private final long[][] durationSecondsByApplicationAndBucket;
    private final long[] applicationTotalSeconds;
    private final long totalActiveSeconds;

    public ApplicationUsageMatrix(
            StatsPeriod statsPeriod,
            List<PeriodBucket> periodBuckets,
            List<String> applicationNames,
            long[][] durationSecondsByApplicationAndBucket,
            long[] applicationTotalSeconds,
            long totalActiveSeconds
    ) {
        this.statsPeriod = Objects.requireNonNull(statsPeriod);
        this.periodBuckets = List.copyOf(periodBuckets);
        this.applicationNames = List.copyOf(applicationNames);
        this.durationSecondsByApplicationAndBucket = durationSecondsByApplicationAndBucket;
        this.applicationTotalSeconds = applicationTotalSeconds;
        this.totalActiveSeconds = totalActiveSeconds;
    }

    public static ApplicationUsageMatrix empty(StatsPeriod statsPeriod, List<PeriodBucket> periodBuckets) {
        return new ApplicationUsageMatrix(
                statsPeriod,
                periodBuckets,
                Collections.emptyList(),
                new long[0][0],
                new long[0],
                0L
        );
    }

    public StatsPeriod getStatsPeriod() {
        return statsPeriod;
    }

    public List<PeriodBucket> getPeriodBuckets() {
        return periodBuckets;
    }

    public List<String> getApplicationNames() {
        return applicationNames;
    }

    public long getDurationSeconds(int applicationIndex, int bucketIndex) {
        return durationSecondsByApplicationAndBucket[applicationIndex][bucketIndex];
    }

    public long getApplicationTotalSeconds(int applicationIndex) {
        return applicationTotalSeconds[applicationIndex];
    }

    public long getTotalActiveSeconds() {
        return totalActiveSeconds;
    }
}
