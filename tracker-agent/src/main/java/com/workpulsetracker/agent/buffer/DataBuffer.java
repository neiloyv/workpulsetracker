package com.workpulsetracker.agent.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Потокобезопасный in-memory буфер текущего открытого интервала активности.
 * Закрытые интервалы не хранятся здесь — они уходят слушателям (persist в {@code ActivityStore}).
 */
public final class DataBuffer {

    private static final Logger logger = LoggerFactory.getLogger(DataBuffer.class);

    private static final DataBuffer INSTANCE = new DataBuffer();

    private final ReentrantLock reentrantLock = new ReentrantLock();
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

    public void startInterval(
            String applicationName,
            String windowTitle,
            boolean idle,
            String appIdentifier,
            String displayTitle
    ) {
        reentrantLock.lock();
        try {
            closeCurrentIntervalLocked(Instant.now());
            currentActivityInterval = new ActivityInterval(
                    Instant.now(),
                    null,
                    applicationName,
                    windowTitle,
                    idle,
                    appIdentifier,
                    displayTitle
            );
            ActivityInterval openedActivityInterval = currentActivityInterval;
            logger.debug(
                    "Opened interval: app={}, displayTitle={}, title={}, idle={}, appIdentifier={}",
                    applicationName,
                    openedActivityInterval.getDisplayTitle(),
                    windowTitle,
                    idle,
                    openedActivityInterval.getAppIdentifier()
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

    public ActivityInterval getCurrentInterval() {
        reentrantLock.lock();
        try {
            return currentActivityInterval;
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
        currentActivityInterval = null;
        logger.debug("Closed interval: {}", closedActivityInterval);
        activityBufferListeners.forEach(listener -> listener.onIntervalClosed(closedActivityInterval));
    }
}
