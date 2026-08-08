package com.workpulsetracker.agent.util;

import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Красивое отображаемое имя приложения по техническому имени процесса.
 * Ключи статистики/иконок не меняет — только UI-подписи.
 */
public final class ApplicationDisplayNameResolver {

    private static final Map<String, String> KNOWN_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("idea64", "IntelliJ IDEA"),
            Map.entry("idea", "IntelliJ IDEA"),
            Map.entry("studio64", "Android Studio"),
            Map.entry("pycharm64", "PyCharm"),
            Map.entry("webstorm64", "WebStorm"),
            Map.entry("clion64", "CLion"),
            Map.entry("rider64", "Rider"),
            Map.entry("datagrip64", "DataGrip"),
            Map.entry("goland64", "GoLand"),
            Map.entry("phpstorm64", "PhpStorm"),
            Map.entry("rubymine64", "RubyMine"),
            Map.entry("code", "Visual Studio Code"),
            Map.entry("code - insiders", "Visual Studio Code - Insiders"),
            Map.entry("devenv", "Visual Studio"),
            Map.entry("chrome", "Google Chrome"),
            Map.entry("msedge", "Microsoft Edge"),
            Map.entry("firefox", "Mozilla Firefox"),
            Map.entry("brave", "Brave"),
            Map.entry("opera", "Opera"),
            Map.entry("explorer", "File Explorer"),
            Map.entry("windowsterminal", "Windows Terminal"),
            Map.entry("cmd", "Command Prompt"),
            Map.entry("powershell", "Windows PowerShell"),
            Map.entry("pwsh", "PowerShell"),
            Map.entry("winword", "Microsoft Word"),
            Map.entry("excel", "Microsoft Excel"),
            Map.entry("powerpnt", "Microsoft PowerPoint"),
            Map.entry("outlook", "Microsoft Outlook"),
            Map.entry("teams", "Microsoft Teams"),
            Map.entry("slack", "Slack"),
            Map.entry("discord", "Discord"),
            Map.entry("spotify", "Spotify"),
            Map.entry("telegram", "Telegram"),
            Map.entry("notepad", "Notepad"),
            Map.entry("notepad++", "Notepad++"),
            Map.entry("java", "Java"),
            Map.entry("javaw", "Java"),
            Map.entry("cursor", "Cursor")
    );

    private static final ConcurrentHashMap<String, String> displayNameCache = new ConcurrentHashMap<>();

    private ApplicationDisplayNameResolver() {
    }

    /**
     * Возвращает подпись для UI: FileDescription / известный alias / исходное имя.
     * Для браузеров сохраняет суффикс сайта: {@code Google Chrome · github.com}.
     */
    public static String resolveDisplayName(String trackedApplicationName) {
        if (StringUtils.isBlank(trackedApplicationName)) {
            return trackedApplicationName;
        }
        if (isOthersCategory(trackedApplicationName)) {
            return trackedApplicationName;
        }

        String cachedDisplayName = displayNameCache.get(trackedApplicationName);
        if (Objects.nonNull(cachedDisplayName)) {
            return cachedDisplayName;
        }

        String baseApplicationName = TrackedApplicationNameResolver.extractBaseApplicationName(trackedApplicationName);
        String friendlyBaseName = resolveFriendlyBaseName(baseApplicationName);
        String resolvedDisplayName = trackedApplicationName;
        if (TrackedApplicationNameResolver.hasSiteDetail(trackedApplicationName)) {
            String siteLabel = TrackedApplicationNameResolver.extractSiteLabel(trackedApplicationName);
            resolvedDisplayName = friendlyBaseName + TrackedApplicationNameResolver.SITE_SEPARATOR + siteLabel;
        } else {
            resolvedDisplayName = friendlyBaseName;
        }

        displayNameCache.put(trackedApplicationName, resolvedDisplayName);
        return resolvedDisplayName;
    }

    private static String resolveFriendlyBaseName(String baseApplicationName) {
        String normalizedBaseApplicationName = ApplicationNameNormalizer.normalize(baseApplicationName);
        if (StringUtils.isBlank(normalizedBaseApplicationName)) {
            return baseApplicationName;
        }

        String knownDisplayName = KNOWN_DISPLAY_NAMES.get(normalizedBaseApplicationName.toLowerCase(Locale.ROOT));
        if (StringUtils.isNotBlank(knownDisplayName)) {
            return knownDisplayName;
        }

        String executablePath = ApplicationIconService.getInstance().findExecutablePath(normalizedBaseApplicationName);
        if (StringUtils.isNotBlank(executablePath)) {
            String fileDescription = ApplicationTitleResolver.resolveDisplayTitle(
                    normalizedBaseApplicationName,
                    executablePath
            );
            if (StringUtils.isNotBlank(fileDescription)
                    && !equalsIgnoreCaseNormalized(fileDescription, normalizedBaseApplicationName)) {
                return cleanupFileDescription(fileDescription);
            }
        }

        return normalizedBaseApplicationName;
    }

    private static String cleanupFileDescription(String fileDescription) {
        String cleanedFileDescription = fileDescription.trim();
        // Убираем типичные хвосты VERSIONINFO вроде " - Java Application".
        int dashIndex = cleanedFileDescription.indexOf(" - ");
        if (dashIndex > 3) {
            String leftPart = cleanedFileDescription.substring(0, dashIndex).trim();
            if (leftPart.length() >= 3) {
                return leftPart;
            }
        }
        return cleanedFileDescription;
    }

    private static boolean equalsIgnoreCaseNormalized(String leftValue, String rightValue) {
        return ApplicationNameNormalizer.normalize(leftValue)
                .equalsIgnoreCase(ApplicationNameNormalizer.normalize(rightValue));
    }

    private static boolean isOthersCategory(String applicationName) {
        return Objects.equals(applicationName, Messages.get(MessageCodes.UI_STATS_OTHERS))
                || Objects.equals(applicationName, "Others")
                || Objects.equals(applicationName, "Інші");
    }
}
