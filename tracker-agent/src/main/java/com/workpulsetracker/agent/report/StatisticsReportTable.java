package com.workpulsetracker.agent.report;

import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Одна таблица внутри раздела отчёта (для месяца — таблица одной недели по дням).
 */
public final class StatisticsReportTable {

    private final String title;
    private final ApplicationUsageMatrix applicationUsageMatrix;

    public StatisticsReportTable(String title, ApplicationUsageMatrix applicationUsageMatrix) {
        this.title = StringUtils.trimToNull(title);
        this.applicationUsageMatrix = Objects.requireNonNull(applicationUsageMatrix);
    }

    public String getTitle() {
        return title;
    }

    public boolean hasTitle() {
        return StringUtils.isNotBlank(title);
    }

    public ApplicationUsageMatrix getApplicationUsageMatrix() {
        return applicationUsageMatrix;
    }
}
