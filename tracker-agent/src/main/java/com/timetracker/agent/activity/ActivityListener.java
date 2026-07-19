package com.timetracker.agent.activity;

/**
 * Слушатель событий пользовательской активности (клавиатура / мышь).
 */
@FunctionalInterface
public interface ActivityListener {

    void onUserActivity();
}
