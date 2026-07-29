package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Имя приложения для записи интервала: для браузеров может включать сайт из title.
 */
public final class TrackedApplicationNameResolver {

    public static final String SITE_SEPARATOR = " · ";

    private TrackedApplicationNameResolver() {
    }

    public static String resolve(String processName, String windowTitle) {
        String normalizedProcessName = ApplicationNameNormalizer.normalize(processName);
        final String resolvedProcessName = StringUtils.isBlank(normalizedProcessName)
                ? "unknown"
                : normalizedProcessName;
        return BrowserSiteTitleParser.parseSiteLabel(resolvedProcessName, windowTitle)
                .map(siteLabel -> resolvedProcessName + SITE_SEPARATOR + siteLabel)
                .orElse(resolvedProcessName);
    }

    /**
     * Базовое имя процесса без суффикса сайта (для иконок и путей exe).
     */
    public static String extractBaseApplicationName(String trackedApplicationName) {
        String normalizedApplicationName = ApplicationNameNormalizer.normalize(trackedApplicationName);
        if (StringUtils.isBlank(normalizedApplicationName)) {
            return normalizedApplicationName;
        }
        int separatorIndex = normalizedApplicationName.indexOf(SITE_SEPARATOR);
        if (separatorIndex > 0) {
            return normalizedApplicationName.substring(0, separatorIndex).trim();
        }
        return normalizedApplicationName;
    }

    public static boolean hasSiteDetail(String trackedApplicationName) {
        return StringUtils.isNotBlank(trackedApplicationName)
                && trackedApplicationName.contains(SITE_SEPARATOR);
    }

    public static String extractSiteLabel(String trackedApplicationName) {
        if (!hasSiteDetail(trackedApplicationName)) {
            return "";
        }
        int separatorIndex = trackedApplicationName.indexOf(SITE_SEPARATOR);
        return trackedApplicationName.substring(separatorIndex + SITE_SEPARATOR.length()).trim();
    }
}
