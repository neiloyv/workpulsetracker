package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Главная вкладка: Start/Pause, время работы, таблица приложений за сегодня, отправка на сервер.
 */
public final class MainPanel extends JPanel {

    private final TrackingEngine trackingEngine;
    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JLabel workTimeCaptionLabel = new JLabel();
    private final JLabel workTimeValueLabel = new JLabel("0:00:00");
    private final JLabel statusValueLabel = new JLabel();
    private final JLabel applicationsCaptionLabel = new JLabel();
    private final JButton startPauseButton = new JButton();
    private final JButton syncButton = new JButton();
    private final ApplicationUsageTableModel applicationUsageTableModel = new ApplicationUsageTableModel();
    private final JTable applicationUsageTable = new JTable(applicationUsageTableModel);
    private final JPanel applicationsPanel = new JPanel(new BorderLayout(4, 8));

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

        JLabel logoLabel = createLogoLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        syncButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        syncButton.setMaximumSize(new Dimension(220, 40));
        syncButton.addActionListener(actionEvent -> onSyncClicked());
        UiTheme.styleSecondaryButton(syncButton);

        heroPanel.add(logoLabel);
        heroPanel.add(Box.createVerticalStrut(12));
        heroPanel.add(workTimeCaptionLabel);
        heroPanel.add(Box.createVerticalStrut(8));
        heroPanel.add(workTimeValueLabel);
        heroPanel.add(Box.createVerticalStrut(6));
        heroPanel.add(statusValueLabel);
        heroPanel.add(Box.createVerticalStrut(18));
        heroPanel.add(startPauseButton);
        heroPanel.add(Box.createVerticalStrut(10));
        heroPanel.add(syncButton);

        UiTheme.styleSurfaceCard(applicationsPanel);
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        UiTheme.styleUsageTable(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnWidths(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        applicationsPanel.add(applicationsCaptionLabel, BorderLayout.NORTH);
        applicationsPanel.add(new JScrollPane(applicationUsageTable), BorderLayout.CENTER);

        add(heroPanel, BorderLayout.NORTH);
        add(applicationsPanel, BorderLayout.CENTER);
        retranslate();
        applyAutoStartSetting(userSettings.isAutoStartTracking());
    }

    private JLabel createLogoLabel() {
        ImageIcon logoIcon = UiImages.loadLogoIcon(44);
        JLabel logoLabel = new JLabel();
        if (Objects.nonNull(logoIcon)) {
            logoLabel.setIcon(logoIcon);
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            logoLabel.setText(Messages.get(MessageCodes.UI_APP_TITLE));
            logoLabel.setForeground(UiTheme.TEXT_PRIMARY);
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        return logoLabel;
    }

    public void retranslate() {
        workTimeCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_WORK_TIME));
        applicationsCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_APPLICATIONS_TODAY));
        syncButton.setText(Messages.get(MessageCodes.UI_MAIN_SYNC));
        applicationUsageTableModel.retranslate();
        ApplicationUsageTableModel.configureColumnWidths(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        refresh();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(applicationsPanel);
        UiTheme.styleMutedLabel(workTimeCaptionLabel);
        UiTheme.styleMutedLabel(statusValueLabel);
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        UiTheme.styleTimerLabel(workTimeValueLabel);
        UiTheme.styleUsageTable(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        UiTheme.styleSecondaryButton(syncButton);
        refresh();
    }

    public void applyAutoStartSetting(boolean autoStartTracking) {
        startPauseButton.setVisible(!autoStartTracking);
        if (autoStartTracking && !trackingEngine.isTrackingEnabled()) {
            trackingEngine.startTracking();
        }
        revalidate();
        repaint();
        refresh();
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

        List<ApplicationUsageSummary> applicationUsageSummaries = ApplicationUsageFilter.groupMinorApplications(
                statisticsService.buildTodayApplicationUsage(),
                userSettings.getMinorUsageThresholdMinutes()
        );
        applicationUsageTableModel.setRows(applicationUsageSummaries, todayActiveSeconds);
        if (applicationUsageTableModel.isEmpty()) {
            applicationUsageTableModel.setRows(
                    Collections.singletonList(
                            new ApplicationUsageSummary(Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS), 0L)
                    ),
                    0L
            );
        }
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
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
