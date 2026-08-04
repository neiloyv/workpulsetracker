package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;

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
    private final PomodoroPanel pomodoroPanel;
    private final SettingsPanel settingsPanel;
    private final AccountPanel accountPanel;
    private final UserSettings userSettings;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final TrayService trayService;
    private final Timer refreshTimer;
    private final Runnable exitAction;

    public TrackerMainFrame(
            TrackingEngine trackingEngine,
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            ActivityStore activityStore,
            AgentAccessClient agentAccessClient,
            AgentSyncClient agentSyncClient,
            Runnable exitAction
    ) {
        super(Messages.get(MessageCodes.UI_APP_TITLE));
        this.exitAction = exitAction;
        this.userSettings = userSettings;
        this.mainPanel = new MainPanel(trackingEngine, statisticsService, userSettings);
        this.statisticsPanel = new StatisticsPanel(statisticsService, userSettings, userSettingsStore);
        this.trayService = new TrayService(this, exitAction);
        this.pomodoroPanel = new PomodoroPanel(
                userSettings,
                userSettingsStore,
                trayService::showNotification
        );
        this.settingsPanel = new SettingsPanel(
                userSettings,
                userSettingsStore,
                activityStore,
                trackingEngine,
                agentSyncClient,
                appLanguage -> retranslateUi(),
                this::onAutoStartSettingChanged,
                this::refreshPanels,
                this::onLocalDataRestored
        );
        this.accountPanel = new AccountPanel(
                userSettings,
                userSettingsStore,
                agentAccessClient,
                agentSyncClient,
                this::onAccountModeOrCredentialsChanged
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

        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_MAIN), mainPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_STATISTICS), statisticsPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_POMODORO), pomodoroPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_SETTINGS), settingsPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_ACCOUNT), accountPanel);
        tabbedPane.addChangeListener(changeEvent -> {
            if (tabbedPane.getSelectedComponent() == statisticsPanel) {
                statisticsPanel.refresh();
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
        statisticsPanel.refresh();
    }

    private void onAccountModeOrCredentialsChanged() {
        settingsPanel.reloadFromSettings();
        statisticsPanel.onOperationModeChanged();
        accountPanel.retranslate();
        refreshPanels();
    }

    private void onAutoStartSettingChanged(boolean autoStartTracking) {
        mainPanel.applyAutoStartSetting(autoStartTracking);
    }

    private void onLocalDataRestored() {
        UserLocaleContext.setLanguage(userSettings.getLanguage());
        settingsPanel.reloadFromSettings();
        pomodoroPanel.reloadFromSettings();
        accountPanel.retranslate();
        statisticsPanel.onOperationModeChanged();
        mainPanel.applyAutoStartSetting(userSettings.isAutoStartTracking());
        retranslateUi();
        refreshPanels();
    }

    public void retranslateUi() {
        setTitle(Messages.get(MessageCodes.UI_APP_TITLE));
        tabbedPane.setTitleAt(0, Messages.get(MessageCodes.UI_TAB_MAIN));
        tabbedPane.setTitleAt(1, Messages.get(MessageCodes.UI_TAB_STATISTICS));
        tabbedPane.setTitleAt(2, Messages.get(MessageCodes.UI_TAB_POMODORO));
        tabbedPane.setTitleAt(3, Messages.get(MessageCodes.UI_TAB_SETTINGS));
        tabbedPane.setTitleAt(4, Messages.get(MessageCodes.UI_TAB_ACCOUNT));
        mainPanel.retranslate();
        statisticsPanel.retranslate();
        pomodoroPanel.retranslate();
        settingsPanel.retranslate();
        accountPanel.retranslate();
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
