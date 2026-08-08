package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Стабильный ключ программы для категорий и Track (приложение целиком, без сайта в браузере).
 */
public final class ProgramApplicationKeyResolver {

    private ProgramApplicationKeyResolver() {
    }

    public static String resolveProgramKey(String applicationName) {
        if (Objects.isNull(applicationName) || StringUtils.isBlank(applicationName)) {
            return "unknown";
        }
        String baseApplicationName = TrackedApplicationNameResolver.extractBaseApplicationName(applicationName);
        String normalizedApplicationName = ApplicationNameNormalizer.normalize(baseApplicationName);
        return StringUtils.isNotBlank(normalizedApplicationName) ? normalizedApplicationName : "unknown";
    }
}
