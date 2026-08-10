package com.workpulsetracker.agent;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.api.TelemetryUploadScheduler;
import com.workpulsetracker.agent.buffer.DataBuffer;
import com.workpulsetracker.agent.config.AgentConfig;
import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.LocalAppRuntimeStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.agent.ui.ActivationDialog;
import com.workpulsetracker.agent.ui.StartupFailureDialogs;
import com.workpulsetracker.agent.ui.TrackerMainFrame;
import com.workpulsetracker.agent.ui.UiTheme;
import com.workpulsetracker.agent.util.JNativeHookLibraryBootstrap;
import com.workpulsetracker.agent.util.WindowsLaunchAtLoginService;
import com.workpulsetracker.common.i18n.UserLocaleContext;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.Objects;

/**
 * Точка входа локального трекера с окном UI и иконкой в трее.
 */
public final class TrackerAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(TrackerAgentApplication.class);

    private TrackerAgentApplication() {
    }

    public static void main(String[] args) {
        JNativeHookLibraryBootstrap.configureLibraryPath();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logger.error(
                        "schema=local Uncaught exception in thread {}: {}",
                        thread.getName(),
                        throwable.getMessage(),
                        throwable
                )
        );
        UiTheme.install();

        SwingUtilities.invokeLater(() -> {
            try {
                startApplication();
            } catch (Throwable throwable) {
                logger.error(
                        "schema=local Failed to start the tracker agent: {}",
                        throwable.getMessage(),
                        throwable
                );
                flushLogs();
                StartupFailureDialogs.showStartupFailure(null, throwable);
                System.exit(1);
            }
        });
    }

    private static void startApplication() {
        logger.info("schema=local Starting tracker agent UI bootstrap");
        AgentConfig agentConfig = AgentConfig.load();
        UserSettingsStore userSettingsStore = new UserSettingsStore();
        UserSettings userSettings = userSettingsStore.loadOrCreateDefault();
        WindowsLaunchAtLoginService.apply(userSettings.isLaunchAtLogin());

        // Язык: сначала из сохранённых настроек пользователя, иначе из application.properties
        if (!userSettings.isSetupCompleted()) {
            userSettings.setLanguageCode(agentConfig.getLanguage().getCode());
        }
        UserLocaleContext.setLanguage(userSettings.getLanguage());
        ApplicationIconService.getInstance().load();

        ActivityStore activityStore = new ActivityStore();
        activityStore.load();

        LocalAppRuntimeStore localAppRuntimeStore = new LocalAppRuntimeStore();
        localAppRuntimeStore.load();

        AgentAccessClient agentAccessClient = new AgentAccessClient(agentConfig.getServerBaseUrl());
        AgentSyncClient agentSyncClient = new AgentSyncClient(
                agentConfig.getServerBaseUrl(),
                agentAccessClient,
                localAppRuntimeStore,
                userSettingsStore
        );

        DataBuffer dataBuffer = DataBuffer.getInstance();
        logger.info("schema=local Creating tracking engine");
        TrackingEngine trackingEngine = new TrackingEngine(
                agentConfig,
                dataBuffer,
                activityStore,
                localAppRuntimeStore
        );
        StatisticsService statisticsService = new StatisticsService(activityStore, dataBuffer, userSettings);

        TrackerMainFrame[] trackerMainFrameHolder = new TrackerMainFrame[1];
        TelemetryUploadScheduler[] telemetryUploadSchedulerHolder = new TelemetryUploadScheduler[1];
        Runnable exitAction = () -> {
            if (Objects.nonNull(trackerMainFrameHolder[0])) {
                trackerMainFrameHolder[0].shutdownUi();
            }
            if (Objects.nonNull(telemetryUploadSchedulerHolder[0])) {
                telemetryUploadSchedulerHolder[0].close();
            }
            shutdown(trackingEngine);
        };

        logger.info("schema=local Creating main window");
        TrackerMainFrame trackerMainFrame = new TrackerMainFrame(
                trackingEngine,
                statisticsService,
                userSettings,
                userSettingsStore,
                activityStore,
                agentAccessClient,
                agentSyncClient,
                agentConfig,
                exitAction
        );
        trackerMainFrameHolder[0] = trackerMainFrame;

        if (!userSettings.isSetupCompleted()) {
            ActivationDialog activationDialog = new ActivationDialog(trackerMainFrame, agentAccessClient);
            boolean confirmed = activationDialog.showAndWait();
            if (!confirmed) {
                exitAction.run();
                return;
            }
            if (activationDialog.isLocalSoloSelected()) {
                userSettings.applyLocalOnlyMode();
            } else {
                userSettings.applyCredentials(activationDialog.getEmail(), activationDialog.getAccessKey());
                AgentAccessClient.AgentAuthResult agentAuthResult = activationDialog.getAgentAuthResult();
                if (Objects.nonNull(agentAuthResult)) {
                    userSettings.applyAgentAuth(
                            agentAuthResult.accessToken(),
                            agentAuthResult.hardwareId(),
                            agentAuthResult.workerId(),
                            agentAuthResult.deviceId()
                    );
                }
            }
            userSettingsStore.save(userSettings);
            trackerMainFrame.refreshPanels();
        }

        TelemetryUploadScheduler telemetryUploadScheduler = new TelemetryUploadScheduler(
                agentSyncClient,
                () -> userSettings,
                agentConfig.getTelemetryUploadIntervalSeconds()
        );
        telemetryUploadScheduler.start();
        telemetryUploadSchedulerHolder[0] = telemetryUploadScheduler;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("schema=local Stopping tracker-agent...");
            telemetryUploadScheduler.close();
            trackingEngine.close();
        }, "tracker-agent-shutdown"));

        trackerMainFrame.setVisible(true);
        startAutoTrackingIfConfigured(trackingEngine, trackerMainFrame, userSettings);
        logger.info("schema=local tracker-agent UI started");
    }

    private static void startAutoTrackingIfConfigured(
            TrackingEngine trackingEngine,
            TrackerMainFrame trackerMainFrame,
            UserSettings userSettings
    ) {
        if (!userSettings.isAutoStartTracking()) {
            return;
        }
        try {
            trackingEngine.startTracking();
            trackerMainFrame.refreshPanels();
        } catch (Exception exception) {
            logger.error(
                    "schema=local Failed to auto-start tracking: {}",
                    exception.getMessage(),
                    exception
            );
            StartupFailureDialogs.showAutoStartTrackingFailure(trackerMainFrame, exception);
        }
    }

    private static void flushLogs() {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            loggerContext.stop();
        }
    }

    private static void shutdown(TrackingEngine trackingEngine) {
        try {
            if (Objects.nonNull(trackingEngine)) {
                trackingEngine.close();
            }
        } finally {
            System.exit(0);
        }
    }
}
