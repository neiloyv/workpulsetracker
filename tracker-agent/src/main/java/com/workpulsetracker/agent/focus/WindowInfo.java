package com.workpulsetracker.agent.focus;

import com.workpulsetracker.agent.util.ApplicationNameNormalizer;
import com.workpulsetracker.agent.util.ApplicationTitleResolver;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Снимок активного окна ОС.
 */
public final class WindowInfo {

    private final String processName;
    private final String windowTitle;
    private final String processImagePath;
    private final String displayTitle;
    private final String appIdentifier;

    public WindowInfo(String processName, String windowTitle) {
        this(processName, windowTitle, null, null);
    }

    public WindowInfo(String processName, String windowTitle, String processImagePath) {
        this(processName, windowTitle, processImagePath, null);
    }

    public WindowInfo(
            String processName,
            String windowTitle,
            String processImagePath,
            String displayTitle
    ) {
        this.processName = StringUtils.isNotBlank(processName)
                ? ApplicationNameNormalizer.normalize(processName)
                : "unknown";
        this.windowTitle = StringUtils.isNotBlank(windowTitle) ? windowTitle.trim() : "";
        this.processImagePath = StringUtils.isNotBlank(processImagePath) ? processImagePath.trim() : null;
        this.displayTitle = StringUtils.isNotBlank(displayTitle)
                ? displayTitle.trim()
                : ApplicationTitleResolver.resolveDisplayTitle(this.processName, this.processImagePath);
        this.appIdentifier = ApplicationTitleResolver.resolveAppIdentifier(this.processName, this.displayTitle);
    }

    public String getProcessName() {
        return processName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public String getProcessImagePath() {
        return processImagePath;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public String getAppIdentifier() {
        return appIdentifier;
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
            return displayTitle;
        }
        return displayTitle + " — \"" + windowTitle + "\"";
    }
}
