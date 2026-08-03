package com.workpulsetracker.agent.api;

import com.workpulsetracker.agent.storage.LocalAppRuntimeStore;
import com.workpulsetracker.agent.storage.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Периодическая отправка telemetry batch с retry (счётчики не сбрасываются при ошибке).
 */
public final class TelemetryUploadScheduler implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryUploadScheduler.class);

    private final AgentSyncClient agentSyncClient;
    private final Supplier<UserSettings> userSettingsSupplier;
    private final long uploadIntervalSeconds;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public TelemetryUploadScheduler(
            AgentSyncClient agentSyncClient,
            Supplier<UserSettings> userSettingsSupplier,
            long uploadIntervalSeconds
    ) {
        this.agentSyncClient = Objects.requireNonNull(agentSyncClient);
        this.userSettingsSupplier = Objects.requireNonNull(userSettingsSupplier);
        this.uploadIntervalSeconds = Math.max(uploadIntervalSeconds, 15L);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "telemetry-upload");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        scheduledExecutorService.scheduleWithFixedDelay(
                this::uploadSafely,
                uploadIntervalSeconds,
                uploadIntervalSeconds,
                TimeUnit.SECONDS
        );
        logger.info("Telemetry upload scheduler started: interval={}s", uploadIntervalSeconds);
    }

    private void uploadSafely() {
        try {
            UserSettings userSettings = userSettingsSupplier.get();
            if (Objects.isNull(userSettings) || !userSettings.isServerSyncEnabled()) {
                return;
            }
            agentSyncClient.uploadTelemetry(userSettings);
        } catch (Exception exception) {
            logger.warn("Periodic telemetry upload failed: {}", exception.getMessage());
        }
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdownNow();
    }
}
