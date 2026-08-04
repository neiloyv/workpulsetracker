package com.workpulsetracker.agent.storage;

import com.workpulsetracker.agent.mode.AgentOperationMode;
import com.workpulsetracker.common.i18n.AppLanguage;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Локальные настройки пользователя агента (язык, режим работы, привязка к веб-аккаунту).
 */
public final class UserSettings {

    private String languageCode = AppLanguage.getDefault().getCode();
    private String email;
    private String activationKey;
    private String accessToken;
    private String hardwareId;
    private Long deviceId;
    private Long workerId;
    /**
     * Явный режим: {@link AgentOperationMode#LOCAL_SOLO} / {@link AgentOperationMode#NETWORK_SYNC}.
     * При чтении старых settings.json может быть null — тогда берём legacy {@link #localOnly}.
     */
    private String operationMode = AgentOperationMode.LOCAL_SOLO.name();
    /**
     * Legacy-флаг для обратной совместимости со старыми settings.json.
     */
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

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = StringUtils.isNotBlank(accessToken) ? accessToken.trim() : null;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = StringUtils.isNotBlank(hardwareId) ? hardwareId.trim() : null;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public AgentOperationMode getOperationMode() {
        if (StringUtils.isNotBlank(operationMode)) {
            return AgentOperationMode.fromCode(operationMode);
        }
        return localOnly ? AgentOperationMode.LOCAL_SOLO : AgentOperationMode.NETWORK_SYNC;
    }

    public void setOperationMode(AgentOperationMode agentOperationMode) {
        AgentOperationMode resolvedOperationMode = Objects.nonNull(agentOperationMode)
                ? agentOperationMode
                : AgentOperationMode.getDefault();
        this.operationMode = resolvedOperationMode.name();
        this.localOnly = resolvedOperationMode.isLocalSolo();
    }

    public boolean isLocalOnly() {
        return getOperationMode().isLocalSolo();
    }

    public void setLocalOnly(boolean localOnly) {
        setOperationMode(localOnly ? AgentOperationMode.LOCAL_SOLO : AgentOperationMode.NETWORK_SYNC);
    }

    /**
     * Можно слать данные на сервер только в NETWORK_SYNC при наличии email и access key.
     */
    public boolean isServerSyncEnabled() {
        return getOperationMode().isNetworkSync()
                && StringUtils.isNotBlank(email)
                && StringUtils.isNotBlank(activationKey);
    }

    public boolean hasValidAccessToken() {
        return StringUtils.isNotBlank(accessToken);
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
        setOperationMode(AgentOperationMode.LOCAL_SOLO);
        this.email = null;
        this.activationKey = null;
        this.accessToken = null;
        this.hardwareId = null;
        this.deviceId = null;
        this.workerId = null;
        this.setupCompleted = true;
    }

    /**
     * Первичная активация / подключение облака: email + access key → NETWORK_SYNC.
     */
    public void applyCredentials(String email, String accessKey) {
        this.email = Objects.requireNonNull(email).trim().toLowerCase();
        this.activationKey = Objects.requireNonNull(accessKey).trim();
        setOperationMode(AgentOperationMode.NETWORK_SYNC);
        this.setupCompleted = true;
    }

    public void applyAgentAuth(
            String accessToken,
            String hardwareId,
            Long workerId,
            Long deviceId
    ) {
        this.accessToken = Objects.requireNonNull(accessToken).trim();
        this.hardwareId = Objects.requireNonNull(hardwareId).trim();
        this.workerId = workerId;
        this.deviceId = deviceId;
        setOperationMode(AgentOperationMode.NETWORK_SYNC);
    }

    /**
     * Обновляет только access key, не затрагивая email и остальные настройки.
     */
    public void updateAccessKey(String accessKey) {
        this.activationKey = Objects.requireNonNull(accessKey).trim();
        setOperationMode(AgentOperationMode.NETWORK_SYNC);
        this.accessToken = null;
    }

    /**
     * Копирует все поля из другого экземпляра (после импорта бэкапа).
     */
    public void copyFrom(UserSettings sourceUserSettings) {
        Objects.requireNonNull(sourceUserSettings);
        setLanguageCode(sourceUserSettings.getLanguageCode());
        setEmail(sourceUserSettings.getEmail());
        setActivationKey(sourceUserSettings.getActivationKey());
        setAccessToken(sourceUserSettings.getAccessToken());
        setHardwareId(sourceUserSettings.getHardwareId());
        setDeviceId(sourceUserSettings.getDeviceId());
        setWorkerId(sourceUserSettings.getWorkerId());
        setOperationMode(sourceUserSettings.getOperationMode());
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
