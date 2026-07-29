package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Извлекает сайт/домен из заголовка окна браузера (Chrome, Edge, Firefox и др.).
 */
public final class BrowserSiteTitleParser {

    private static final int MAX_SITE_LABEL_LENGTH = 48;
    private static final Set<String> BROWSER_PROCESS_NAMES = Stream.of(
                    "chrome",
                    "msedge",
                    "chromium",
                    "brave",
                    "opera",
                    "vivaldi",
                    "firefox",
                    "waterfox",
                    "librewolf",
                    "browser",
                    "yandex",
                    "arc",
                    "whale",
                    "safari"
            )
            .collect(Collectors.toUnmodifiableSet());

    private static final Pattern BROWSER_TITLE_SUFFIX_PATTERN = Pattern.compile(
            "(?i)\\s*[-–—|]\\s*(Google Chrome|Microsoft\\s*Edge|Mozilla Firefox|Brave|Opera|Vivaldi|Chromium|Firefox|Arc|Yandex|Яндекс\\s*Браузер|Whale)\\s*$"
    );
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "(?i)(?:https?://)?(?:www\\.)?((?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,})(?:[/:?#]|$|\\s)"
    );
    private static final Set<String> EMPTY_PAGE_TITLES = Stream.of(
                    "new tab",
                    "new tab page",
                    "новая вкладка",
                    "нова вкладка",
                    "about:blank",
                    "about:newtab",
                    "startpage",
                    "edge start page",
                    "microsoft edge start page"
            )
            .collect(Collectors.toUnmodifiableSet());

    private BrowserSiteTitleParser() {
    }

    public static boolean isBrowserProcess(String processName) {
        String normalizedProcessName = ApplicationNameNormalizer.normalize(processName);
        if (StringUtils.isBlank(normalizedProcessName)) {
            return false;
        }
        return BROWSER_PROCESS_NAMES.contains(normalizedProcessName.toLowerCase(Locale.ROOT));
    }

    /**
     * @return метка сайта (домен или короткое имя), если удалось извлечь из title
     */
    public static Optional<String> parseSiteLabel(String processName, String windowTitle) {
        if (!isBrowserProcess(processName) || StringUtils.isBlank(windowTitle)) {
            return Optional.empty();
        }

        String titleWithoutBrowserSuffix = stripBrowserSuffix(windowTitle.trim());
        if (StringUtils.isBlank(titleWithoutBrowserSuffix) || isEmptyPageTitle(titleWithoutBrowserSuffix)) {
            return Optional.empty();
        }

        Optional<String> hostLabel = extractHostLabel(titleWithoutBrowserSuffix);
        if (hostLabel.isPresent()) {
            return hostLabel;
        }

        return extractFallbackSiteLabel(titleWithoutBrowserSuffix);
    }

    private static String stripBrowserSuffix(String windowTitle) {
        Matcher matcher = BROWSER_TITLE_SUFFIX_PATTERN.matcher(windowTitle);
        if (matcher.find()) {
            return windowTitle.substring(0, matcher.start()).trim();
        }
        return windowTitle;
    }

    private static boolean isEmptyPageTitle(String titleWithoutBrowserSuffix) {
        return EMPTY_PAGE_TITLES.contains(titleWithoutBrowserSuffix.toLowerCase(Locale.ROOT));
    }

    private static Optional<String> extractHostLabel(String titleWithoutBrowserSuffix) {
        Matcher matcher = HOST_PATTERN.matcher(titleWithoutBrowserSuffix);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String hostName = matcher.group(1).toLowerCase(Locale.ROOT);
        if (StringUtils.startsWithIgnoreCase(hostName, "www.")) {
            hostName = hostName.substring(4);
        }
        return Optional.of(truncate(hostName));
    }

    private static Optional<String> extractFallbackSiteLabel(String titleWithoutBrowserSuffix) {
        String[] titleParts = Arrays.stream(titleWithoutBrowserSuffix.split("\\s*[-–—|]\\s*"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        if (titleParts.length == 0) {
            return Optional.empty();
        }
        String candidateSiteLabel = titleParts.length >= 2
                ? titleParts[titleParts.length - 1]
                : titleParts[0];
        if (isEmptyPageTitle(candidateSiteLabel) || candidateSiteLabel.length() < 2) {
            return Optional.empty();
        }
        return Optional.of(truncate(candidateSiteLabel));
    }

    private static String truncate(String siteLabel) {
        if (siteLabel.length() <= MAX_SITE_LABEL_LENGTH) {
            return siteLabel;
        }
        return siteLabel.substring(0, MAX_SITE_LABEL_LENGTH - 1).trim() + "…";
    }
}
