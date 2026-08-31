package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Все производные имени приложения от пары (процесс ОС, заголовок окна) — в одном месте.
 *
 * <ul>
 *   <li><b>tracked name</b> — {@link #resolve}: имя для записи интервала. Для браузера это
 *       {@code "chrome · youtube.com"}, для остальных — нормализованное имя процесса.
 *       Ключ группировки статистики и колонка {@code application_name} в БД.</li>
 *   <li><b>base name</b> — {@link #extractBaseApplicationName}: tracked name без суффикса сайта
 *       ({@code "chrome · youtube.com"} → {@code "chrome"}). Для иконок и путей к exe.</li>
 *   <li><b>program key</b> — {@link #resolveProgramKey}: base name как ключ пользовательских
 *       настроек (категория программы, Track ON/OFF).</li>
 *   <li><b>site label</b> — {@link #extractSiteLabel}: часть после {@code " · "}.</li>
 * </ul>
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

    /**
     * Стабильный ключ программы для категорий и Track (приложение целиком, без сайта в браузере).
     */
    public static String resolveProgramKey(String applicationName) {
        if (StringUtils.isBlank(applicationName)) {
            return "unknown";
        }
        String baseApplicationName = ApplicationNameNormalizer.normalize(extractBaseApplicationName(applicationName));
        return StringUtils.isNotBlank(baseApplicationName) ? baseApplicationName : "unknown";
    }
}
