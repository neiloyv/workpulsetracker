package com.timetracker.agent.stats;

/**
 * Время, проведённое в одном приложении.
 */
public final class ApplicationUsageSummary {

    private final String applicationName;
    private final long durationSeconds;

    public ApplicationUsageSummary(String applicationName, long durationSeconds) {
        this.applicationName = applicationName;
        this.durationSeconds = durationSeconds;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }
}
