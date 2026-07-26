package com.workpulsetracker.agent.buffer;

/**
 * Слушатель изменений буфера интервалов.
 */
public interface ActivityBufferListener {

    void onIntervalClosed(ActivityInterval activityInterval);

    default void onIntervalOpened(ActivityInterval activityInterval) {
        // optional
    }
}
