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
import java.util.Collections;
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
    private final List<StatisticsReportTable> reportTables;

    private StatisticsReportSection(
            StatsPeriod statsPeriod,
            String periodTitle,
            long totalActiveSeconds,
            List<ApplicationUsageSummary> applicationUsageSummaries,
            List<StatisticsReportTable> reportTables
    ) {
        this.statsPeriod = statsPeriod;
        this.periodTitle = periodTitle;
        this.totalActiveSeconds = totalActiveSeconds;
        this.applicationUsageSummaries = applicationUsageSummaries;
        this.reportTables = List.copyOf(reportTables);
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

    public static StatisticsReportSection build(
            StatisticsService statisticsService,
            StatsPeriod statsPeriod,
            int minorUsageThresholdMinutes
    ) {
        return buildForAnchor(
                statisticsService,
                statsPeriod,
                LocalDate.now(),
                minorUsageThresholdMinutes
        );
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
                List.of(new StatisticsReportTable(null, applicationUsageMatrix))
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

    public List<StatisticsReportTable> getReportTables() {
        return Collections.unmodifiableList(reportTables);
    }
}
