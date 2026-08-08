package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.stats.ApplicationUsageBrowserGrouper;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageGroup;
import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.agent.util.PercentageCalculator;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
    private final JLabel timelineActiveDurationLabel = new JLabel("0:00:00");
    private final JLabel timelineIdleDurationLabel = new JLabel("0:00:00");
    private final JLabel timelineActivePercentLabel = new JLabel("0%");
    private final JLabel timelineIdlePercentLabel = new JLabel("0%");
    private final JLabel timelineLegendActiveLabel = new JLabel();
    private final JLabel timelineLegendIdleLabel = new JLabel();
    private final JLabel timelineLegendExcludedLabel = new JLabel();
    private final JPanel timelineLegendActiveSwatch = createLegendSwatch(DayActivityTimelinePanel.activeColor());
    private final JPanel timelineLegendIdleSwatch = createLegendSwatch(DayActivityTimelinePanel.idleColor());
    private final JPanel timelineLegendExcludedSwatch = createLegendSwatch(DayActivityTimelinePanel.excludedColor());
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
        styleTimelineSummaryLabels();
        JPanel timelineHeaderPanel = new JPanel(new BorderLayout(12, 0));
        timelineHeaderPanel.setOpaque(false);
        timelineHeaderPanel.add(timelineCaptionLabel, BorderLayout.WEST);
        timelineHeaderPanel.add(createTimelineSummaryPanel(), BorderLayout.CENTER);
        timelineHeaderPanel.add(createTimelineLegendPanel(), BorderLayout.EAST);
        timelinePanel.add(timelineHeaderPanel, BorderLayout.NORTH);
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

    private JPanel createTimelineSummaryPanel() {
        JPanel durationRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        durationRowPanel.setOpaque(false);
        durationRowPanel.add(timelineActiveDurationLabel);
        durationRowPanel.add(createVerticalSeparator());
        durationRowPanel.add(timelineIdleDurationLabel);

        JPanel percentRowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        percentRowPanel.setOpaque(false);
        percentRowPanel.add(timelineActivePercentLabel);
        percentRowPanel.add(createVerticalSeparator());
        percentRowPanel.add(timelineIdlePercentLabel);

        JPanel summaryPanel = new JPanel();
        summaryPanel.setOpaque(false);
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        durationRowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        percentRowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        summaryPanel.add(durationRowPanel);
        summaryPanel.add(percentRowPanel);
        return summaryPanel;
    }

    private JPanel createTimelineLegendPanel() {
        UiTheme.styleMutedLabel(timelineLegendActiveLabel);
        UiTheme.styleMutedLabel(timelineLegendIdleLabel);
        UiTheme.styleMutedLabel(timelineLegendExcludedLabel);

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legendPanel.setOpaque(false);
        legendPanel.add(createLegendItem(timelineLegendActiveSwatch, timelineLegendActiveLabel));
        legendPanel.add(createLegendItem(timelineLegendIdleSwatch, timelineLegendIdleLabel));
        legendPanel.add(createLegendItem(timelineLegendExcludedSwatch, timelineLegendExcludedLabel));
        return legendPanel;
    }

    private static JPanel createLegendItem(JPanel colorSwatch, JLabel captionLabel) {
        JPanel legendItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        legendItemPanel.setOpaque(false);
        legendItemPanel.add(colorSwatch);
        legendItemPanel.add(captionLabel);
        return legendItemPanel;
    }

    private static JPanel createLegendSwatch(Color color) {
        JPanel colorSwatch = new JPanel();
        colorSwatch.setOpaque(true);
        colorSwatch.setBackground(color);
        Dimension swatchSize = new Dimension(12, 12);
        colorSwatch.setPreferredSize(swatchSize);
        colorSwatch.setMinimumSize(swatchSize);
        colorSwatch.setMaximumSize(swatchSize);
        return colorSwatch;
    }

    private static JPanel createVerticalSeparator() {
        JPanel separatorPanel = new JPanel();
        separatorPanel.setOpaque(true);
        separatorPanel.setBackground(UiTheme.BORDER);
        Dimension separatorSize = new Dimension(1, 14);
        separatorPanel.setPreferredSize(separatorSize);
        separatorPanel.setMinimumSize(separatorSize);
        separatorPanel.setMaximumSize(separatorSize);
        return separatorPanel;
    }

    private void styleTimelineSummaryLabels() {
        float captionFontSize = timelineCaptionLabel.getFont().getSize2D();
        styleTimelineMetricLabel(
                timelineActiveDurationLabel,
                DayActivityTimelinePanel.activeColor(),
                Font.BOLD,
                captionFontSize
        );
        styleTimelineMetricLabel(
                timelineIdleDurationLabel,
                DayActivityTimelinePanel.idleColor(),
                Font.BOLD,
                captionFontSize
        );
        styleTimelineMetricLabel(
                timelineActivePercentLabel,
                DayActivityTimelinePanel.activeColor(),
                Font.PLAIN,
                captionFontSize
        );
        styleTimelineMetricLabel(
                timelineIdlePercentLabel,
                DayActivityTimelinePanel.idleColor(),
                Font.PLAIN,
                captionFontSize
        );
    }

    private static void styleTimelineMetricLabel(
            JLabel metricLabel,
            Color foregroundColor,
            int fontStyle,
            float fontSize
    ) {
        metricLabel.setForeground(foregroundColor);
        metricLabel.setFont(metricLabel.getFont().deriveFont(fontStyle, fontSize));
        metricLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public void retranslate() {
        workTimeCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_WORK_TIME));
        timelineCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_TODAY));
        timelineLegendActiveLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE));
        timelineLegendIdleLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE));
        timelineLegendExcludedLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED));
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
        UiTheme.styleMutedLabel(timelineLegendActiveLabel);
        UiTheme.styleMutedLabel(timelineLegendIdleLabel);
        UiTheme.styleMutedLabel(timelineLegendExcludedLabel);
        UiTheme.styleMutedLabel(applicationsCaptionLabel);
        UiTheme.styleMutedLabel(chartCaptionLabel);
        UiTheme.styleTimerLabel(workTimeValueLabel);
        styleTimelineSummaryLabels();
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
        long todayIdleSeconds = statisticsService.buildTodayIdleSeconds();
        long todayComputerSeconds = todayActiveSeconds + todayIdleSeconds;
        int activePercentage = PercentageCalculator.calculatePercentage(todayActiveSeconds, todayComputerSeconds);
        int idlePercentage = todayComputerSeconds <= 0L ? 0 : Math.max(0, 100 - activePercentage);

        workTimeValueLabel.setText(DurationFormatter.formatSeconds(todayActiveSeconds));
        timelineActiveDurationLabel.setText(DurationFormatter.formatSeconds(todayActiveSeconds));
        timelineIdleDurationLabel.setText(DurationFormatter.formatSeconds(todayIdleSeconds));
        timelineActivePercentLabel.setText(activePercentage + "%");
        timelineIdlePercentLabel.setText(idlePercentage + "%");

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

        statusValueLabel.setText(resolveStatusLabelText(trackingEnabled));

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

    private String resolveStatusLabelText(boolean trackingEnabled) {
        if (!trackingEnabled) {
            return Messages.get(MessageCodes.UI_MAIN_STATUS_PAUSED);
        }
        ActivityInterval currentActivityInterval = trackingEngine.getDataBuffer().getCurrentInterval();
        if (Objects.nonNull(currentActivityInterval)
                && !currentActivityInterval.isIdle()
                && !userSettings.isApplicationTracked(currentActivityInterval.getApplicationName())) {
            return Messages.get(MessageCodes.UI_MAIN_STATUS_EXCLUDED);
        }
        return Messages.get(MessageCodes.UI_MAIN_STATUS_RUNNING);
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
