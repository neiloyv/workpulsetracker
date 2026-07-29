package com.workpulsetracker.agent.storage;

import com.workpulsetracker.common.i18n.AppLanguage;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Локальные настройки пользователя агента (язык, привязка к веб-аккаунту).
 */
public final class UserSettings {

    private String languageCode = AppLanguage.getDefault().getCode();
    private String email;
    private String activationKey;
    private boolean localOnly = true;
    private boolean setupCompleted;
    private boolean autoStartTracking;
    private Integer minorUsageThresholdMinutes = 5;
    private String lastReportDirectoryPath;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = StringUtils.isNotBlank(email) ? email.trim().toLowerCase() : null;
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
     * Можно слать данные на сервер только если есть email, ключ и режим не «только локально».
     */
    public boolean isServerSyncEnabled() {
        return !localOnly
                && StringUtils.isNotBlank(email)
                && StringUtils.isNotBlank(activationKey);
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }

    public void setSetupCompleted(boolean setupCompleted) {
        this.setupCompleted = setupCompleted;
    }

    public boolean isAutoStartTracking() {
        return autoStartTracking;
    }

    public void setAutoStartTracking(boolean autoStartTracking) {
        this.autoStartTracking = autoStartTracking;
    }

    public int getMinorUsageThresholdMinutes() {
        if (Objects.isNull(minorUsageThresholdMinutes) || minorUsageThresholdMinutes < 0) {
            return 5;
        }
        return minorUsageThresholdMinutes;
    }

    public void setMinorUsageThresholdMinutes(int minorUsageThresholdMinutes) {
        this.minorUsageThresholdMinutes = Math.max(minorUsageThresholdMinutes, 0);
    }

    public String getLastReportDirectoryPath() {
        return lastReportDirectoryPath;
    }

    public void setLastReportDirectoryPath(String lastReportDirectoryPath) {
        this.lastReportDirectoryPath = StringUtils.isNotBlank(lastReportDirectoryPath)
                ? lastReportDirectoryPath.trim()
                : null;
    }

    public void applyLocalOnlyMode() {
        this.localOnly = true;
        this.email = null;
        this.activationKey = null;
        this.setupCompleted = true;
    }

    /**
     * Первичная активация: сохраняет email и access key, остальные настройки не трогает.
     */
    public void applyCredentials(String email, String accessKey) {
        this.email = Objects.requireNonNull(email).trim().toLowerCase();
        this.activationKey = Objects.requireNonNull(accessKey).trim();
        this.localOnly = false;
        this.setupCompleted = true;
    }

    /**
     * Обновляет только access key, не затрагивая email и остальные настройки.
     */
    public void updateAccessKey(String accessKey) {
        this.activationKey = Objects.requireNonNull(accessKey).trim();
        this.localOnly = false;
    }
}
