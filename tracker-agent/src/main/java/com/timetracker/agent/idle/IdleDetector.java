package com.timetracker.agent.idle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Периодически проверяет время последнего действия пользователя
 * и переключает статус ACTIVE / IDLE.
 */
public final class IdleDetector implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(IdleDetector.class);

    private final long idleTimeoutSeconds;
    private final long checkIntervalSeconds;
    private final AtomicReference<Instant> lastActivityInstant;
    private final AtomicReference<TrackerStatus> currentStatus;
    private final List<IdleStatusListener> idleStatusListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduledExecutorService;

    public IdleDetector(long idleTimeoutSeconds, long checkIntervalSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.lastActivityInstant = new AtomicReference<>(Instant.now());
        this.currentStatus = new AtomicReference<>(TrackerStatus.ACTIVE);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "idle-detector");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(
                this::checkIdleState,
                checkIntervalSeconds,
                checkIntervalSeconds,
                TimeUnit.SECONDS
        );
        logger.info(
                "IdleDetector started: timeout={}s, checkInterval={}s",
                idleTimeoutSeconds,
                checkIntervalSeconds
        );
    }

    public void onUserActivity() {
        lastActivityInstant.set(Instant.now());
        TrackerStatus previousStatus = currentStatus.get();
        if (previousStatus == TrackerStatus.IDLE) {
            changeStatus(TrackerStatus.ACTIVE);
            logger.info("Activity restored");
        }
    }

    public TrackerStatus getCurrentStatus() {
        return currentStatus.get();
    }

    public Instant getLastActivityInstant() {
        return lastActivityInstant.get();
    }

    public void addListener(IdleStatusListener idleStatusListener) {
        if (Objects.nonNull(idleStatusListener)) {
            idleStatusListeners.add(idleStatusListener);
        }
    }

    public void removeListener(IdleStatusListener idleStatusListener) {
        if (Objects.nonNull(idleStatusListener)) {
            idleStatusListeners.remove(idleStatusListener);
        }
    }

    private void checkIdleState() {
        try {
            Instant lastActivity = lastActivityInstant.get();
            long idleSeconds = Instant.now().getEpochSecond() - lastActivity.getEpochSecond();
            if (idleSeconds >= idleTimeoutSeconds && currentStatus.get() == TrackerStatus.ACTIVE) {
                changeStatus(TrackerStatus.IDLE);
                logger.info(
                        "User went IDLE (no activity for {}s, threshold {}s)",
                        idleSeconds,
                        idleTimeoutSeconds
                );
            }
        } catch (Exception exception) {
            logger.error("IdleDetector error: {}", exception.getMessage(), exception);
        }
    }

    private void changeStatus(TrackerStatus newStatus) {
        TrackerStatus previousStatus = currentStatus.getAndSet(newStatus);
        if (previousStatus == newStatus) {
            return;
        }
        idleStatusListeners.forEach(listener -> listener.onStatusChanged(previousStatus, newStatus));
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdownNow();
        logger.info("IdleDetector stopped");
    }
}
