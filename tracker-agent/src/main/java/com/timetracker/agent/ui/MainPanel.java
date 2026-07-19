package com.timetracker.agent.ui;

import com.timetracker.agent.stats.ApplicationUsageSummary;
import com.timetracker.agent.stats.StatisticsService;
import com.timetracker.agent.storage.UserSettings;
import com.timetracker.agent.tracking.TrackingEngine;
import com.timetracker.agent.util.DurationFormatter;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Главная вкладка: Start/Pause, время работы, приложения за сегодня, отправка на сервер.
 */
public final class MainPanel extends JPanel {

    private final TrackingEngine trackingEngine;
    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JLabel workTimeValueLabel = new JLabel("0:00:00");
    private final JLabel statusValueLabel = new JLabel();
    private final JButton startPauseButton = new JButton();
    private final JButton syncButton = new JButton();
    private final DefaultListModel<String> applicationListModel = new DefaultListModel<>();
    private final JList<String> applicationList = new JList<>(applicationListModel);

    public MainPanel(
            TrackingEngine trackingEngine,
            StatisticsService statisticsService,
            UserSettings userSettings
    ) {
        this.trackingEngine = trackingEngine;
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildContent();
        refresh();
    }

    private void buildContent() {
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));

        JPanel timePanel = new JPanel(new BorderLayout());
        JLabel workTimeCaptionLabel = new JLabel(Messages.get(MessageCodes.UI_MAIN_WORK_TIME));
        workTimeValueLabel.setFont(workTimeValueLabel.getFont().deriveFont(Font.BOLD, 28f));
        timePanel.add(workTimeCaptionLabel, BorderLayout.NORTH);
        timePanel.add(workTimeValueLabel, BorderLayout.CENTER);
        timePanel.add(statusValueLabel, BorderLayout.SOUTH);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        startPauseButton.addActionListener(actionEvent -> onStartPauseClicked());
        syncButton.setText(Messages.get(MessageCodes.UI_MAIN_SYNC));
        syncButton.addActionListener(actionEvent -> onSyncClicked());
        buttonsPanel.add(startPauseButton);
        buttonsPanel.add(syncButton);

        topPanel.add(timePanel, BorderLayout.CENTER);
        topPanel.add(buttonsPanel, BorderLayout.SOUTH);

        JPanel applicationsPanel = new JPanel(new BorderLayout(4, 4));
        applicationsPanel.add(
                new JLabel(Messages.get(MessageCodes.UI_MAIN_APPLICATIONS_TODAY)),
                BorderLayout.NORTH
        );
        applicationsPanel.add(new JScrollPane(applicationList), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(applicationsPanel, BorderLayout.CENTER);
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        long todayActiveSeconds = statisticsService.buildTodayActiveSeconds();
        workTimeValueLabel.setText(DurationFormatter.formatSeconds(todayActiveSeconds));

        boolean trackingEnabled = trackingEngine.isTrackingEnabled();
        startPauseButton.setText(
                trackingEnabled
                        ? Messages.get(MessageCodes.UI_MAIN_PAUSE)
                        : Messages.get(MessageCodes.UI_MAIN_START)
        );
        statusValueLabel.setText(
                trackingEnabled
                        ? Messages.get(MessageCodes.UI_MAIN_STATUS_RUNNING)
                        : Messages.get(MessageCodes.UI_MAIN_STATUS_PAUSED)
        );

        boolean syncEnabled = userSettings.isServerSyncEnabled();
        syncButton.setEnabled(syncEnabled);
        syncButton.setToolTipText(
                syncEnabled
                        ? Messages.get(MessageCodes.UI_MAIN_SYNC_TOOLTIP)
                        : Messages.get(MessageCodes.UI_MAIN_SYNC_DISABLED_TOOLTIP)
        );

        List<String> applicationLines = statisticsService.buildTodayApplicationUsage().stream()
                .map(this::formatApplicationUsageLine)
                .collect(Collectors.toList());
        applicationListModel.clear();
        applicationLines.forEach(applicationListModel::addElement);
        if (applicationListModel.isEmpty()) {
            applicationListModel.addElement(Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS));
        }
    }

    private String formatApplicationUsageLine(ApplicationUsageSummary applicationUsageSummary) {
        return applicationUsageSummary.getApplicationName()
                + " — "
                + DurationFormatter.formatSeconds(applicationUsageSummary.getDurationSeconds());
    }

    private void onStartPauseClicked() {
        if (trackingEngine.isTrackingEnabled()) {
            trackingEngine.pauseTracking();
        } else {
            trackingEngine.startTracking();
        }
        refresh();
    }

    private void onSyncClicked() {
        // Пока заглушка: серверная синхронизация появится позже
        javax.swing.JOptionPane.showMessageDialog(
                this,
                Messages.get(MessageCodes.UI_MAIN_SYNC_NOT_IMPLEMENTED),
                Messages.get(MessageCodes.UI_MAIN_SYNC),
                javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }
}
