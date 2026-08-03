package com.workpulsetracker.agent.buffer;

import com.workpulsetracker.agent.util.ApplicationNameNormalizer;
import com.workpulsetracker.agent.util.ApplicationTitleResolver;
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
    private final String appIdentifier;
    private final String displayTitle;

    public ActivityInterval(
            Instant startInstant,
            Instant endInstant,
            String applicationName,
            String windowTitle,
            boolean idle
    ) {
        this(startInstant, endInstant, applicationName, windowTitle, idle, null, null);
    }

    public ActivityInterval(
            Instant startInstant,
            Instant endInstant,
            String applicationName,
            String windowTitle,
            boolean idle,
            String appIdentifier,
            String displayTitle
    ) {
        this.startInstant = startInstant;
        this.endInstant = endInstant;
        this.applicationName = StringUtils.isNotBlank(applicationName)
                ? ApplicationNameNormalizer.normalize(applicationName)
                : "unknown";
        this.windowTitle = StringUtils.isNotBlank(windowTitle) ? windowTitle : "";
        this.idle = idle;
        this.displayTitle = StringUtils.isNotBlank(displayTitle)
                ? displayTitle.trim()
                : this.applicationName;
        this.appIdentifier = StringUtils.isNotBlank(appIdentifier)
                ? appIdentifier.trim().toLowerCase()
                : ApplicationTitleResolver.resolveAppIdentifier(this.applicationName, this.displayTitle);
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

    public String getAppIdentifier() {
        return appIdentifier;
    }

    public String getDisplayTitle() {
        return displayTitle;
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
