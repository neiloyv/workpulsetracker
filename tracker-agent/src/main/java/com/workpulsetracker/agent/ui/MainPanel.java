package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageBrowserGrouper;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageGroup;
import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Главная вкладка: Start/Pause, время работы, таймлайн, таблица и круговая диаграмма за сегодня.
 */
public final class MainPanel extends JPanel {

    private final TrackingEngine trackingEngine;
    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JLabel workTimeCaptionLabel = new JLabel();
    private final JLabel workTimeValueLabel = new JLabel("0:00:00");
    private final JLabel statusValueLabel = new JLabel();
    private final JLabel timelineCaptionLabel = new JLabel();
    private final JLabel applicationsCaptionLabel = new JLabel();
    private final JLabel chartCaptionLabel = new JLabel();
    private final JButton startPauseButton = new JButton();
    private final ApplicationUsageTableModel applicationUsageTableModel = new ApplicationUsageTableModel();
    private final JTable applicationUsageTable = new JTable(applicationUsageTableModel);
    private final ApplicationUsagePieChartPanel usagePieChartPanel = new ApplicationUsagePieChartPanel();
    private final DayActivityTimelinePanel dayActivityTimelinePanel = new DayActivityTimelinePanel();
    private final JPanel timelinePanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel applicationsPanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel chartPanel = new JPanel(new BorderLayout(4, 8));

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

        heroPanel.add(logoLabel);
        heroPanel.add(Box.createVerticalStrut(16));
        heroPanel.add(workTimeCaptionLabel);
        heroPanel.add(Box.createVerticalStrut(8));
        heroPanel.add(workTimeValueLabel);
        heroPanel.add(Box.createVerticalStrut(6));
        heroPanel.add(statusValueLabel);
        heroPanel.add(Box.createVerticalStrut(18));
        heroPanel.add(startPauseButton);

        UiTheme.styleSurfaceCard(timelinePanel);
        UiTheme.styleMutedLabel(timelineCaptionLabel);
        timelinePanel.add(timelineCaptionLabel, BorderLayout.NORTH);
        timelinePanel.add(dayActivityTimelinePanel, BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(applicationsPanel);
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        UiTheme.styleUsageTable(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnWidths(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        applicationUsageTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int rowIndex = applicationUsageTable.rowAtPoint(mouseEvent.getPoint());
                int columnIndex = applicationUsageTable.columnAtPoint(mouseEvent.getPoint());
                if (columnIndex != 0 || rowIndex < 0) {
                    return;
                }
                if (applicationUsageTableModel.isExpandableRow(rowIndex)) {
                    applicationUsageTableModel.toggleExpanded(rowIndex);
                }
            }
        });
        applicationsPanel.add(applicationsCaptionLabel, BorderLayout.NORTH);
        applicationsPanel.add(new JScrollPane(applicationUsageTable), BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(chartPanel);
        UiTheme.styleMutedLabel(chartCaptionLabel);
        chartPanel.add(chartCaptionLabel, BorderLayout.NORTH);
        chartPanel.add(usagePieChartPanel, BorderLayout.CENTER);

        JPanel bottomSplitPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomSplitPanel.setOpaque(false);
        bottomSplitPanel.add(applicationsPanel);
        bottomSplitPanel.add(chartPanel);

        JPanel centerContentPanel = new JPanel(new BorderLayout(0, 12));
        centerContentPanel.setOpaque(false);
        centerContentPanel.add(timelinePanel, BorderLayout.NORTH);
        centerContentPanel.add(bottomSplitPanel, BorderLayout.CENTER);

        add(heroPanel, BorderLayout.NORTH);
        add(centerContentPanel, BorderLayout.CENTER);
        retranslate();
        applyAutoStartSetting(userSettings.isAutoStartTracking());
    }

    private JLabel createLogoLabel() {
        ImageIcon logoIcon = UiImages.loadLogoIcon(64);
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
        timelineCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_TODAY));
        applicationsCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_APPLICATIONS_TODAY));
        chartCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_USAGE_CHART));
        usagePieChartPanel.setEmptyMessage(Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS));
        dayActivityTimelinePanel.setEmptyMessage(Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS));
        applicationUsageTableModel.retranslate();
        ApplicationUsageTableModel.configureColumnWidths(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        refresh();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(timelinePanel);
        UiTheme.styleSurfaceCard(applicationsPanel);
        UiTheme.styleSurfaceCard(chartPanel);
        UiTheme.styleMutedLabel(workTimeCaptionLabel);
        UiTheme.styleMutedLabel(statusValueLabel);
        UiTheme.styleMutedLabel(timelineCaptionLabel);
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        UiTheme.styleMutedLabel(chartCaptionLabel);
        UiTheme.styleTimerLabel(workTimeValueLabel);
        UiTheme.styleUsageTable(applicationUsageTable);
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
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

        List<ApplicationUsageGroup> applicationUsageGroups = ApplicationUsageFilter.groupMinorApplicationGroups(
                ApplicationUsageBrowserGrouper.group(statisticsService.buildTodayApplicationUsage()),
                userSettings.getMinorUsageThresholdMinutes()
        );
        List<ApplicationUsageSummary> pieChartSummaries = applicationUsageGroups.stream()
                .map(ApplicationUsageGroup::toSummary)
                .collect(Collectors.toList());
        applicationUsageTableModel.setGroups(applicationUsageGroups, todayActiveSeconds);
        usagePieChartPanel.setUsageData(pieChartSummaries, todayActiveSeconds);
        boolean timelineVisible = userSettings.isTimelineVisible();
        timelinePanel.setVisible(timelineVisible);
        if (timelineVisible) {
            dayActivityTimelinePanel.setTimeline(statisticsService.buildTodayActivityTimeline());
        }
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        revalidate();
    }

    private void onStartPauseClicked() {
        if (trackingEngine.isTrackingEnabled()) {
            trackingEngine.pauseTracking();
        } else {
            trackingEngine.startTracking();
        }
        refresh();
    }
}
