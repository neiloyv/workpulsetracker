package com.workpulsetracker.agent.tracking;

import com.workpulsetracker.agent.activity.ActivityMonitor;
import com.workpulsetracker.agent.activity.NativeActivityMonitor;
import com.workpulsetracker.agent.buffer.ActivityBufferListener;
import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.buffer.DataBuffer;
import com.workpulsetracker.agent.config.AgentConfig;
import com.workpulsetracker.agent.focus.AppFocusTracker;
import com.workpulsetracker.agent.focus.NativeOSService;
import com.workpulsetracker.agent.focus.NativeOSServiceFactory;
import com.workpulsetracker.agent.focus.WindowInfo;
import com.workpulsetracker.agent.idle.IdleDetector;
import com.workpulsetracker.agent.idle.TrackerStatus;
import com.workpulsetracker.agent.storage.ActivityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Управляет жизненным циклом трекинга: старт мониторов, Start/Pause записи времени.
 */
public final class TrackingEngine implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TrackingEngine.class);

    private final DataBuffer dataBuffer;
    private final ActivityStore activityStore;
    private final NativeOSService nativeOSService;
    private final IdleDetector idleDetector;
    private final ActivityMonitor activityMonitor;
    private final AppFocusTracker appFocusTracker;
    private final AtomicBoolean trackingEnabled = new AtomicBoolean(false);
    private final AtomicBoolean monitorsStarted = new AtomicBoolean(false);
    private final List<Runnable> stateChangeListeners = new CopyOnWriteArrayList<>();

    public TrackingEngine(AgentConfig agentConfig, DataBuffer dataBuffer, ActivityStore activityStore) {
        this.dataBuffer = dataBuffer;
        this.activityStore = activityStore;
        this.nativeOSService = NativeOSServiceFactory.create();
        this.idleDetector = new IdleDetector(
                agentConfig.getIdleTimeoutSeconds(),
                agentConfig.getIdleCheckIntervalSeconds()
        );
        this.activityMonitor = new NativeActivityMonitor();
        this.appFocusTracker = new AppFocusTracker(
                nativeOSService,
                agentConfig.getFocusPollIntervalSeconds(),
                idleDetector::getCurrentStatus,
                (previousWindowInfo, currentWindowInfo, trackerStatus) ->
                        openIntervalIfTracking(currentWindowInfo, trackerStatus)
        );

        idleDetector.addListener((previousStatus, currentStatus) -> {
            if (!trackingEnabled.get()) {
                return;
            }
            WindowInfo currentWindowInfo = appFocusTracker.getCurrentWindowInfo();
            if (Objects.isNull(currentWindowInfo)) {
                currentWindowInfo = nativeOSService.getActiveWindowInfo();
            }
            openIntervalIfTracking(currentWindowInfo, currentStatus);
        });

        activityMonitor.addListener(idleDetector::onUserActivity);

        dataBuffer.addListener(new ActivityBufferListener() {
            @Override
            public void onIntervalClosed(ActivityInterval activityInterval) {
                activityStore.appendClosedInterval(activityInterval);
                notifyStateChanged();
            }

            @Override
            public void onIntervalOpened(ActivityInterval activityInterval) {
                notifyStateChanged();
            }
        });
    }

    public void initializeMonitors() {
        if (!monitorsStarted.compareAndSet(false, true)) {
            return;
        }
        activityMonitor.start();
        idleDetector.start();
        appFocusTracker.start();
        logger.info("TrackingEngine monitors started (time recording paused)");
    }

    public void startTracking() {
        if (!monitorsStarted.get()) {
            initializeMonitors();
        }
        if (!trackingEnabled.compareAndSet(false, true)) {
            return;
        }
        WindowInfo currentWindowInfo = nativeOSService.getActiveWindowInfo();
        openIntervalIfTracking(currentWindowInfo, idleDetector.getCurrentStatus());
        logger.info("Tracking started");
        notifyStateChanged();
    }

    public void pauseTracking() {
        if (!trackingEnabled.compareAndSet(true, false)) {
            return;
        }
        dataBuffer.closeCurrentInterval();
        logger.info("Tracking paused");
        notifyStateChanged();
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled.get();
    }

    public TrackerStatus getTrackerStatus() {
        return idleDetector.getCurrentStatus();
    }

    public DataBuffer getDataBuffer() {
        return dataBuffer;
    }

    public ActivityStore getActivityStore() {
        return activityStore;
    }

    public void addStateChangeListener(Runnable stateChangeListener) {
        if (Objects.nonNull(stateChangeListener)) {
            stateChangeListeners.add(stateChangeListener);
        }
    }

    private void openIntervalIfTracking(WindowInfo windowInfo, TrackerStatus trackerStatus) {
        if (!trackingEnabled.get()) {
            return;
        }
        boolean idle = trackerStatus == TrackerStatus.IDLE;
        String applicationName = Objects.nonNull(windowInfo) ? windowInfo.getProcessName() : "unknown";
        String windowTitle = Objects.nonNull(windowInfo) ? windowInfo.getWindowTitle() : "";
        dataBuffer.startInterval(applicationName, windowTitle, idle);
    }

    private void notifyStateChanged() {
        stateChangeListeners.forEach(Runnable::run);
    }

    @Override
    public void close() {
        trackingEnabled.set(false);
        dataBuffer.closeCurrentInterval();
        closeQuietly(appFocusTracker);
        closeQuietly(idleDetector);
        closeQuietly(activityMonitor);
    }

    private void closeQuietly(AutoCloseable autoCloseable) {
        if (Objects.isNull(autoCloseable)) {
            return;
        }
        try {
            autoCloseable.close();
        } catch (Exception exception) {
            logger.warn("Failed to close resource: {}", exception.getMessage());
        }
    }
}
