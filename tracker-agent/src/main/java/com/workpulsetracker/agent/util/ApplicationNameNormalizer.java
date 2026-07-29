package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Нормализация имени приложения для отображения и агрегации.
 */
public final class ApplicationNameNormalizer {

    private ApplicationNameNormalizer() {
    }

    /**
     * Убирает расширение исполняемого файла (например {@code .exe}), оставляя только имя программы.
     */
    public static String normalize(String applicationName) {
        if (StringUtils.isBlank(applicationName)) {
            return applicationName;
        }
        String trimmedApplicationName = applicationName.trim();
        if (StringUtils.endsWithIgnoreCase(trimmedApplicationName, ".exe")) {
            return trimmedApplicationName.substring(0, trimmedApplicationName.length() - 4);
        }
        return trimmedApplicationName;
    }
}
