package com.timetracker.agent.focus;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Снимок активного окна ОС.
 */
public final class WindowInfo {

    private final String processName;
    private final String windowTitle;

    public WindowInfo(String processName, String windowTitle) {
        this.processName = StringUtils.isNotBlank(processName) ? processName.trim() : "unknown";
        this.windowTitle = StringUtils.isNotBlank(windowTitle) ? windowTitle.trim() : "";
    }

    public String getProcessName() {
        return processName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public boolean isSameWindow(WindowInfo otherWindowInfo) {
        if (Objects.isNull(otherWindowInfo)) {
            return false;
        }
        return Objects.equals(processName, otherWindowInfo.processName)
                && Objects.equals(windowTitle, otherWindowInfo.windowTitle);
    }

    @Override
    public String toString() {
        if (StringUtils.isBlank(windowTitle)) {
            return processName;
        }
        return processName + " — \"" + windowTitle + "\"";
    }
}
