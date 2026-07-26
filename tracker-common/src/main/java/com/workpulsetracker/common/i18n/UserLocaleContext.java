package com.workpulsetracker.common.i18n;

import java.util.Objects;

/**
 * Текущий язык пользователя для пользовательских сообщений.
 * Позже сюда будет писать окно настроек приложения; сейчас — из конфига при старте.
 */
public final class UserLocaleContext {

    private static volatile AppLanguage currentLanguage = AppLanguage.getDefault();

    private UserLocaleContext() {
    }

    public static void setLanguage(AppLanguage appLanguage) {
        if (Objects.nonNull(appLanguage)) {
            currentLanguage = appLanguage;
        }
    }

    public static AppLanguage getLanguage() {
        return currentLanguage;
    }

    public static void resetToDefault() {
        currentLanguage = AppLanguage.getDefault();
    }
}
