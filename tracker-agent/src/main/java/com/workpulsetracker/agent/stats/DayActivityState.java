package com.workpulsetracker.agent.stats;

/**
 * Состояние сегмента суточного таймлайна (PC Off не хранится — это пробелы между сегментами).
 */
public enum DayActivityState {
    ACTIVE,
    IDLE
}
