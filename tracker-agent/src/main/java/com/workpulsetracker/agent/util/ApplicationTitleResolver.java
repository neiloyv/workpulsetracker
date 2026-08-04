package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Человекочитаемое имя приложения (app_title / displayTitle):
 * FileDescription через Win32 Version API (JNA), иначе имя процесса без .exe.
 */
public final class ApplicationTitleResolver {

    private ApplicationTitleResolver() {
    }

    public static String resolveDisplayTitle(String processName, String processImagePath) {
        String fileDescription = null;
        String operatingSystemName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (operatingSystemName.contains("win") && StringUtils.isNotBlank(processImagePath)) {
            fileDescription = WindowsFileDescriptionResolver.resolveFileDescription(processImagePath);
        }
        if (StringUtils.isNotBlank(fileDescription)) {
            return fileDescription.trim();
        }
        // Fallback: техническое имя без расширения (.exe → idea64).
        return ApplicationNameNormalizer.normalize(processName);
    }

    /**
     * Стабильный идентификатор приложения для telemetry (нижний регистр, без расширения).
     */
    public static String resolveAppIdentifier(String processName, String displayTitle) {
        String baseName = StringUtils.isNotBlank(displayTitle)
                ? displayTitle
                : processName;
        String normalized = ApplicationNameNormalizer.normalize(baseName);
        if (StringUtils.isBlank(normalized)) {
            return "unknown";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
