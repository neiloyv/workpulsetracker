package com.workpulsetracker.agent.idle;

/**
 * Уведомление об изменении статуса IDLE / ACTIVE.
 */
@FunctionalInterface
public interface IdleStatusListener {

    void onStatusChanged(TrackerStatus previousStatus, TrackerStatus currentStatus);
}
