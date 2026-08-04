package com.workpulsetracker.common.i18n;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Поддерживаемые языки UI / пользовательских сообщений.
 * Код языка — ISO 639-1 ({@code en}, {@code uk}).
 */
public enum AppLanguage {

    ENGLISH("en", Locale.ENGLISH),
    UKRAINIAN("uk", Locale.forLanguageTag("uk"));

    private final String code;
    private final Locale locale;

    AppLanguage(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }

    public String getCode() {
        return code;
    }

    public Locale toLocale() {
        return locale;
    }

    public static AppLanguage getDefault() {
        return ENGLISH;
    }

    /**
     * Язык по локали ОС/JVM: {@code uk} → украинский, иначе английский.
     */
    public static AppLanguage fromSystemLocale() {
        String systemLanguage = Locale.getDefault().getLanguage();
        if (Objects.equals("uk", systemLanguage.toLowerCase(Locale.ROOT))) {
            return UKRAINIAN;
        }
        return ENGLISH;
    }

    /**
     * Разбирает код языка из настроек ({@code en}, {@code uk}).
     * Пустое значение или {@code auto} → автодетект по локали ОС.
     * Неизвестный код → язык по умолчанию (английский).
     */
    public static AppLanguage fromCode(String languageCode) {
        if (StringUtils.isBlank(languageCode) || "auto".equalsIgnoreCase(languageCode.trim())) {
            return fromSystemLocale();
        }
        String normalizedLanguageCode = languageCode.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(appLanguage -> Objects.equals(appLanguage.code, normalizedLanguageCode))
                .findFirst()
                .orElseGet(AppLanguage::getDefault);
    }

    public static Optional<AppLanguage> findByCode(String languageCode) {
        if (StringUtils.isBlank(languageCode)) {
            return Optional.empty();
        }
        String normalizedLanguageCode = languageCode.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(appLanguage -> Objects.equals(appLanguage.code, normalizedLanguageCode))
                .findFirst();
    }
}
