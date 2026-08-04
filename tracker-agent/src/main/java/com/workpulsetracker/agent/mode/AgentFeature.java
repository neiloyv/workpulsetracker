package com.workpulsetracker.agent.mode;

/**
 * Возможности агента, которые могут зависеть от {@link AgentOperationMode}.
 */
public enum AgentFeature {

    /**
     * Отправка телеметрии и reverse sync с облаком.
     */
    SYNC_TO_CLOUD,

    /**
     * Агрегация активности между устройствами одного аккаунта.
     */
    MULTI_DEVICE_AGGREGATION,

    /**
     * Расширенная история (год / всё время) и связанные длинные экспорты.
     */
    EXTENDED_HISTORY_EXPORT,

    /**
     * Продвинутая аналитика (матрица и т.п. поверх базовых графиков).
     */
    ADVANCED_ANALYTICS,

    /**
     * Дневной таймлайн активности — доступен в Free Solo.
     */
    DAILY_TIMELINE,

    /**
     * Базовая локальная статистика (день / неделя / месяц) — доступна в Free Solo.
     */
    BASIC_LOCAL_STATS
}
