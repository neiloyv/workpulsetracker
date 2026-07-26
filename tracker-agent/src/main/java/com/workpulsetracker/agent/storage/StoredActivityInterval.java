package com.workpulsetracker.agent.storage;

/**
 * DTO для сериализации интервала в JSON.
 */
public final class StoredActivityInterval {

    private String startInstant;
    private String endInstant;
    private String applicationName;
    private String windowTitle;
    private boolean idle;

    public StoredActivityInterval() {
    }

    public StoredActivityInterval(
            String startInstant,
            String endInstant,
            String applicationName,
            String windowTitle,
            boolean idle
    ) {
        this.startInstant = startInstant;
        this.endInstant = endInstant;
        this.applicationName = applicationName;
        this.windowTitle = windowTitle;
        this.idle = idle;
    }

    public String getStartInstant() {
        return startInstant;
    }

    public void setStartInstant(String startInstant) {
        this.startInstant = startInstant;
    }

    public String getEndInstant() {
        return endInstant;
    }

    public void setEndInstant(String endInstant) {
        this.endInstant = endInstant;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }
}
