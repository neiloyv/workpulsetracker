package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

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
    private final SettingsPanel settingsPanel;
    private final AccountPanel accountPanel;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final TrayService trayService;
    private final Timer refreshTimer;
    private final Runnable exitAction;

    public TrackerMainFrame(
            TrackingEngine trackingEngine,
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            Runnable exitAction
    ) {
        super(Messages.get(MessageCodes.UI_APP_TITLE));
        this.exitAction = exitAction;
        this.mainPanel = new MainPanel(trackingEngine, statisticsService, userSettings);
        this.statisticsPanel = new StatisticsPanel(statisticsService, userSettings, userSettingsStore);
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
                this::refreshPanels
        );
        this.trayService = new TrayService(this, exitAction);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(900, 600));
        setSize(1100, 760);
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
                hideToTray();
            }

            @Override
            public void windowIconified(WindowEvent windowEvent) {
                hideToTray();
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

    private void onAutoStartSettingChanged(boolean autoStartTracking) {
        mainPanel.applyAutoStartSetting(autoStartTracking);
    }

    public void retranslateUi() {
        setTitle(Messages.get(MessageCodes.UI_APP_TITLE));
        tabbedPane.setTitleAt(0, Messages.get(MessageCodes.UI_TAB_MAIN));
        tabbedPane.setTitleAt(1, Messages.get(MessageCodes.UI_TAB_STATISTICS));
        tabbedPane.setTitleAt(2, Messages.get(MessageCodes.UI_TAB_SETTINGS));
        tabbedPane.setTitleAt(3, Messages.get(MessageCodes.UI_TAB_ACCOUNT));
        mainPanel.retranslate();
        statisticsPanel.retranslate();
        settingsPanel.retranslate();
        accountPanel.retranslate();
        trayService.retranslate();
    }

    public void shutdownUi() {
        refreshTimer.stop();
        trayService.uninstall();
        dispose();
    }

    public void requestExit() {
        exitAction.run();
    }
}
