package com.timetracker.agent.stats;

import java.util.List;

/**
 * Снимок статистики за выбранный период.
 */
public final class StatisticsSnapshot {

    private final StatsPeriod statsPeriod;
    private final long totalActiveSeconds;
    private final List<DailyUsageSummary> dailyUsageSummaries;
    private final List<ApplicationUsageSummary> applicationUsageSummaries;

    public StatisticsSnapshot(
            StatsPeriod statsPeriod,
            long totalActiveSeconds,
            List<DailyUsageSummary> dailyUsageSummaries,
            List<ApplicationUsageSummary> applicationUsageSummaries
    ) {
        this.statsPeriod = statsPeriod;
        this.totalActiveSeconds = totalActiveSeconds;
        this.dailyUsageSummaries = dailyUsageSummaries;
        this.applicationUsageSummaries = applicationUsageSummaries;
    }

    public StatsPeriod getStatsPeriod() {
        return statsPeriod;
    }

    public long getTotalActiveSeconds() {
        return totalActiveSeconds;
    }

    public List<DailyUsageSummary> getDailyUsageSummaries() {
        return dailyUsageSummaries;
    }

    public List<ApplicationUsageSummary> getApplicationUsageSummaries() {
        return applicationUsageSummaries;
    }
}
