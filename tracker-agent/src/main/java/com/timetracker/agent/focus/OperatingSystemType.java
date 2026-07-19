package com.timetracker.agent.focus;

import org.apache.commons.lang3.StringUtils;

/**
 * Тип ОС, на которой запущен агент.
 * Нужен, чтобы выбирать нативную реализацию и тип установщика.
 */
public enum OperatingSystemType {

    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    public static OperatingSystemType detect() {
        String operatingSystemName = System.getProperty("os.name", "unknown");
        String normalizedOperatingSystemName = StringUtils.lowerCase(operatingSystemName);

        if (StringUtils.contains(normalizedOperatingSystemName, "win")) {
            return WINDOWS;
        }
        if (StringUtils.contains(normalizedOperatingSystemName, "mac")
                || StringUtils.contains(normalizedOperatingSystemName, "darwin")) {
            return MACOS;
        }
        if (StringUtils.contains(normalizedOperatingSystemName, "nux")
                || StringUtils.contains(normalizedOperatingSystemName, "nix")
                || StringUtils.contains(normalizedOperatingSystemName, "aix")) {
            return LINUX;
        }
        return UNKNOWN;
    }
}
