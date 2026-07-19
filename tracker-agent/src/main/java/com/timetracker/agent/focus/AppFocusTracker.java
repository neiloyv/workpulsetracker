package com.timetracker.agent.focus;

import com.timetracker.agent.idle.TrackerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Периодически опрашивает ОС и сообщает о смене активного окна.
 */
public final class AppFocusTracker implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AppFocusTracker.class);

    private final NativeOSService nativeOSService;
    private final long pollIntervalSeconds;
    private final Supplier<TrackerStatus> trackerStatusSupplier;
    private final FocusChangeListener focusChangeListener;
    private final AtomicReference<WindowInfo> currentWindowInfo = new AtomicReference<>();
    private final ScheduledExecutorService scheduledExecutorService;

    public AppFocusTracker(
            NativeOSService nativeOSService,
            long pollIntervalSeconds,
            Supplier<TrackerStatus> trackerStatusSupplier,
            FocusChangeListener focusChangeListener
    ) {
        this.nativeOSService = nativeOSService;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.trackerStatusSupplier = trackerStatusSupplier;
        this.focusChangeListener = focusChangeListener;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "app-focus-tracker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        WindowInfo initialWindowInfo = nativeOSService.getActiveWindowInfo();
        currentWindowInfo.set(initialWindowInfo);
        logActiveWindow(initialWindowInfo, trackerStatusSupplier.get());

        scheduledExecutorService.scheduleAtFixedRate(
                this::pollActiveWindow,
                pollIntervalSeconds,
                pollIntervalSeconds,
                TimeUnit.SECONDS
        );
        logger.info(
                "AppFocusTracker started (OS={}, interval={}s)",
                nativeOSService.getOperatingSystemName(),
                pollIntervalSeconds
        );
    }

    public WindowInfo getCurrentWindowInfo() {
        return currentWindowInfo.get();
    }

    private void pollActiveWindow() {
        try {
            WindowInfo latestWindowInfo = nativeOSService.getActiveWindowInfo();
            WindowInfo previousWindowInfo = currentWindowInfo.get();
            TrackerStatus trackerStatus = trackerStatusSupplier.get();

            if (Objects.isNull(previousWindowInfo) || !previousWindowInfo.isSameWindow(latestWindowInfo)) {
                currentWindowInfo.set(latestWindowInfo);
                if (Objects.nonNull(focusChangeListener)) {
                    focusChangeListener.onFocusChanged(previousWindowInfo, latestWindowInfo, trackerStatus);
                }
                logActiveWindow(latestWindowInfo, trackerStatus);
            }
        } catch (Exception exception) {
            logger.error("AppFocusTracker error: {}", exception.getMessage(), exception);
        }
    }

    private void logActiveWindow(WindowInfo windowInfo, TrackerStatus trackerStatus) {
        if (trackerStatus == TrackerStatus.IDLE) {
            logger.info("User is IDLE (last window: {})", windowInfo);
            return;
        }
        logger.info("User is active (application {})", windowInfo);
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdownNow();
        logger.info("AppFocusTracker stopped");
    }

    @FunctionalInterface
    public interface FocusChangeListener {

        void onFocusChanged(WindowInfo previousWindowInfo, WindowInfo currentWindowInfo, TrackerStatus trackerStatus);
    }
}
