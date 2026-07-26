package com.workpulsetracker.agent.buffer;

import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Интервал активности пользователя в одном приложении/окне.
 */
public final class ActivityInterval {

    private final Instant startInstant;
    private Instant endInstant;
    private final String applicationName;
    private final String windowTitle;
    private final boolean idle;

    public ActivityInterval(
            Instant startInstant,
            Instant endInstant,
            String applicationName,
            String windowTitle,
            boolean idle
    ) {
        this.startInstant = startInstant;
        this.endInstant = endInstant;
        this.applicationName = StringUtils.isNotBlank(applicationName) ? applicationName : "unknown";
        this.windowTitle = StringUtils.isNotBlank(windowTitle) ? windowTitle : "";
        this.idle = idle;
    }

    public Instant getStartInstant() {
        return startInstant;
    }

    public Instant getEndInstant() {
        return endInstant;
    }

    public void setEndInstant(Instant endInstant) {
        this.endInstant = endInstant;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public boolean isIdle() {
        return idle;
    }

    public boolean isOpen() {
        return Objects.isNull(endInstant);
    }

    public long getDurationSeconds() {
        Instant effectiveEndInstant = Objects.nonNull(endInstant) ? endInstant : Instant.now();
        long durationSeconds = Duration.between(startInstant, effectiveEndInstant).getSeconds();
        return Math.max(durationSeconds, 0L);
    }

    @Override
    public String toString() {
        return "ActivityInterval{"
                + "start=" + startInstant
                + ", end=" + endInstant
                + ", application='" + applicationName + '\''
                + ", title='" + windowTitle + '\''
                + ", idle=" + idle
                + '}';
    }
}
