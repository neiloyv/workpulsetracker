package com.workpulsetracker.agent.pomodoro;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Логика Pomodoro: интервалы, фазы, счётчик помидоров. Не связан с трекингом окон.
 */
public final class PomodoroEngine {

    public static final int DEFAULT_WORK_MINUTES = 25;
    public static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
    public static final int DEFAULT_LONG_BREAK_MINUTES = 15;
    public static final int DEFAULT_SESSIONS_UNTIL_LONG_BREAK = 4;

    private final CopyOnWriteArrayList<Runnable> stateChangeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<PomodoroPhase>> phaseCompletedListeners = new CopyOnWriteArrayList<>();

    private boolean featureEnabled;
    private boolean timerRunning;
    private PomodoroPhase currentPhase = PomodoroPhase.WORK;
    private int remainingSeconds = DEFAULT_WORK_MINUTES * 60;
    private int workMinutes = DEFAULT_WORK_MINUTES;
    private int shortBreakMinutes = DEFAULT_SHORT_BREAK_MINUTES;
    private int longBreakMinutes = DEFAULT_LONG_BREAK_MINUTES;
    private int sessionsUntilLongBreak = DEFAULT_SESSIONS_UNTIL_LONG_BREAK;
    private int completedWorkSessionsInCycle;
    private int completedPomodorosToday;

    public void addStateChangeListener(Runnable stateChangeListener) {
        if (Objects.nonNull(stateChangeListener)) {
            stateChangeListeners.add(stateChangeListener);
        }
    }

    public void addPhaseCompletedListener(Consumer<PomodoroPhase> phaseCompletedListener) {
        if (Objects.nonNull(phaseCompletedListener)) {
            phaseCompletedListeners.add(phaseCompletedListener);
        }
    }

    public void setFeatureEnabled(boolean featureEnabled) {
        this.featureEnabled = featureEnabled;
        if (!featureEnabled) {
            stopTimer();
            resetToWorkPhase();
        }
        notifyStateChanged();
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public PomodoroPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getCompletedPomodorosToday() {
        return completedPomodorosToday;
    }

    public int getWorkMinutes() {
        return workMinutes;
    }

    public int getShortBreakMinutes() {
        return shortBreakMinutes;
    }

    public int getLongBreakMinutes() {
        return longBreakMinutes;
    }

    public int getSessionsUntilLongBreak() {
        return sessionsUntilLongBreak;
    }

    public void updateDurations(
            int workMinutes,
            int shortBreakMinutes,
            int longBreakMinutes,
            int sessionsUntilLongBreak
    ) {
        this.workMinutes = clampMinutes(workMinutes);
        this.shortBreakMinutes = clampMinutes(shortBreakMinutes);
        this.longBreakMinutes = clampMinutes(longBreakMinutes);
        this.sessionsUntilLongBreak = Math.max(1, Math.min(sessionsUntilLongBreak, 12));
        if (!timerRunning) {
            remainingSeconds = durationSecondsForPhase(currentPhase);
        }
        notifyStateChanged();
    }

    public void startOrResume() {
        if (!featureEnabled) {
            return;
        }
        if (remainingSeconds <= 0) {
            remainingSeconds = durationSecondsForPhase(currentPhase);
        }
        timerRunning = true;
        notifyStateChanged();
    }

    public void pause() {
        if (!timerRunning) {
            return;
        }
        timerRunning = false;
        notifyStateChanged();
    }

    public void skip() {
        if (!featureEnabled) {
            return;
        }
        advancePhase(false);
    }

    /**
     * Тик раз в секунду из Swing Timer.
     */
    public void onTick() {
        if (!featureEnabled || !timerRunning) {
            return;
        }
        if (remainingSeconds <= 0) {
            advancePhase(true);
            return;
        }
        remainingSeconds--;
        if (remainingSeconds <= 0) {
            advancePhase(true);
            return;
        }
        notifyStateChanged();
    }

    private void advancePhase(boolean phaseCompletedNaturally) {
        boolean wasRunning = timerRunning;
        PomodoroPhase completedPhase = currentPhase;
        if (phaseCompletedNaturally) {
            phaseCompletedListeners.forEach(listener -> listener.accept(completedPhase));
        }

        if (completedPhase == PomodoroPhase.WORK) {
            if (phaseCompletedNaturally) {
                completedPomodorosToday++;
                completedWorkSessionsInCycle++;
                if (completedWorkSessionsInCycle >= sessionsUntilLongBreak) {
                    currentPhase = PomodoroPhase.LONG_BREAK;
                    completedWorkSessionsInCycle = 0;
                } else {
                    currentPhase = PomodoroPhase.SHORT_BREAK;
                }
            } else {
                currentPhase = PomodoroPhase.SHORT_BREAK;
            }
        } else {
            currentPhase = PomodoroPhase.WORK;
        }

        remainingSeconds = durationSecondsForPhase(currentPhase);
        timerRunning = featureEnabled && (phaseCompletedNaturally || wasRunning);
        notifyStateChanged();
    }

    private void stopTimer() {
        timerRunning = false;
    }

    private void resetToWorkPhase() {
        currentPhase = PomodoroPhase.WORK;
        remainingSeconds = durationSecondsForPhase(PomodoroPhase.WORK);
        completedWorkSessionsInCycle = 0;
    }

    private int durationSecondsForPhase(PomodoroPhase pomodoroPhase) {
        return switch (pomodoroPhase) {
            case WORK -> workMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            case LONG_BREAK -> longBreakMinutes * 60;
        };
    }

    private static int clampMinutes(int minutes) {
        return Math.max(1, Math.min(minutes, 180));
    }

    private void notifyStateChanged() {
        stateChangeListeners.forEach(Runnable::run);
    }
}
