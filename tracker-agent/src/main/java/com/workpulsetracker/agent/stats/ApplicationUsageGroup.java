package com.workpulsetracker.agent.stats;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Группа приложений для UI: браузер (chrome/msedge/firefox/…) и его сайты-вкладки.
 */
public final class ApplicationUsageGroup {

    private final String applicationName;
    private final long durationSeconds;
    private final List<ApplicationUsageSummary> siteChildren;

    public ApplicationUsageGroup(
            String applicationName,
            long durationSeconds,
            List<ApplicationUsageSummary> siteChildren
    ) {
        this.applicationName = Objects.requireNonNull(applicationName);
        this.durationSeconds = Math.max(durationSeconds, 0L);
        this.siteChildren = Objects.isNull(siteChildren) ? List.of() : List.copyOf(siteChildren);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public List<ApplicationUsageSummary> getSiteChildren() {
        return siteChildren;
    }

    public boolean isExpandable() {
        return !siteChildren.isEmpty();
    }

    public ApplicationUsageSummary toSummary() {
        return new ApplicationUsageSummary(applicationName, durationSeconds);
    }

    public static ApplicationUsageGroup leaf(String applicationName, long durationSeconds) {
        return new ApplicationUsageGroup(applicationName, durationSeconds, Collections.emptyList());
    }
}
