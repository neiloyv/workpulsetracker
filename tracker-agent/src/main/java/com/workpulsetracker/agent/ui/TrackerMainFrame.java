package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.api.AgentFeedbackClient;
import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.config.AgentConfig;
import com.workpulsetracker.agent.feedback.FeedbackSubmitService;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Главное окно локального агента.
 * При закрытии/сворачивании прячется в трей (не завершает процесс).
 */
public final class TrackerMainFrame extends JFrame {

    private final MainPanel mainPanel;
    private final StatisticsPanel statisticsPanel;
    private final ProgramsPanel programsPanel;
    private final PomodoroPanel pomodoroPanel;
    private final SettingsPanel settingsPanel;
    private final AccountPanel accountPanel;
    private final InfoPanel infoPanel;
    private final FeedbackPanel feedbackPanel;
    private final UserSettings userSettings;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final TrayService trayService;
    private final Timer refreshTimer;
    private final DailyWorkGoalNotifier dailyWorkGoalNotifier;
    private final Runnable exitAction;

    public TrackerMainFrame(
            TrackingEngine trackingEngine,
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            ActivityStore activityStore,
            AgentAccessClient agentAccessClient,
            AgentSyncClient agentSyncClient,
            AgentConfig agentConfig,
            Runnable exitAction
    ) {
        super(Messages.get(MessageCodes.UI_APP_TITLE));
        this.exitAction = exitAction;
        this.userSettings = userSettings;
        this.mainPanel = new MainPanel(trackingEngine, statisticsService, userSettings, userSettingsStore);
        this.statisticsPanel = new StatisticsPanel(statisticsService, userSettings, userSettingsStore);
        this.trayService = new TrayService(this, exitAction);
        this.programsPanel = new ProgramsPanel(
                statisticsService,
                userSettings,
                userSettingsStore,
                this::refreshPanels
        );
        this.pomodoroPanel = new PomodoroPanel(
                userSettings,
                userSettingsStore,
                trayService::showNotification
        );
        this.settingsPanel = new SettingsPanel(
                userSettings,
                userSettingsStore,
                appLanguage -> retranslateUi(),
                this::onAutoStartSettingChanged,
                this::refreshPanels
        );
        this.accountPanel = new AccountPanel(
                userSettings,
                userSettingsStore,
                agentAccessClient,
                agentSyncClient,
                activityStore,
                trackingEngine,
                this::onAccountModeOrCredentialsChanged,
                this::onLocalDataRestored
        );
        this.infoPanel = new InfoPanel();
        AgentFeedbackClient agentFeedbackClient = new AgentFeedbackClient(
                agentConfig.getServerBaseUrl(),
                agentAccessClient,
                userSettingsStore
        );
        this.feedbackPanel = new FeedbackPanel(
                userSettings,
                new FeedbackSubmitService(agentConfig, agentFeedbackClient)
        );
        this.dailyWorkGoalNotifier = new DailyWorkGoalNotifier(
                statisticsService,
                userSettings,
                userSettingsStore
        );

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1320, 900);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        setLocationRelativeTo(null);
        UiTheme.installRoundedWindowCorners(this);
        buildContent();
        wireWindowEvents();
        trayService.install();

        trackingEngine.addStateChangeListener(this::refreshPanels);
        refreshTimer = new Timer(1000, actionEvent -> refreshPanels());
        refreshTimer.start();
    }

    private void buildContent() {
        setIconImages(UiImages.loadWindowIconImages());

        tabbedPane.putClientProperty(
                FlatClientProperties.TABBED_PANE_TAB_TYPE,
                FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED
        );
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, false);
        tabbedPane.putClientProperty(FlatClientProperties.STYLE,
                "tabHeight: 44;"
                        + "tabInsets: 10,20,10,20;"
                        + "foreground: #9E9EB5;"
                        + "selectedForeground: #EDEDF6;"
                        + "hoverColor: #1A1A2B;"
                        + "underlineColor: #7458FF;"
                        + "inactiveUnderlineColor: #2F2F47;"
                        + "contentAreaColor: #0A0A14"
        );

        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_MAIN), mainPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_STATISTICS), statisticsPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_PROGRAMS), programsPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_POMODORO), pomodoroPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_SETTINGS), settingsPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_ACCOUNT), accountPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_INFO), infoPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_FEEDBACK), feedbackPanel);
        tabbedPane.addChangeListener(changeEvent -> {
            if (tabbedPane.getSelectedComponent() == statisticsPanel) {
                statisticsPanel.refresh();
            } else if (tabbedPane.getSelectedComponent() == programsPanel) {
                programsPanel.refresh();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private void wireWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                requestExit();
            }

            @Override
            public void windowIconified(WindowEvent windowEvent) {
                if (userSettings.isMinimizeToTray()) {
                    hideToTray();
                }
            }
        });
    }

    public void hideToTray() {
        setVisible(false);
    }

    public void restoreFromTray() {
        setVisible(true);
        setState(NORMAL);
        toFront();
    }

    public void refreshPanels() {
        mainPanel.refresh();
        dailyWorkGoalNotifier.checkAndNotify();
        statisticsPanel.syncShowPercentagesToggleFromSettings();
        if (tabbedPane.getSelectedComponent() == statisticsPanel) {
            statisticsPanel.refresh();
        }
    }

    private void onAccountModeOrCredentialsChanged() {
        settingsPanel.reloadFromSettings();
        statisticsPanel.onOperationModeChanged();
        accountPanel.retranslate();
        feedbackPanel.reloadFromSettings();
        refreshPanels();
    }

    private void onAutoStartSettingChanged(boolean autoStartTracking) {
        mainPanel.applyAutoStartSetting(autoStartTracking);
    }

    private void onLocalDataRestored() {
        UserLocaleContext.setLanguage(userSettings.getLanguage());
        settingsPanel.reloadFromSettings();
        pomodoroPanel.reloadFromSettings();
        programsPanel.refresh();
        accountPanel.retranslate();
        feedbackPanel.reloadFromSettings();
        statisticsPanel.onOperationModeChanged();
        mainPanel.applyAutoStartSetting(userSettings.isAutoStartTracking());
        retranslateUi();
        refreshPanels();
    }

    public void retranslateUi() {
        setTitle(Messages.get(MessageCodes.UI_APP_TITLE));
        tabbedPane.setTitleAt(0, Messages.get(MessageCodes.UI_TAB_MAIN));
        tabbedPane.setTitleAt(1, Messages.get(MessageCodes.UI_TAB_STATISTICS));
        tabbedPane.setTitleAt(2, Messages.get(MessageCodes.UI_TAB_PROGRAMS));
        tabbedPane.setTitleAt(3, Messages.get(MessageCodes.UI_TAB_POMODORO));
        tabbedPane.setTitleAt(4, Messages.get(MessageCodes.UI_TAB_SETTINGS));
        tabbedPane.setTitleAt(5, Messages.get(MessageCodes.UI_TAB_ACCOUNT));
        tabbedPane.setTitleAt(6, Messages.get(MessageCodes.UI_TAB_INFO));
        tabbedPane.setTitleAt(7, Messages.get(MessageCodes.UI_TAB_FEEDBACK));
        mainPanel.retranslate();
        statisticsPanel.retranslate();
        programsPanel.retranslate();
        pomodoroPanel.retranslate();
        settingsPanel.retranslate();
        accountPanel.retranslate();
        infoPanel.retranslate();
        feedbackPanel.retranslate();
        trayService.retranslate();
    }

    public void shutdownUi() {
        refreshTimer.stop();
        pomodoroPanel.shutdown();
        trayService.uninstall();
        dispose();
    }

    public void requestExit() {
        exitAction.run();
    }
}
