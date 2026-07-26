package com.workpulsetracker.agent.activity;

/**
 * Контракт монитора глобальной активности пользователя.
 */
public interface ActivityMonitor extends AutoCloseable {

    void start();

    void addListener(ActivityListener activityListener);

    void removeListener(ActivityListener activityListener);

    @Override
    void close();
}
