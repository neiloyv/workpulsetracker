package com.timetracker.common.i18n;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Резолвер пользовательских строк по текущему языку из {@link UserLocaleContext}.
 * <p>
 * Бандлы: {@code i18n/messages_en.properties}, {@code i18n/messages_uk.properties},
 * корневой {@code i18n/messages.properties} — английский fallback.
 * <p>
 * Важно: в эти файлы кладём только то, что может увидеть пользователь в UI.
 * Технические логи разработчика сюда не переводим.
 */
public final class Messages {

    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    private Messages() {
    }

    public static String get(String messageCode, Object... arguments) {
        return getForLanguage(UserLocaleContext.getLanguage(), messageCode, arguments);
    }

    /**
     * Сообщение для конкретного языка (например, язык пользователя из БД/запроса на сервере).
     * Не меняет глобальный {@link UserLocaleContext}.
     */
    public static String getForLanguage(AppLanguage appLanguage, String messageCode, Object... arguments) {
        if (StringUtils.isBlank(messageCode)) {
            return "";
        }

        AppLanguage resolvedLanguage = Objects.nonNull(appLanguage) ? appLanguage : AppLanguage.getDefault();
        Locale locale = resolvedLanguage.toLocale();

        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle(
                    BUNDLE_BASE_NAME,
                    locale,
                    Messages.class.getClassLoader(),
                    Utf8ResourceBundleControl.INSTANCE
            );
            String pattern = resourceBundle.getString(messageCode);
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return pattern;
            }
            return MessageFormat.format(pattern, arguments);
        } catch (MissingResourceException exception) {
            return messageCode;
        }
    }

    /**
     * Загрузка .properties в UTF-8, чтобы украинский текст в файлах читался нормально.
     */
    private static final class Utf8ResourceBundleControl extends ResourceBundle.Control {

        private static final Utf8ResourceBundleControl INSTANCE = new Utf8ResourceBundleControl();

        @Override
        public ResourceBundle newBundle(
                String baseName,
                Locale locale,
                String format,
                ClassLoader classLoader,
                boolean reload
        ) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
                if (Objects.isNull(inputStream)) {
                    return null;
                }
                try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    }
}
