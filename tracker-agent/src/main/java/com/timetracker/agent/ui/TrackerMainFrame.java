package com.timetracker.agent.ui;

import com.timetracker.agent.stats.StatisticsService;
import com.timetracker.agent.storage.UserSettings;
import com.timetracker.agent.tracking.TrackingEngine;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;

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
    private final TrayService trayService;
    private final Timer refreshTimer;
    private final Runnable exitAction;

    public TrackerMainFrame(
            TrackingEngine trackingEngine,
            StatisticsService statisticsService,
            UserSettings userSettings,
            Runnable exitAction
    ) {
        super(Messages.get(MessageCodes.UI_APP_TITLE));
        this.exitAction = exitAction;
        this.mainPanel = new MainPanel(trackingEngine, statisticsService, userSettings);
        this.statisticsPanel = new StatisticsPanel(statisticsService);
        this.trayService = new TrayService(this, exitAction);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(480, 560));
        getContentPane().setBackground(UiTheme.BACKGROUND);
        setLocationRelativeTo(null);
        buildContent();
        wireWindowEvents();
        trayService.install();

        trackingEngine.addStateChangeListener(this::refreshPanels);
        refreshTimer = new Timer(1000, actionEvent -> refreshPanels());
        refreshTimer.start();
    }

    private void buildContent() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_MAIN), mainPanel);
        tabbedPane.addTab(Messages.get(MessageCodes.UI_TAB_STATISTICS), statisticsPanel);
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

    public void shutdownUi() {
        refreshTimer.stop();
        trayService.uninstall();
        dispose();
    }

    public void requestExit() {
        exitAction.run();
    }
}
