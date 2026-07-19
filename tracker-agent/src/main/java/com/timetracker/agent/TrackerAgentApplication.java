package com.timetracker.agent;

import com.timetracker.agent.buffer.DataBuffer;
import com.timetracker.agent.config.AgentConfig;
import com.timetracker.agent.stats.StatisticsService;
import com.timetracker.agent.storage.ActivityStore;
import com.timetracker.agent.storage.UserSettings;
import com.timetracker.agent.storage.UserSettingsStore;
import com.timetracker.agent.tracking.TrackingEngine;
import com.timetracker.agent.ui.ActivationDialog;
import com.timetracker.agent.ui.TrackerMainFrame;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;
import com.timetracker.common.i18n.UserLocaleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Objects;

/**
 * Точка входа локального трекера с окном UI и иконкой в трее.
 */
public final class TrackerAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(TrackerAgentApplication.class);

    private TrackerAgentApplication() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            logger.debug("Не удалось установить системный Look&Feel: {}", exception.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                startApplication();
            } catch (Exception exception) {
                String userFacingMessage = Messages.get(
                        MessageCodes.ERROR_AGENT_STARTUP_FAILED,
                        exception.getMessage()
                );
                logger.error(userFacingMessage, exception);
                System.exit(1);
            }
        });
    }

    private static void startApplication() {
        AgentConfig agentConfig = AgentConfig.load();
        UserSettingsStore userSettingsStore = new UserSettingsStore();
        UserSettings userSettings = userSettingsStore.loadOrCreateDefault();

        // Язык: сначала из сохранённых настроек пользователя, иначе из application.properties
        if (!userSettings.isSetupCompleted()) {
            userSettings.setLanguageCode(agentConfig.getLanguage().getCode());
        }
        UserLocaleContext.setLanguage(userSettings.getLanguage());

        ActivityStore activityStore = new ActivityStore();
        activityStore.load();

        DataBuffer dataBuffer = DataBuffer.getInstance();
        TrackingEngine trackingEngine = new TrackingEngine(agentConfig, dataBuffer, activityStore);
        StatisticsService statisticsService = new StatisticsService(activityStore, dataBuffer);

        TrackerMainFrame[] trackerMainFrameHolder = new TrackerMainFrame[1];
        Runnable exitAction = () -> {
            if (Objects.nonNull(trackerMainFrameHolder[0])) {
                trackerMainFrameHolder[0].shutdownUi();
            }
            shutdown(trackingEngine);
        };

        TrackerMainFrame trackerMainFrame = new TrackerMainFrame(
                trackingEngine,
                statisticsService,
                userSettings,
                exitAction
        );
        trackerMainFrameHolder[0] = trackerMainFrame;

        if (!userSettings.isSetupCompleted()) {
            ActivationDialog activationDialog = new ActivationDialog(trackerMainFrame);
            boolean confirmed = activationDialog.showAndWait();
            if (!confirmed) {
                exitAction.run();
                return;
            }
            if (activationDialog.isLocalOnlySelected()) {
                userSettings.applyLocalOnlyMode();
            } else {
                userSettings.applyActivationKey(activationDialog.getActivationKey());
            }
            userSettingsStore.save(userSettings);
            trackerMainFrame.refreshPanels();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Остановка tracker-agent...");
            trackingEngine.close();
        }, "tracker-agent-shutdown"));

        trackerMainFrame.setVisible(true);
        logger.info("tracker-agent UI запущен");
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
