package com.timetracker.agent.ui;

import com.timetracker.agent.stats.ApplicationUsageSummary;
import com.timetracker.agent.stats.StatisticsService;
import com.timetracker.agent.storage.UserSettings;
import com.timetracker.agent.tracking.TrackingEngine;
import com.timetracker.agent.util.DurationFormatter;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Главная вкладка: Start/Pause, время работы, приложения за сегодня, отправка на сервер.
 */
public final class MainPanel extends JPanel {

    private final TrackingEngine trackingEngine;
    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JLabel workTimeCaptionLabel = new JLabel();
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
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        refresh();
    }

    private void buildContent() {
        JPanel heroPanel = new JPanel();
        heroPanel.setOpaque(false);
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));

        workTimeCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_WORK_TIME));
        workTimeCaptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        workTimeCaptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        UiTheme.styleMutedLabel(workTimeCaptionLabel);

        UiTheme.styleTimerLabel(workTimeValueLabel);
        workTimeValueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusValueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        UiTheme.styleMutedLabel(statusValueLabel);

        startPauseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startPauseButton.setMaximumSize(new Dimension(220, 48));
        startPauseButton.addActionListener(actionEvent -> onStartPauseClicked());
        UiTheme.stylePrimaryButton(startPauseButton);

        syncButton.setText(Messages.get(MessageCodes.UI_MAIN_SYNC));
        syncButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        syncButton.setMaximumSize(new Dimension(220, 40));
        syncButton.addActionListener(actionEvent -> onSyncClicked());
        UiTheme.styleSecondaryButton(syncButton);

        heroPanel.add(Box.createVerticalStrut(8));
        heroPanel.add(workTimeCaptionLabel);
        heroPanel.add(Box.createVerticalStrut(8));
        heroPanel.add(workTimeValueLabel);
        heroPanel.add(Box.createVerticalStrut(6));
        heroPanel.add(statusValueLabel);
        heroPanel.add(Box.createVerticalStrut(18));
        heroPanel.add(startPauseButton);
        heroPanel.add(Box.createVerticalStrut(10));
        heroPanel.add(syncButton);

        JPanel applicationsPanel = new JPanel(new BorderLayout(4, 8));
        UiTheme.styleSurfaceCard(applicationsPanel);
        JLabel applicationsCaptionLabel = new JLabel(Messages.get(MessageCodes.UI_MAIN_APPLICATIONS_TODAY));
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        applicationList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        applicationsPanel.add(applicationsCaptionLabel, BorderLayout.NORTH);
        applicationsPanel.add(new JScrollPane(applicationList), BorderLayout.CENTER);

        add(heroPanel, BorderLayout.NORTH);
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
        if (trackingEnabled) {
            UiTheme.styleDangerButton(startPauseButton);
        } else {
            UiTheme.stylePrimaryButton(startPauseButton);
        }

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
        JOptionPane.showMessageDialog(
                this,
                Messages.get(MessageCodes.UI_MAIN_SYNC_NOT_IMPLEMENTED),
                Messages.get(MessageCodes.UI_MAIN_SYNC),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
