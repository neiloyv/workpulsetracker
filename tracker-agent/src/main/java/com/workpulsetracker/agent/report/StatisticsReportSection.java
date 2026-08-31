package com.workpulsetracker.agent.report;

import com.workpulsetracker.agent.stats.ApplicationUsageBrowserGrouper;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageGroup;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.stats.StatisticsSnapshot;
import com.workpulsetracker.agent.stats.StatsPeriod;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Данные одного раздела отчёта (период + таблицы + данные для круговой диаграммы).
 */
public final class StatisticsReportSection {

    private final StatsPeriod statsPeriod;
    private final String periodTitle;
    private final long totalActiveSeconds;
    private final List<ApplicationUsageSummary> applicationUsageSummaries;
    private final ApplicationUsageMatrix applicationUsageMatrix;

    private StatisticsReportSection(
            StatsPeriod statsPeriod,
            String periodTitle,
            long totalActiveSeconds,
            List<ApplicationUsageSummary> applicationUsageSummaries,
            ApplicationUsageMatrix applicationUsageMatrix
    ) {
        this.statsPeriod = statsPeriod;
        this.periodTitle = periodTitle;
        this.totalActiveSeconds = totalActiveSeconds;
        this.applicationUsageSummaries = applicationUsageSummaries;
        this.applicationUsageMatrix = applicationUsageMatrix;
    }

    public static List<StatisticsReportSection> buildAll(
            StatisticsService statisticsService,
            int minorUsageThresholdMinutes
    ) {
        Objects.requireNonNull(statisticsService);
        LocalDate today = LocalDate.now();
        return Arrays.stream(new StatsPeriod[]{
                        StatsPeriod.WEEK,
                        StatsPeriod.MONTH,
                        StatsPeriod.YEAR
                })
                .map(statsPeriod -> buildForAnchor(
                        statisticsService,
                        statsPeriod,
                        today,
                        minorUsageThresholdMinutes
                ))
                .collect(Collectors.toList());
    }

    public static StatisticsReportSection buildForAnchor(
            StatisticsService statisticsService,
            StatsPeriod statsPeriod,
            LocalDate anchorDate,
            int minorUsageThresholdMinutes
    ) {
        Objects.requireNonNull(statisticsService);
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : LocalDate.now();
        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshotForAnchor(
                statsPeriod,
                resolvedAnchorDate
        );
        List<ApplicationUsageSummary> applicationUsageSummaries = ApplicationUsageFilter.groupMinorApplicationGroups(
                        ApplicationUsageBrowserGrouper.group(statisticsSnapshot.getApplicationUsageSummaries()),
                        minorUsageThresholdMinutes
                ).stream()
                .map(ApplicationUsageGroup::toSummary)
                .collect(Collectors.toList());
        ApplicationUsageMatrix applicationUsageMatrix = ApplicationUsageFilter.groupMinorApplications(
                ApplicationUsageBrowserGrouper.collapseBrowserApplications(
                        statisticsService.buildApplicationUsageMatrixForAnchor(statsPeriod, resolvedAnchorDate)
                ),
                minorUsageThresholdMinutes
        );
        String periodTitle = statisticsService.formatPeriodCaption(statsPeriod, resolvedAnchorDate)
                + " ("
                + resolvePeriodTitle(statsPeriod)
                + ")";
        return new StatisticsReportSection(
                statsPeriod,
                periodTitle,
                statisticsSnapshot.getTotalActiveSeconds(),
                applicationUsageSummaries,
                applicationUsageMatrix
        );
    }

    private static String resolvePeriodTitle(StatsPeriod statsPeriod) {
        return switch (statsPeriod) {
            case DAY -> Messages.get(MessageCodes.UI_STATS_PERIOD_DAY);
            case WEEK -> Messages.get(MessageCodes.UI_STATS_PERIOD_WEEK);
            case MONTH -> Messages.get(MessageCodes.UI_STATS_PERIOD_MONTH);
            case YEAR -> Messages.get(MessageCodes.UI_STATS_PERIOD_YEAR);
            case ALL_TIME -> Messages.get(MessageCodes.UI_STATS_PERIOD_ALL);
            case CUSTOM -> Messages.get(MessageCodes.UI_STATS_PERIOD_CUSTOM);
        };
    }

    public StatsPeriod getStatsPeriod() {
        return statsPeriod;
    }

    public String getPeriodTitle() {
        return periodTitle;
    }

    public long getTotalActiveSeconds() {
        return totalActiveSeconds;
    }

    public List<ApplicationUsageSummary> getApplicationUsageSummaries() {
        return applicationUsageSummaries;
    }

    public ApplicationUsageMatrix getApplicationUsageMatrix() {
        return applicationUsageMatrix;
    }
}
