package com.workpulsetracker.agent.api;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.storage.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Клиент облачной синхронизации интервалов с tracker-server.
 * Пока только заготовки — реализация будет позже.
 */
public final class AgentSyncClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentSyncClient.class);

    /**
     * @return {@code true}, если для настроек теоретически доступна синхронизация
     */
    public boolean isSyncConfigured(UserSettings userSettings) {
        return Objects.nonNull(userSettings) && userSettings.isServerSyncEnabled();
    }

    /**
     * Загружает закрытые интервалы на сервер.
     *
     * @param userSettings      настройки с email/access key
     * @param activityIntervals интервалы для отправки
     */
    public void uploadIntervals(UserSettings userSettings, List<ActivityInterval> activityIntervals) {
        Objects.requireNonNull(userSettings);
        List<ActivityInterval> safeIntervals = Objects.isNull(activityIntervals)
                ? Collections.emptyList()
                : List.copyOf(activityIntervals);
        // TODO: реализовать POST пачки интервалов на tracker-server (идемпотентно по interval id / start+end+app)
        logger.debug(
                "uploadIntervals stub called: syncConfigured={}, intervals={}",
                isSyncConfigured(userSettings),
                safeIntervals.size()
        );
        throw new UnsupportedOperationException("Cloud sync upload is not implemented yet");
    }

    /**
     * Забирает интервалы с сервера и возвращает их для слияния с локальным store.
     */
    public List<ActivityInterval> downloadIntervals(UserSettings userSettings) {
        Objects.requireNonNull(userSettings);
        // TODO: реализовать GET интервалов пользователя с tracker-server
        logger.debug(
                "downloadIntervals stub called: syncConfigured={}",
                isSyncConfigured(userSettings)
        );
        throw new UnsupportedOperationException("Cloud sync download is not implemented yet");
    }

    /**
     * Полный цикл sync: upload локальных + merge remote.
     */
    public void synchronize(UserSettings userSettings, List<ActivityInterval> localActivityIntervals) {
        Objects.requireNonNull(userSettings);
        // TODO: оркестрация upload + download + merge конфликтов (append-only / last-write-wins)
        logger.debug(
                "synchronize stub called: syncConfigured={}, localIntervals={}",
                isSyncConfigured(userSettings),
                Objects.isNull(localActivityIntervals) ? 0 : localActivityIntervals.size()
        );
        throw new UnsupportedOperationException("Cloud sync is not implemented yet");
    }
}
