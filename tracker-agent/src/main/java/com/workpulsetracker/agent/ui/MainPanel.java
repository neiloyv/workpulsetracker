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
import com.workpulsetracker.agent.util.ProgramCategoryDisplayNames;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Главная вкладка: Start/Pause, время работы, таймлайн, таблица и круговая диаграмма за сегодня.
 */
public final class MainPanel extends JPanel {

    private enum UsageChartMode {
        PROGRAMS,
        CATEGORIES,
        ACTIVITY
    }

    private final TrackingEngine trackingEngine;
    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JLabel workTimeCaptionLabel = new JLabel();
    private final JLabel workTimeValueLabel = new JLabel("0:00:00");
    private final JLabel statusValueLabel = new JLabel();
    private final JLabel timelineCaptionLabel = new JLabel();
    private final JLabel timelineLegendActiveLabel = new JLabel();
    private final JLabel timelineLegendIdleLabel = new JLabel();
    private final JLabel timelineLegendExcludedLabel = new JLabel();
    private final JPanel timelineLegendActiveSwatch = createLegendSwatch(DayActivityTimelinePanel.activeColor());
    private final JPanel timelineLegendIdleSwatch = createLegendSwatch(DayActivityTimelinePanel.idleColor());
    private final JPanel timelineLegendExcludedSwatch = createLegendSwatch(DayActivityTimelinePanel.excludedColor());
    private final JPanel timelineLegendExcludedItem = createLegendItem(
            timelineLegendExcludedSwatch,
            timelineLegendExcludedLabel
    );
    private final JLabel applicationsCaptionLabel = new JLabel();
    private final JLabel chartCaptionLabel = new JLabel();
    private final JToggleButton chartProgramsModeButton = new JToggleButton();
    private final JToggleButton chartCategoriesModeButton = new JToggleButton();
    private final JToggleButton chartActivityModeButton = new JToggleButton();
    private final JButton startPauseButton = new JButton();
    private final ApplicationUsageTableModel applicationUsageTableModel = new ApplicationUsageTableModel();
    private final JTable applicationUsageTable = new JTable(applicationUsageTableModel);
    private final ApplicationUsagePieChartPanel usagePieChartPanel = new ApplicationUsagePieChartPanel();
    private final DayActivityTimelinePanel dayActivityTimelinePanel = new DayActivityTimelinePanel();
    private final JPanel timelinePanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel applicationsPanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel chartPanel = new JPanel(new BorderLayout(4, 8));
    private UsageChartMode selectedUsageChartMode = UsageChartMode.PROGRAMS;
    private boolean suppressChartModeChangeEvents;

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
        timelinePanel.add(createTimelineLegendPanel(), BorderLayout.SOUTH);

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

        ButtonGroup chartModeButtonGroup = new ButtonGroup();
        chartModeButtonGroup.add(chartProgramsModeButton);
        chartModeButtonGroup.add(chartCategoriesModeButton);
        chartModeButtonGroup.add(chartActivityModeButton);
        chartProgramsModeButton.setSelected(true);
        chartProgramsModeButton.addActionListener(actionEvent -> onUsageChartModeSelected(UsageChartMode.PROGRAMS));
        chartCategoriesModeButton.addActionListener(actionEvent -> onUsageChartModeSelected(UsageChartMode.CATEGORIES));
        chartActivityModeButton.addActionListener(actionEvent -> onUsageChartModeSelected(UsageChartMode.ACTIVITY));
        UiTheme.styleSegmentedToggleButton(chartProgramsModeButton);
        UiTheme.styleSegmentedToggleButton(chartCategoriesModeButton);
        UiTheme.styleSegmentedToggleButton(chartActivityModeButton);

        JPanel chartModePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        chartModePanel.setOpaque(false);
        chartModePanel.add(chartProgramsModeButton);
        chartModePanel.add(chartCategoriesModeButton);
        chartModePanel.add(chartActivityModeButton);

        JPanel chartHeaderPanel = new JPanel(new BorderLayout(8, 0));
        chartHeaderPanel.setOpaque(false);
        chartHeaderPanel.add(chartCaptionLabel, BorderLayout.WEST);
        chartHeaderPanel.add(chartModePanel, BorderLayout.EAST);

        UiTheme.styleSurfaceCard(chartPanel);
        UiTheme.styleMutedLabel(chartCaptionLabel);
        chartPanel.add(chartHeaderPanel, BorderLayout.NORTH);
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

    private JPanel createTimelineLegendPanel() {
        UiTheme.styleMutedLabel(timelineLegendActiveLabel);
        UiTheme.styleMutedLabel(timelineLegendIdleLabel);
        UiTheme.styleMutedLabel(timelineLegendExcludedLabel);

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        legendPanel.setOpaque(false);
        legendPanel.add(createLegendItem(timelineLegendActiveSwatch, timelineLegendActiveLabel));
        legendPanel.add(createLegendItem(timelineLegendIdleSwatch, timelineLegendIdleLabel));
        legendPanel.add(timelineLegendExcludedItem);
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

    private static String formatTimelineLegendText(String stateName, String durationText, int percentage) {
        return stateName + ": " + durationText + " (" + percentage + "%)";
    }

    public void retranslate() {
        workTimeCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_WORK_TIME));
        timelineCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_TODAY));
        applicationsCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_APPLICATIONS_TODAY));
        chartCaptionLabel.setText(Messages.get(MessageCodes.UI_MAIN_USAGE_CHART));
        chartProgramsModeButton.setText(Messages.get(MessageCodes.UI_MAIN_CHART_MODE_PROGRAMS));
        chartCategoriesModeButton.setText(Messages.get(MessageCodes.UI_MAIN_CHART_MODE_CATEGORIES));
        chartActivityModeButton.setText(Messages.get(MessageCodes.UI_MAIN_CHART_MODE_ACTIVITY));
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
        UiTheme.styleSegmentedToggleButton(chartProgramsModeButton);
        UiTheme.styleSegmentedToggleButton(chartCategoriesModeButton);
        UiTheme.styleSegmentedToggleButton(chartActivityModeButton);
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
        long todayIdleSeconds = statisticsService.buildTodayIdleSeconds();
        long todayExcludedSeconds = statisticsService.buildTodayExcludedSeconds();
        boolean showExceptionsOnTimeline = userSettings.isShowExceptionsOnTimeline();
        long todayComputerSeconds = todayActiveSeconds + todayIdleSeconds + todayExcludedSeconds;
        int activePercentage = PercentageCalculator.calculatePercentage(todayActiveSeconds, todayComputerSeconds);
        int idlePercentage = PercentageCalculator.calculatePercentage(todayIdleSeconds, todayComputerSeconds);
        int excludedPercentage = todayComputerSeconds <= 0L
                ? 0
                : Math.max(0, 100 - activePercentage - idlePercentage);

        workTimeValueLabel.setText(DurationFormatter.formatSeconds(todayActiveSeconds));
        timelineLegendActiveLabel.setText(formatTimelineLegendText(
                Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE),
                DurationFormatter.formatSeconds(todayActiveSeconds),
                activePercentage
        ));
        timelineLegendIdleLabel.setText(formatTimelineLegendText(
                Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE),
                DurationFormatter.formatSeconds(todayIdleSeconds),
                idlePercentage
        ));
        timelineLegendExcludedLabel.setText(formatTimelineLegendText(
                Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED),
                DurationFormatter.formatSeconds(todayExcludedSeconds),
                excludedPercentage
        ));
        timelineLegendExcludedItem.setVisible(showExceptionsOnTimeline);

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

        List<ApplicationUsageSummary> todayApplicationUsageSummaries = statisticsService.buildTodayApplicationUsage();
        List<ApplicationUsageGroup> applicationUsageGroups = ApplicationUsageFilter.groupMinorApplicationGroups(
                ApplicationUsageBrowserGrouper.group(todayApplicationUsageSummaries),
                userSettings.getMinorUsageThresholdMinutes()
        );
        applicationUsageTableModel.setGroups(applicationUsageGroups, todayActiveSeconds);
        updateUsageChart(
                applicationUsageGroups,
                todayApplicationUsageSummaries,
                todayActiveSeconds,
                todayIdleSeconds,
                todayExcludedSeconds
        );
        suppressChartModeChangeEvents = true;
        try {
            chartProgramsModeButton.setSelected(selectedUsageChartMode == UsageChartMode.PROGRAMS);
            chartCategoriesModeButton.setSelected(selectedUsageChartMode == UsageChartMode.CATEGORIES);
            chartActivityModeButton.setSelected(selectedUsageChartMode == UsageChartMode.ACTIVITY);
        } finally {
            suppressChartModeChangeEvents = false;
        }
        boolean timelineVisible = userSettings.isTimelineVisible();
        timelinePanel.setVisible(timelineVisible);
        if (timelineVisible) {
            dayActivityTimelinePanel.setTimeline(statisticsService.buildTodayActivityTimeline());
        }
        ApplicationUsageTableModel.configureColumnAlignment(applicationUsageTable);
        revalidate();
    }

    private void updateUsageChart(
            List<ApplicationUsageGroup> applicationUsageGroups,
            List<ApplicationUsageSummary> todayApplicationUsageSummaries,
            long todayActiveSeconds,
            long todayIdleSeconds,
            long todayExcludedSeconds
    ) {
        switch (selectedUsageChartMode) {
            case PROGRAMS -> {
                List<ApplicationUsageSummary> programChartSummaries = applicationUsageGroups.stream()
                        .map(ApplicationUsageGroup::toSummary)
                        .collect(Collectors.toList());
                usagePieChartPanel.setUsageData(programChartSummaries, todayActiveSeconds);
            }
            case CATEGORIES -> usagePieChartPanel.setUsageData(
                    buildCategoryChartSummaries(todayApplicationUsageSummaries),
                    todayActiveSeconds,
                    null,
                    false
            );
            case ACTIVITY -> {
                List<ApplicationUsageSummary> activityChartSummaries = new ArrayList<>();
                List<Color> activitySliceColors = new ArrayList<>();
                if (todayActiveSeconds > 0L) {
                    activityChartSummaries.add(new ApplicationUsageSummary(
                            Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE),
                            todayActiveSeconds
                    ));
                    activitySliceColors.add(DayActivityTimelinePanel.activeColor());
                }
                if (todayIdleSeconds > 0L) {
                    activityChartSummaries.add(new ApplicationUsageSummary(
                            Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE),
                            todayIdleSeconds
                    ));
                    activitySliceColors.add(DayActivityTimelinePanel.idleColor());
                }
                if (userSettings.isShowExceptionsOnTimeline() && todayExcludedSeconds > 0L) {
                    activityChartSummaries.add(new ApplicationUsageSummary(
                            Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED),
                            todayExcludedSeconds
                    ));
                    activitySliceColors.add(DayActivityTimelinePanel.excludedColor());
                }
                long activityTotalSeconds = todayActiveSeconds + todayIdleSeconds
                        + (userSettings.isShowExceptionsOnTimeline() ? todayExcludedSeconds : 0L);
                usagePieChartPanel.setUsageData(
                        activityChartSummaries,
                        activityTotalSeconds,
                        activitySliceColors,
                        false
                );
            }
        }
    }

    private List<ApplicationUsageSummary> buildCategoryChartSummaries(
            List<ApplicationUsageSummary> applicationUsageSummaries
    ) {
        return applicationUsageSummaries.stream()
                .collect(Collectors.groupingBy(
                        applicationUsageSummary -> userSettings.getApplicationCategoryId(
                                applicationUsageSummary.getApplicationName()
                        ),
                        Collectors.summingLong(ApplicationUsageSummary::getDurationSeconds)
                ))
                .entrySet().stream()
                .filter(categoryDurationEntry -> categoryDurationEntry.getValue() > 0L)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(categoryDurationEntry -> new ApplicationUsageSummary(
                        ProgramCategoryDisplayNames.resolveDisplayName(categoryDurationEntry.getKey()),
                        categoryDurationEntry.getValue()
                ))
                .collect(Collectors.toList());
    }

    private void onUsageChartModeSelected(UsageChartMode usageChartMode) {
        if (suppressChartModeChangeEvents) {
            return;
        }
        selectedUsageChartMode = usageChartMode;
        refresh();
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
