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
    private Boolean timelineVisible = true;
    private Boolean minimizeToTray = true;
    private String lastReportDirectoryPath;
    private String lastBackupDirectoryPath;
    private boolean pomodoroEnabled;
    private Integer pomodoroWorkMinutes = 25;
    private Integer pomodoroShortBreakMinutes = 5;
    private Integer pomodoroLongBreakMinutes = 15;
    private Integer pomodoroSessionsUntilLongBreak = 4;

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

    public boolean isTimelineVisible() {
        return Objects.isNull(timelineVisible) || timelineVisible;
    }

    public void setTimelineVisible(boolean timelineVisible) {
        this.timelineVisible = timelineVisible;
    }

    public boolean isMinimizeToTray() {
        return Objects.isNull(minimizeToTray) || minimizeToTray;
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    public String getLastReportDirectoryPath() {
        return lastReportDirectoryPath;
    }

    public void setLastReportDirectoryPath(String lastReportDirectoryPath) {
        this.lastReportDirectoryPath = StringUtils.isNotBlank(lastReportDirectoryPath)
                ? lastReportDirectoryPath.trim()
                : null;
    }

    public String getLastBackupDirectoryPath() {
        return lastBackupDirectoryPath;
    }

    public void setLastBackupDirectoryPath(String lastBackupDirectoryPath) {
        this.lastBackupDirectoryPath = StringUtils.isNotBlank(lastBackupDirectoryPath)
                ? lastBackupDirectoryPath.trim()
                : null;
    }

    public boolean isPomodoroEnabled() {
        return pomodoroEnabled;
    }

    public void setPomodoroEnabled(boolean pomodoroEnabled) {
        this.pomodoroEnabled = pomodoroEnabled;
    }

    public int getPomodoroWorkMinutes() {
        return normalizePomodoroMinutes(pomodoroWorkMinutes, 25);
    }

    public void setPomodoroWorkMinutes(int pomodoroWorkMinutes) {
        this.pomodoroWorkMinutes = normalizePomodoroMinutes(pomodoroWorkMinutes, 25);
    }

    public int getPomodoroShortBreakMinutes() {
        return normalizePomodoroMinutes(pomodoroShortBreakMinutes, 5);
    }

    public void setPomodoroShortBreakMinutes(int pomodoroShortBreakMinutes) {
        this.pomodoroShortBreakMinutes = normalizePomodoroMinutes(pomodoroShortBreakMinutes, 5);
    }

    public int getPomodoroLongBreakMinutes() {
        return normalizePomodoroMinutes(pomodoroLongBreakMinutes, 15);
    }

    public void setPomodoroLongBreakMinutes(int pomodoroLongBreakMinutes) {
        this.pomodoroLongBreakMinutes = normalizePomodoroMinutes(pomodoroLongBreakMinutes, 15);
    }

    public int getPomodoroSessionsUntilLongBreak() {
        if (Objects.isNull(pomodoroSessionsUntilLongBreak) || pomodoroSessionsUntilLongBreak < 1) {
            return 4;
        }
        return Math.min(pomodoroSessionsUntilLongBreak, 12);
    }

    public void setPomodoroSessionsUntilLongBreak(int pomodoroSessionsUntilLongBreak) {
        this.pomodoroSessionsUntilLongBreak = Math.max(1, Math.min(pomodoroSessionsUntilLongBreak, 12));
    }

    private static int normalizePomodoroMinutes(Integer minutes, int defaultMinutes) {
        if (Objects.isNull(minutes) || minutes < 1) {
            return defaultMinutes;
        }
        return Math.min(minutes, 180);
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

    /**
     * Копирует все поля из другого экземпляра (после импорта бэкапа).
     */
    public void copyFrom(UserSettings sourceUserSettings) {
        Objects.requireNonNull(sourceUserSettings);
        setLanguageCode(sourceUserSettings.getLanguageCode());
        setEmail(sourceUserSettings.getEmail());
        setActivationKey(sourceUserSettings.getActivationKey());
        setLocalOnly(sourceUserSettings.isLocalOnly());
        setSetupCompleted(sourceUserSettings.isSetupCompleted());
        setAutoStartTracking(sourceUserSettings.isAutoStartTracking());
        setMinorUsageThresholdMinutes(sourceUserSettings.getMinorUsageThresholdMinutes());
        setTimelineVisible(sourceUserSettings.isTimelineVisible());
        setMinimizeToTray(sourceUserSettings.isMinimizeToTray());
        setLastReportDirectoryPath(sourceUserSettings.getLastReportDirectoryPath());
        setLastBackupDirectoryPath(sourceUserSettings.getLastBackupDirectoryPath());
        setPomodoroEnabled(sourceUserSettings.isPomodoroEnabled());
        setPomodoroWorkMinutes(sourceUserSettings.getPomodoroWorkMinutes());
        setPomodoroShortBreakMinutes(sourceUserSettings.getPomodoroShortBreakMinutes());
        setPomodoroLongBreakMinutes(sourceUserSettings.getPomodoroLongBreakMinutes());
        setPomodoroSessionsUntilLongBreak(sourceUserSettings.getPomodoroSessionsUntilLongBreak());
    }
}
