package com.workpulsetracker.agent.stats;

/**
 * Состояние сегмента суточного таймлайна (PC Off не хранится — это пробелы между сегментами).
 */
public enum DayActivityState {
    ACTIVE,
    IDLE,
    /**
     * Приложение с Track OFF: человек за ПК, но время не считается обычной работой.
     */
    EXCLUDED
}
