package com.workpulsetracker.agent.storage;

import com.workpulsetracker.common.i18n.AppLanguage;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Локальные настройки пользователя агента (язык, привязка к веб-аккаунту).
 */
public final class UserSettings {

    private String languageCode = AppLanguage.getDefault().getCode();
    private String activationKey;
    private boolean localOnly = true;
    private boolean setupCompleted;

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = StringUtils.isNotBlank(languageCode)
                ? languageCode.trim()
                : AppLanguage.getDefault().getCode();
    }

    public AppLanguage getLanguage() {
        return AppLanguage.fromCode(languageCode);
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = StringUtils.isNotBlank(activationKey) ? activationKey.trim() : null;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    /**
     * Можно слать данные на сервер только если есть ключ и режим не «только локально».
     */
    public boolean isServerSyncEnabled() {
        return !localOnly && StringUtils.isNotBlank(activationKey);
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }

    public void setSetupCompleted(boolean setupCompleted) {
        this.setupCompleted = setupCompleted;
    }

    public void applyLocalOnlyMode() {
        this.localOnly = true;
        this.activationKey = null;
        this.setupCompleted = true;
    }

    public void applyActivationKey(String activationKey) {
        this.activationKey = Objects.requireNonNull(activationKey).trim();
        this.localOnly = false;
        this.setupCompleted = true;
    }
}
