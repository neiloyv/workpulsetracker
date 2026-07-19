package com.timetracker.agent.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Потокобезопасный in-memory буфер интервалов активности текущей сессии.
 */
public final class DataBuffer {

    private static final Logger logger = LoggerFactory.getLogger(DataBuffer.class);

    private static final DataBuffer INSTANCE = new DataBuffer();

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final List<ActivityInterval> closedActivityIntervals = new ArrayList<>();
    private final List<ActivityBufferListener> activityBufferListeners = new CopyOnWriteArrayList<>();
    private ActivityInterval currentActivityInterval;

    private DataBuffer() {
    }

    public static DataBuffer getInstance() {
        return INSTANCE;
    }

    public void addListener(ActivityBufferListener activityBufferListener) {
        if (Objects.nonNull(activityBufferListener)) {
            activityBufferListeners.add(activityBufferListener);
        }
    }

    public void startInterval(String applicationName, String windowTitle, boolean idle) {
        reentrantLock.lock();
        try {
            closeCurrentIntervalLocked(Instant.now());
            currentActivityInterval = new ActivityInterval(
                    Instant.now(),
                    null,
                    applicationName,
                    windowTitle,
                    idle
            );
            ActivityInterval openedActivityInterval = currentActivityInterval;
            logger.debug(
                    "Открыт интервал: app={}, title={}, idle={}",
                    applicationName,
                    windowTitle,
                    idle
            );
            activityBufferListeners.forEach(listener -> listener.onIntervalOpened(openedActivityInterval));
        } finally {
            reentrantLock.unlock();
        }
    }

    public void closeCurrentInterval() {
        reentrantLock.lock();
        try {
            closeCurrentIntervalLocked(Instant.now());
        } finally {
            reentrantLock.unlock();
        }
    }

    public List<ActivityInterval> getClosedIntervals() {
        reentrantLock.lock();
        try {
            return Collections.unmodifiableList(
                    closedActivityIntervals.stream().collect(Collectors.toList())
            );
        } finally {
            reentrantLock.unlock();
        }
    }

    public List<ActivityInterval> getAllIntervalsSnapshot() {
        reentrantLock.lock();
        try {
            List<ActivityInterval> activityIntervals = closedActivityIntervals.stream()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (Objects.nonNull(currentActivityInterval)) {
                activityIntervals.add(currentActivityInterval);
            }
            return Collections.unmodifiableList(activityIntervals);
        } finally {
            reentrantLock.unlock();
        }
    }

    public ActivityInterval getCurrentInterval() {
        reentrantLock.lock();
        try {
            return currentActivityInterval;
        } finally {
            reentrantLock.unlock();
        }
    }

    public int getClosedIntervalCount() {
        reentrantLock.lock();
        try {
            return closedActivityIntervals.size();
        } finally {
            reentrantLock.unlock();
        }
    }

    private void closeCurrentIntervalLocked(Instant endInstant) {
        if (Objects.isNull(currentActivityInterval)) {
            return;
        }
        currentActivityInterval.setEndInstant(endInstant);
        ActivityInterval closedActivityInterval = currentActivityInterval;
        closedActivityIntervals.add(closedActivityInterval);
        currentActivityInterval = null;
        logger.debug("Закрыт интервал: {}", closedActivityInterval);
        activityBufferListeners.forEach(listener -> listener.onIntervalClosed(closedActivityInterval));
    }
}
