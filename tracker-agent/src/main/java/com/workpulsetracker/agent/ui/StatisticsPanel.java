package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.report.StatisticsExcelReportWriter;
import com.workpulsetracker.agent.report.StatisticsPdfReportWriter;
import com.workpulsetracker.agent.report.StatisticsReportFormat;
import com.workpulsetracker.agent.stats.ApplicationUsageBrowserGrouper;
import com.workpulsetracker.agent.stats.ApplicationUsageCategoryAggregator;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.DayActivityState;
import com.workpulsetracker.agent.stats.DayActivityTimeline;
import com.workpulsetracker.agent.stats.DayActivityTimelineRow;
import com.workpulsetracker.agent.stats.DayActivityTimelineSegment;
import com.workpulsetracker.agent.stats.PeriodBucket;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.stats.StatisticsSnapshot;
import com.workpulsetracker.agent.stats.StatsPeriod;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.agent.util.PercentageCalculator;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Вкладка локальной статистики: Week/Month/Year, матрица приложений и таймлайн по дням.
 */
public final class StatisticsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsPanel.class);
    private static final int FROZEN_COLUMN_COUNT = 2;
    private static final int APPLICATION_COLUMN_PREFERRED_WIDTH = 180;
    private static final int TOTAL_COLUMN_PREFERRED_WIDTH = 120;
    private static final int PERIOD_COLUMN_PREFERRED_WIDTH = 110;
    private static final int TIMELINE_DAY_CAPTION_SIDE_INSET = 8;
    private static final int TIMELINE_SUMMARY_METRIC_COLUMN_WIDTH = 48;
    private static final int TIMELINE_SUMMARY_COLUMN_GAP = 6;
    private static final String CONTENT_CARD_MATRIX = "matrix";
    private static final String CONTENT_CARD_TIMELINE = "timeline";
    private static final String BODY_CARD_DATA = "data";
    private static final String BODY_CARD_EMPTY = "empty";

    private enum StatisticsViewMode {
        MATRIX,
        CATEGORIES,
        TIMELINE
    }

    private final StatisticsService statisticsService;
    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;

    private final JToggleButton weekPeriodButton = new JToggleButton();
    private final JToggleButton monthPeriodButton = new JToggleButton();
    private final JToggleButton yearPeriodButton = new JToggleButton();
    private final JToggleButton matrixViewButton = new JToggleButton();
    private final JToggleButton categoriesViewButton = new JToggleButton();
    private final JToggleButton timelineViewButton = new JToggleButton();
    private final JButton previousPeriodButton = new JButton("‹");
    private final JButton nextPeriodButton = new JButton("›");
    private final JButton previousWeekInMonthButton = new JButton("‹");
    private final JButton nextWeekInMonthButton = new JButton("›");
    private final JLabel periodCaptionLabel = new JLabel();
    private final JLabel weekInMonthCaptionLabel = new JLabel();
    private final JPanel weekInMonthNavigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel totalCaptionLabel = new JLabel();
    private final JLabel totalTimeValueLabel = new JLabel("0:00");
    private final JLabel tableCaptionLabel = new JLabel();
    private final JLabel matrixEmptyLabel = new JLabel();
    private final JLabel timelineEmptyLabel = new JLabel();
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
    private final JLabel reportFormatLabel = new JLabel();
    private final JComboBox<ReportFormatItem> reportFormatComboBox = new JComboBox<>();
    private final JButton downloadReportButton = new JButton();
    private final JPanel headerCard = new JPanel(new BorderLayout(8, 10));
    private final JPanel tablePanel = new JPanel(new BorderLayout(4, 8));
    private final CardLayout matrixBodyCardLayout = new CardLayout();
    private final JPanel matrixBodyPanel = new JPanel(matrixBodyCardLayout);
    private final JPanel timelineViewPanel = new JPanel(new BorderLayout(4, 8));
    private final CardLayout timelineBodyCardLayout = new CardLayout();
    private final JPanel timelineBodyPanel = new JPanel(timelineBodyCardLayout);
    private final JPanel timelineRowsPanel = new JPanel();
    private final JScrollPane timelineScrollPane = new JScrollPane(timelineRowsPanel);
    private final CardLayout contentCardLayout = new CardLayout();
    private final JPanel contentCardPanel = new JPanel(contentCardLayout);
    private final JPanel footerCard = new JPanel(new BorderLayout(12, 0));
    private final ApplicationUsageMatrixTableModel statisticsTableModel = new ApplicationUsageMatrixTableModel();
    private final JTable statisticsTable = new JTable(statisticsTableModel);
    private JScrollPane statisticsScrollPane;

    private StatsPeriod selectedStatsPeriod = StatsPeriod.WEEK;
    private StatisticsViewMode selectedViewMode = StatisticsViewMode.MATRIX;
    private LocalDate periodAnchorDate = LocalDate.now();
    private LocalDate monthWeekAnchorDate = LocalDate.now();
    private int currentPeriodBucketIndex = -1;
    private boolean suppressPeriodChangeEvents;
    private boolean suppressViewChangeEvents;

    public StatisticsPanel(
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore
    ) {
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
    }

    private void buildContent() {
        ButtonGroup periodButtonGroup = new ButtonGroup();
        periodButtonGroup.add(weekPeriodButton);
        periodButtonGroup.add(monthPeriodButton);
        periodButtonGroup.add(yearPeriodButton);
        weekPeriodButton.setSelected(true);

        ButtonGroup viewButtonGroup = new ButtonGroup();
        viewButtonGroup.add(matrixViewButton);
        viewButtonGroup.add(categoriesViewButton);
        viewButtonGroup.add(timelineViewButton);
        matrixViewButton.setSelected(true);

        weekPeriodButton.addActionListener(actionEvent -> onPeriodModeSelected(StatsPeriod.WEEK));
        monthPeriodButton.addActionListener(actionEvent -> onPeriodModeSelected(StatsPeriod.MONTH));
        yearPeriodButton.addActionListener(actionEvent -> onPeriodModeSelected(StatsPeriod.YEAR));
        matrixViewButton.addActionListener(actionEvent -> onViewModeSelected(StatisticsViewMode.MATRIX));
        categoriesViewButton.addActionListener(actionEvent -> onViewModeSelected(StatisticsViewMode.CATEGORIES));
        timelineViewButton.addActionListener(actionEvent -> onViewModeSelected(StatisticsViewMode.TIMELINE));
        previousPeriodButton.addActionListener(actionEvent -> navigatePeriod(-1));
        nextPeriodButton.addActionListener(actionEvent -> navigatePeriod(1));
        previousWeekInMonthButton.addActionListener(actionEvent -> navigateWeekInMonth(-1));
        nextWeekInMonthButton.addActionListener(actionEvent -> navigateWeekInMonth(1));

        stylePeriodModeButton(weekPeriodButton);
        stylePeriodModeButton(monthPeriodButton);
        stylePeriodModeButton(yearPeriodButton);
        stylePeriodModeButton(matrixViewButton);
        stylePeriodModeButton(categoriesViewButton);
        stylePeriodModeButton(timelineViewButton);
        UiTheme.styleCompactSecondaryButton(previousPeriodButton);
        UiTheme.styleCompactSecondaryButton(nextPeriodButton);
        UiTheme.styleCompactSecondaryButton(previousWeekInMonthButton);
        UiTheme.styleCompactSecondaryButton(nextWeekInMonthButton);
        previousPeriodButton.setPreferredSize(new Dimension(40, 32));
        nextPeriodButton.setPreferredSize(new Dimension(40, 32));
        previousWeekInMonthButton.setPreferredSize(new Dimension(40, 32));
        nextWeekInMonthButton.setPreferredSize(new Dimension(40, 32));

        UiTheme.styleSurfaceCard(headerCard);

        JPanel viewModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        viewModePanel.setOpaque(false);
        viewModePanel.add(matrixViewButton);
        viewModePanel.add(categoriesViewButton);
        viewModePanel.add(timelineViewButton);

        JPanel periodModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodModePanel.setOpaque(false);
        periodModePanel.add(weekPeriodButton);
        periodModePanel.add(monthPeriodButton);
        periodModePanel.add(yearPeriodButton);

        JPanel periodNavigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodNavigationPanel.setOpaque(false);
        periodNavigationPanel.add(previousPeriodButton);
        periodCaptionLabel.setFont(periodCaptionLabel.getFont().deriveFont(Font.BOLD, 15f));
        periodCaptionLabel.setForeground(UiTheme.TEXT_PRIMARY);
        periodNavigationPanel.add(periodCaptionLabel);
        periodNavigationPanel.add(nextPeriodButton);

        weekInMonthNavigationPanel.setOpaque(false);
        weekInMonthNavigationPanel.setVisible(false);
        weekInMonthNavigationPanel.add(previousWeekInMonthButton);
        weekInMonthCaptionLabel.setFont(weekInMonthCaptionLabel.getFont().deriveFont(Font.PLAIN, 14f));
        weekInMonthCaptionLabel.setForeground(UiTheme.TEXT_PRIMARY);
        weekInMonthNavigationPanel.add(weekInMonthCaptionLabel);
        weekInMonthNavigationPanel.add(nextWeekInMonthButton);

        JPanel periodFiltersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        periodFiltersPanel.setOpaque(false);
        periodFiltersPanel.add(periodModePanel);
        periodFiltersPanel.add(periodNavigationPanel);
        periodFiltersPanel.add(weekInMonthNavigationPanel);

        JPanel leftHeaderPanel = new JPanel();
        leftHeaderPanel.setOpaque(false);
        leftHeaderPanel.setLayout(new BoxLayout(leftHeaderPanel, BoxLayout.X_AXIS));
        viewModePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        periodFiltersPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        leftHeaderPanel.add(viewModePanel);
        leftHeaderPanel.add(Box.createHorizontalStrut(50));
        leftHeaderPanel.add(periodFiltersPanel);

        JPanel totalBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        totalBlock.setOpaque(false);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        totalTimeValueLabel.setFont(totalTimeValueLabel.getFont().deriveFont(Font.BOLD, 20f));
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        totalTimeValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalBlock.add(totalCaptionLabel);
        totalBlock.add(totalTimeValueLabel);

        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);
        topRow.add(leftHeaderPanel, BorderLayout.WEST);
        topRow.add(totalBlock, BorderLayout.EAST);
        headerCard.add(topRow, BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(tablePanel);
        UiTheme.styleMutedLabel(tableCaptionLabel);
        UiTheme.styleUsageTable(statisticsTable);
        statisticsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        statisticsScrollPane = new JScrollPane(statisticsTable);
        statisticsScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        statisticsScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                stretchColumnsToViewport();
            }
        });
        matrixBodyPanel.setOpaque(false);
        matrixBodyPanel.add(statisticsScrollPane, BODY_CARD_DATA);
        matrixBodyPanel.add(createEmptyStatePanel(matrixEmptyLabel), BODY_CARD_EMPTY);
        tablePanel.add(tableCaptionLabel, BorderLayout.NORTH);
        tablePanel.add(matrixBodyPanel, BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(timelineViewPanel);
        timelineRowsPanel.setOpaque(false);
        timelineRowsPanel.setLayout(new BoxLayout(timelineRowsPanel, BoxLayout.Y_AXIS));
        timelineScrollPane.setBorder(BorderFactory.createEmptyBorder());
        timelineScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        timelineScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        timelineBodyPanel.setOpaque(false);
        timelineBodyPanel.add(timelineScrollPane, BODY_CARD_DATA);
        timelineBodyPanel.add(createEmptyStatePanel(timelineEmptyLabel), BODY_CARD_EMPTY);
        timelineViewPanel.add(timelineBodyPanel, BorderLayout.CENTER);
        timelineViewPanel.add(createTimelineLegendPanel(), BorderLayout.SOUTH);

        contentCardPanel.setOpaque(false);
        contentCardPanel.add(tablePanel, CONTENT_CARD_MATRIX);
        contentCardPanel.add(timelineViewPanel, CONTENT_CARD_TIMELINE);

        downloadReportButton.addActionListener(actionEvent -> onDownloadReportClicked());
        UiTheme.styleSecondaryButton(downloadReportButton);
        reportFormatComboBox.addItem(new ReportFormatItem(
                StatisticsReportFormat.EXCEL,
                MessageCodes.UI_STATS_DOWNLOAD_FORMAT_EXCEL
        ));
        reportFormatComboBox.addItem(new ReportFormatItem(
                StatisticsReportFormat.PDF,
                MessageCodes.UI_STATS_DOWNLOAD_FORMAT_PDF
        ));
        reportFormatComboBox.setSelectedIndex(0);

        UiTheme.styleSurfaceCard(footerCard);
        UiTheme.styleMutedLabel(reportFormatLabel);
        JPanel reportFormatRow = new JPanel(new BorderLayout(8, 0));
        reportFormatRow.setOpaque(false);
        reportFormatRow.add(reportFormatLabel, BorderLayout.WEST);
        reportFormatRow.add(reportFormatComboBox, BorderLayout.CENTER);
        footerCard.add(reportFormatRow, BorderLayout.CENTER);
        footerCard.add(downloadReportButton, BorderLayout.EAST);

        add(headerCard, BorderLayout.NORTH);
        add(contentCardPanel, BorderLayout.CENTER);
        add(footerCard, BorderLayout.SOUTH);
        retranslate();
    }

    private void stylePeriodModeButton(JToggleButton toggleButton) {
        UiTheme.styleSegmentedToggleButton(toggleButton);
    }

    private void onPeriodModeSelected(StatsPeriod statsPeriod) {
        if (suppressPeriodChangeEvents) {
            return;
        }
        selectedStatsPeriod = statsPeriod;
        periodAnchorDate = statisticsService.normalizeAnchorDate(statsPeriod, LocalDate.now());
        if (statsPeriod == StatsPeriod.MONTH) {
            YearMonth yearMonth = YearMonth.from(periodAnchorDate);
            monthWeekAnchorDate = statisticsService.resolveDefaultWeekMondayForMonth(yearMonth);
        }
        refresh();
    }

    private void onViewModeSelected(StatisticsViewMode statisticsViewMode) {
        if (suppressViewChangeEvents) {
            return;
        }
        selectedViewMode = statisticsViewMode;
        if (selectedViewMode == StatisticsViewMode.TIMELINE && selectedStatsPeriod == StatsPeriod.YEAR) {
            selectedStatsPeriod = StatsPeriod.WEEK;
            periodAnchorDate = statisticsService.normalizeAnchorDate(StatsPeriod.WEEK, LocalDate.now());
        }
        refresh();
    }

    private void navigatePeriod(int periodOffset) {
        LocalDate nextAnchorDate = statisticsService.shiftAnchorDate(
                selectedStatsPeriod,
                periodAnchorDate,
                periodOffset
        );
        if (periodOffset > 0
                && !statisticsService.canNavigateToNextPeriod(selectedStatsPeriod, periodAnchorDate)) {
            return;
        }
        if (periodOffset < 0
                && !statisticsService.canNavigateToPreviousPeriod(selectedStatsPeriod, periodAnchorDate)) {
            return;
        }
        periodAnchorDate = statisticsService.normalizeAnchorDate(selectedStatsPeriod, nextAnchorDate);
        if (selectedStatsPeriod == StatsPeriod.MONTH) {
            YearMonth yearMonth = YearMonth.from(periodAnchorDate);
            monthWeekAnchorDate = statisticsService.resolveDefaultWeekMondayForMonth(yearMonth);
        }
        refresh();
    }

    private void navigateWeekInMonth(int weekOffset) {
        if (selectedStatsPeriod != StatsPeriod.MONTH) {
            return;
        }
        YearMonth yearMonth = YearMonth.from(periodAnchorDate);
        if (weekOffset > 0
                && !statisticsService.canNavigateToNextWeekInMonth(yearMonth, monthWeekAnchorDate)) {
            return;
        }
        if (weekOffset < 0
                && !statisticsService.canNavigateToPreviousWeekInMonth(yearMonth, monthWeekAnchorDate)) {
            return;
        }
        monthWeekAnchorDate = statisticsService.shiftWeekMondayInMonth(
                yearMonth,
                monthWeekAnchorDate,
                weekOffset
        );
        refresh();
    }

    /**
     * После смены режима работы — перерисовка (Year теперь доступен всегда).
     */
    public void onOperationModeChanged() {
        refresh();
    }

    public void retranslate() {
        weekPeriodButton.setText(Messages.get(MessageCodes.UI_STATS_PERIOD_WEEK));
        monthPeriodButton.setText(Messages.get(MessageCodes.UI_STATS_PERIOD_MONTH));
        yearPeriodButton.setText(Messages.get(MessageCodes.UI_STATS_PERIOD_YEAR));
        matrixViewButton.setText(Messages.get(MessageCodes.UI_STATS_VIEW_MATRIX));
        categoriesViewButton.setText(Messages.get(MessageCodes.UI_STATS_VIEW_CATEGORIES));
        timelineViewButton.setText(Messages.get(MessageCodes.UI_STATS_VIEW_TIMELINE));
        previousPeriodButton.setToolTipText(Messages.get(MessageCodes.UI_STATS_PERIOD_PREVIOUS));
        nextPeriodButton.setToolTipText(Messages.get(MessageCodes.UI_STATS_PERIOD_NEXT));
        previousWeekInMonthButton.setToolTipText(Messages.get(MessageCodes.UI_STATS_PERIOD_PREVIOUS_WEEK));
        nextWeekInMonthButton.setToolTipText(Messages.get(MessageCodes.UI_STATS_PERIOD_NEXT_WEEK));
        totalCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_TOTAL));
        updateTableCaptionForSelectedView();
        matrixEmptyLabel.setText(Messages.get(MessageCodes.UI_STATS_EMPTY));
        timelineEmptyLabel.setText(Messages.get(MessageCodes.UI_STATS_EMPTY));
        timelineLegendActiveLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE));
        timelineLegendIdleLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE));
        timelineLegendExcludedLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED));
        timelineLegendExcludedItem.setVisible(userSettings.isShowExceptionsOnTimeline());
        reportFormatLabel.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_FORMAT));
        downloadReportButton.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT));
        reportFormatComboBox.repaint();
        statisticsTableModel.retranslate();
        refresh();
        if (isTableStyleView() && !statisticsTableModel.isEmpty()) {
            rebuildFrozenColumnsScrollPane();
        }
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(headerCard);
        UiTheme.styleSurfaceCard(tablePanel);
        UiTheme.styleSurfaceCard(timelineViewPanel);
        UiTheme.styleSurfaceCard(footerCard);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        UiTheme.styleMutedLabel(tableCaptionLabel);
        UiTheme.styleMutedLabel(matrixEmptyLabel);
        UiTheme.styleMutedLabel(timelineEmptyLabel);
        UiTheme.styleMutedLabel(timelineLegendActiveLabel);
        UiTheme.styleMutedLabel(timelineLegendIdleLabel);
        UiTheme.styleMutedLabel(timelineLegendExcludedLabel);
        UiTheme.styleMutedLabel(reportFormatLabel);
        periodCaptionLabel.setForeground(UiTheme.TEXT_PRIMARY);
        weekInMonthCaptionLabel.setForeground(UiTheme.TEXT_PRIMARY);
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        stylePeriodModeButton(weekPeriodButton);
        stylePeriodModeButton(monthPeriodButton);
        stylePeriodModeButton(yearPeriodButton);
        stylePeriodModeButton(matrixViewButton);
        stylePeriodModeButton(categoriesViewButton);
        stylePeriodModeButton(timelineViewButton);
        UiTheme.styleCompactSecondaryButton(previousPeriodButton);
        UiTheme.styleCompactSecondaryButton(nextPeriodButton);
        UiTheme.styleCompactSecondaryButton(previousWeekInMonthButton);
        UiTheme.styleCompactSecondaryButton(nextWeekInMonthButton);
        UiTheme.styleSecondaryButton(downloadReportButton);
        UiTheme.styleUsageTable(statisticsTable);
        statisticsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        if (Objects.nonNull(statisticsScrollPane)) {
            statisticsScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        }
        timelineScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        refresh();
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        timelineLegendExcludedItem.setVisible(userSettings.isShowExceptionsOnTimeline());

        if (selectedViewMode == StatisticsViewMode.TIMELINE && selectedStatsPeriod == StatsPeriod.YEAR) {
            selectedStatsPeriod = StatsPeriod.WEEK;
            periodAnchorDate = statisticsService.normalizeAnchorDate(StatsPeriod.WEEK, periodAnchorDate);
        }

        periodAnchorDate = statisticsService.normalizeAnchorDate(selectedStatsPeriod, periodAnchorDate);
        suppressPeriodChangeEvents = true;
        suppressViewChangeEvents = true;
        try {
            weekPeriodButton.setSelected(selectedStatsPeriod == StatsPeriod.WEEK);
            monthPeriodButton.setSelected(selectedStatsPeriod == StatsPeriod.MONTH);
            yearPeriodButton.setSelected(selectedStatsPeriod == StatsPeriod.YEAR);
            matrixViewButton.setSelected(selectedViewMode == StatisticsViewMode.MATRIX);
            categoriesViewButton.setSelected(selectedViewMode == StatisticsViewMode.CATEGORIES);
            timelineViewButton.setSelected(selectedViewMode == StatisticsViewMode.TIMELINE);
        } finally {
            suppressPeriodChangeEvents = false;
            suppressViewChangeEvents = false;
        }

        yearPeriodButton.setVisible(isTableStyleView());

        boolean monthMode = selectedStatsPeriod == StatsPeriod.MONTH;
        weekInMonthNavigationPanel.setVisible(monthMode);

        periodCaptionLabel.setText(
                statisticsService.formatPeriodCaption(selectedStatsPeriod, periodAnchorDate)
        );
        previousPeriodButton.setEnabled(
                statisticsService.canNavigateToPreviousPeriod(selectedStatsPeriod, periodAnchorDate)
        );
        nextPeriodButton.setEnabled(
                statisticsService.canNavigateToNextPeriod(selectedStatsPeriod, periodAnchorDate)
        );

        StatsPeriod matrixStatsPeriod = selectedStatsPeriod;
        LocalDate matrixAnchorDate = periodAnchorDate;
        if (monthMode) {
            YearMonth yearMonth = YearMonth.from(periodAnchorDate);
            if (!statisticsService.weekIntersectsMonth(monthWeekAnchorDate, yearMonth)) {
                monthWeekAnchorDate = statisticsService.resolveDefaultWeekMondayForMonth(yearMonth);
            }
            monthWeekAnchorDate = statisticsService.normalizeAnchorDate(StatsPeriod.WEEK, monthWeekAnchorDate);
            weekInMonthCaptionLabel.setText(statisticsService.formatWeekRangeCaption(monthWeekAnchorDate));
            previousWeekInMonthButton.setEnabled(
                    statisticsService.canNavigateToPreviousWeekInMonth(yearMonth, monthWeekAnchorDate)
            );
            nextWeekInMonthButton.setEnabled(
                    statisticsService.canNavigateToNextWeekInMonth(yearMonth, monthWeekAnchorDate)
            );
            matrixStatsPeriod = StatsPeriod.WEEK;
            matrixAnchorDate = monthWeekAnchorDate;
        }

        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshotForAnchor(
                matrixStatsPeriod,
                matrixAnchorDate
        );
        totalTimeValueLabel.setText(
                DurationFormatter.formatHoursMinutes(statisticsSnapshot.getTotalActiveSeconds())
        );

        if (selectedViewMode == StatisticsViewMode.TIMELINE) {
            contentCardLayout.show(contentCardPanel, CONTENT_CARD_TIMELINE);
            if (statisticsSnapshot.getTotalActiveSeconds() <= 0L) {
                timelineRowsPanel.removeAll();
                timelineBodyCardLayout.show(timelineBodyPanel, BODY_CARD_EMPTY);
            } else {
                rebuildTimelineRows(matrixAnchorDate);
                timelineBodyCardLayout.show(timelineBodyPanel, BODY_CARD_DATA);
            }
        } else {
            contentCardLayout.show(contentCardPanel, CONTENT_CARD_MATRIX);
            ApplicationUsageMatrix sourceApplicationUsageMatrix =
                    statisticsService.buildApplicationUsageMatrixForAnchor(matrixStatsPeriod, matrixAnchorDate);
            ApplicationUsageMatrix applicationUsageMatrix = selectedViewMode == StatisticsViewMode.CATEGORIES
                    ? ApplicationUsageCategoryAggregator.aggregateByCategories(
                            sourceApplicationUsageMatrix,
                            userSettings
                    )
                    : ApplicationUsageFilter.groupMinorApplications(
                            ApplicationUsageBrowserGrouper.collapseBrowserApplications(sourceApplicationUsageMatrix),
                            userSettings.getMinorUsageThresholdMinutes()
                    );
            updateTableCaptionForSelectedView();
            boolean firstColumnNameChanged = statisticsTableModel.setFirstColumnName(
                    selectedViewMode == StatisticsViewMode.CATEGORIES
                            ? Messages.get(MessageCodes.UI_PROGRAMS_COLUMN_CATEGORY)
                            : Messages.get(MessageCodes.UI_TABLE_APPLICATION)
            );
            currentPeriodBucketIndex = findCurrentPeriodBucketIndex(applicationUsageMatrix);
            boolean structureChanged = statisticsTableModel.setMatrix(applicationUsageMatrix);
            if (applicationUsageMatrix.getApplicationNames().isEmpty()
                    || applicationUsageMatrix.getTotalActiveSeconds() <= 0L) {
                tableCaptionLabel.setVisible(false);
                matrixBodyCardLayout.show(matrixBodyPanel, BODY_CARD_EMPTY);
            } else {
                tableCaptionLabel.setVisible(true);
                if (structureChanged
                        || firstColumnNameChanged
                        || !isFrozenColumnsLayoutReady(applicationUsageMatrix)) {
                    rebuildFrozenColumnsScrollPane();
                }
                matrixBodyCardLayout.show(matrixBodyPanel, BODY_CARD_DATA);
            }
        }
        revalidate();
        repaint();
    }

    private boolean isTableStyleView() {
        return selectedViewMode == StatisticsViewMode.MATRIX
                || selectedViewMode == StatisticsViewMode.CATEGORIES;
    }

    private void updateTableCaptionForSelectedView() {
        tableCaptionLabel.setText(
                selectedViewMode == StatisticsViewMode.CATEGORIES
                        ? Messages.get(MessageCodes.UI_STATS_BY_CATEGORY)
                        : Messages.get(MessageCodes.UI_STATS_BY_APP)
        );
    }

    private boolean isFrozenColumnsLayoutReady(ApplicationUsageMatrix applicationUsageMatrix) {
        int columnCount = 2 + applicationUsageMatrix.getPeriodBuckets().size();
        if (columnCount <= FROZEN_COLUMN_COUNT) {
            return Objects.isNull(statisticsScrollPane.getRowHeader())
                    || Objects.isNull(statisticsScrollPane.getRowHeader().getView());
        }
        return Objects.nonNull(statisticsScrollPane.getRowHeader())
                && Objects.nonNull(statisticsScrollPane.getRowHeader().getView())
                && statisticsTable.getColumnModel().getColumnCount() == columnCount - FROZEN_COLUMN_COUNT;
    }

    private int findCurrentPeriodBucketIndex(ApplicationUsageMatrix applicationUsageMatrix) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        YearMonth currentYearMonth = YearMonth.from(today);
        StatsPeriod statsPeriod = applicationUsageMatrix.getStatsPeriod();
        List<PeriodBucket> periodBuckets = applicationUsageMatrix.getPeriodBuckets();
        return IntStream.range(0, periodBuckets.size())
                .filter(bucketIndex -> {
                    LocalDate bucketStartDate = LocalDate.ofInstant(
                            periodBuckets.get(bucketIndex).getStartInclusive(),
                            zoneId
                    );
                    if (statsPeriod == StatsPeriod.YEAR) {
                        return YearMonth.from(bucketStartDate).equals(currentYearMonth);
                    }
                    return bucketStartDate.equals(today);
                })
                .findFirst()
                .orElse(-1);
    }

    private void rebuildTimelineRows(LocalDate weekAnchorDate) {
        timelineRowsPanel.removeAll();
        List<DayActivityTimelineRow> timelineRows = statisticsService.buildWeekActivityTimelines(weekAnchorDate);
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("d MMMM", locale);
        int dayLabelColumnWidth = resolveTimelineDayLabelColumnWidth(timelineRows, dayDateFormatter, locale);
        timelineRows.forEach(timelineRow -> {
            timelineRowsPanel.add(createTimelineDayRow(
                    timelineRow,
                    dayDateFormatter,
                    locale,
                    dayLabelColumnWidth
            ));
            timelineRowsPanel.add(Box.createVerticalStrut(6));
        });
        timelineRowsPanel.revalidate();
        timelineRowsPanel.repaint();
    }

    private static int resolveTimelineDayLabelColumnWidth(
            List<DayActivityTimelineRow> timelineRows,
            DateTimeFormatter dayDateFormatter,
            Locale locale
    ) {
        return timelineRows.stream()
                .mapToInt(timelineRow -> {
                    String dayDateText = dayDateFormatter.format(timelineRow.getDayDate());
                    String dayOfWeekShortName = timelineRow.getDayDate()
                            .getDayOfWeek()
                            .getDisplayName(TextStyle.SHORT, locale);
                    JLabel probeLabel = new JLabel(dayDateText + " (" + dayOfWeekShortName + ")");
                    probeLabel.setFont(probeLabel.getFont().deriveFont(Font.BOLD, 12f));
                    return probeLabel.getPreferredSize().width + TIMELINE_DAY_CAPTION_SIDE_INSET * 2;
                })
                .max()
                .orElse(120);
    }

    private JPanel createTimelineDayRow(
            DayActivityTimelineRow timelineRow,
            DateTimeFormatter dayDateFormatter,
            Locale locale,
            int dayLabelColumnWidth
    ) {
        String dayDateText = dayDateFormatter.format(timelineRow.getDayDate());
        String dayOfWeekShortName = timelineRow.getDayDate()
                .getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, locale);
        JLabel dayCaptionLabel = new JLabel(dayDateText + " (" + dayOfWeekShortName + ")");
        dayCaptionLabel.setFont(dayCaptionLabel.getFont().deriveFont(Font.BOLD, 12f));
        dayCaptionLabel.setForeground(UiTheme.TEXT_PRIMARY);

        int trackAreaHeight = DayActivityTimelinePanel.trackAreaHeight();
        int hourLabelAreaHeight = DayActivityTimelinePanel.hourLabelAreaHeight();
        int timelineGap = 8;

        JPanel dayLabelPanel = new JPanel(new GridBagLayout());
        dayLabelPanel.setOpaque(false);
        dayLabelPanel.setBorder(BorderFactory.createEmptyBorder(
                0,
                TIMELINE_DAY_CAPTION_SIDE_INSET,
                0,
                TIMELINE_DAY_CAPTION_SIDE_INSET
        ));
        dayLabelPanel.add(dayCaptionLabel);

        Dimension trackAlignedSize = new Dimension(dayLabelColumnWidth, trackAreaHeight);
        dayLabelPanel.setPreferredSize(trackAlignedSize);
        dayLabelPanel.setMinimumSize(trackAlignedSize);
        dayLabelPanel.setMaximumSize(trackAlignedSize);
        dayLabelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel dayLabelColumn = new JPanel();
        dayLabelColumn.setOpaque(false);
        dayLabelColumn.setLayout(new BoxLayout(dayLabelColumn, BoxLayout.Y_AXIS));
        dayLabelColumn.add(dayLabelPanel);
        dayLabelColumn.add(Box.createVerticalStrut(hourLabelAreaHeight));
        dayLabelColumn.setPreferredSize(new Dimension(dayLabelColumnWidth, trackAreaHeight + hourLabelAreaHeight));
        dayLabelColumn.setMinimumSize(new Dimension(dayLabelColumnWidth, trackAreaHeight + hourLabelAreaHeight));
        dayLabelColumn.setMaximumSize(new Dimension(dayLabelColumnWidth, trackAreaHeight + hourLabelAreaHeight));

        JPanel daySummaryColumn = createTimelineDaySummaryColumn(
                timelineRow.getDayActivityTimeline(),
                trackAreaHeight,
                hourLabelAreaHeight
        );

        JPanel leftColumnsPanel = new JPanel();
        leftColumnsPanel.setOpaque(false);
        leftColumnsPanel.setLayout(new BoxLayout(leftColumnsPanel, BoxLayout.X_AXIS));
        leftColumnsPanel.add(dayLabelColumn);
        leftColumnsPanel.add(Box.createHorizontalStrut(8));
        leftColumnsPanel.add(daySummaryColumn);

        DayActivityTimelinePanel dayActivityTimelinePanel = new DayActivityTimelinePanel();
        dayActivityTimelinePanel.setShowHourLabels(true);
        dayActivityTimelinePanel.setTimeline(timelineRow.getDayActivityTimeline());
        dayActivityTimelinePanel.setAlignmentY(Component.TOP_ALIGNMENT);

        boolean isTodayRow = timelineRow.getDayDate().equals(LocalDate.now());
        int rowHeight = trackAreaHeight + hourLabelAreaHeight + 8;
        JPanel rowPanel = new JPanel(new BorderLayout(timelineGap, 0));
        rowPanel.setOpaque(isTodayRow);
        if (isTodayRow) {
            rowPanel.setBackground(UiTheme.CURRENT_PERIOD_HIGHLIGHT);
            rowPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiTheme.BORDER),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
        } else {
            rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        rowPanel.add(leftColumnsPanel, BorderLayout.WEST);
        rowPanel.add(dayActivityTimelinePanel, BorderLayout.CENTER);
        return rowPanel;
    }

    private JPanel createTimelineDaySummaryColumn(
            DayActivityTimeline dayActivityTimeline,
            int trackAreaHeight,
            int hourLabelAreaHeight
    ) {
        boolean showExceptionsOnTimeline = userSettings.isShowExceptionsOnTimeline();
        long activeSeconds = sumTimelineSecondsByState(dayActivityTimeline, DayActivityState.ACTIVE);
        long idleSeconds = sumTimelineSecondsByState(dayActivityTimeline, DayActivityState.IDLE);
        long excludedSeconds = showExceptionsOnTimeline
                ? sumTimelineSecondsByState(dayActivityTimeline, DayActivityState.EXCLUDED)
                : 0L;
        long computerSeconds = activeSeconds + idleSeconds + excludedSeconds;
        int activePercentage = PercentageCalculator.calculatePercentage(activeSeconds, computerSeconds);
        int idlePercentage = PercentageCalculator.calculatePercentage(idleSeconds, computerSeconds);
        int excludedPercentage = computerSeconds <= 0L
                ? 0
                : Math.max(0, 100 - activePercentage - idlePercentage);

        JPanel metricsPanel = new JPanel();
        metricsPanel.setOpaque(false);
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.X_AXIS));
        metricsPanel.add(createTimelineDayMetricColumn(
                DurationFormatter.formatHoursMinutes(activeSeconds),
                activePercentage,
                DayActivityTimelinePanel.activeColor()
        ));
        metricsPanel.add(Box.createHorizontalStrut(TIMELINE_SUMMARY_COLUMN_GAP));
        metricsPanel.add(createTimelineSummaryVerticalSeparator());
        metricsPanel.add(Box.createHorizontalStrut(TIMELINE_SUMMARY_COLUMN_GAP));
        metricsPanel.add(createTimelineDayMetricColumn(
                DurationFormatter.formatHoursMinutes(idleSeconds),
                idlePercentage,
                DayActivityTimelinePanel.idleColor()
        ));
        if (showExceptionsOnTimeline) {
            metricsPanel.add(Box.createHorizontalStrut(TIMELINE_SUMMARY_COLUMN_GAP));
            metricsPanel.add(createTimelineSummaryVerticalSeparator());
            metricsPanel.add(Box.createHorizontalStrut(TIMELINE_SUMMARY_COLUMN_GAP));
            metricsPanel.add(createTimelineDayMetricColumn(
                    DurationFormatter.formatHoursMinutes(excludedSeconds),
                    excludedPercentage,
                    DayActivityTimelinePanel.excludedColor()
            ));
        }

        JPanel summaryTrackPanel = new JPanel(new GridBagLayout());
        summaryTrackPanel.setOpaque(false);
        summaryTrackPanel.add(metricsPanel);

        int metricColumnCount = showExceptionsOnTimeline ? 3 : 2;
        int separatorCount = metricColumnCount - 1;
        int summaryWidth = TIMELINE_SUMMARY_METRIC_COLUMN_WIDTH * metricColumnCount
                + TIMELINE_SUMMARY_COLUMN_GAP * (separatorCount * 2)
                + separatorCount;
        Dimension trackAlignedSize = new Dimension(summaryWidth, trackAreaHeight);
        summaryTrackPanel.setPreferredSize(trackAlignedSize);
        summaryTrackPanel.setMinimumSize(trackAlignedSize);
        summaryTrackPanel.setMaximumSize(trackAlignedSize);

        JPanel summaryColumn = new JPanel();
        summaryColumn.setOpaque(false);
        summaryColumn.setLayout(new BoxLayout(summaryColumn, BoxLayout.Y_AXIS));
        summaryColumn.add(summaryTrackPanel);
        summaryColumn.add(Box.createVerticalStrut(hourLabelAreaHeight));
        summaryColumn.setPreferredSize(new Dimension(summaryWidth, trackAreaHeight + hourLabelAreaHeight));
        summaryColumn.setMinimumSize(new Dimension(summaryWidth, trackAreaHeight + hourLabelAreaHeight));
        summaryColumn.setMaximumSize(new Dimension(summaryWidth, trackAreaHeight + hourLabelAreaHeight));
        return summaryColumn;
    }

    private static JPanel createTimelineDayMetricColumn(
            String durationText,
            int percentage,
            Color foregroundColor
    ) {
        JLabel durationLabel = new JLabel(durationText);
        JLabel percentLabel = new JLabel(percentage + "%");
        styleTimelineDayMetricLabel(durationLabel, foregroundColor, Font.BOLD, 12f);
        styleTimelineDayMetricLabel(percentLabel, foregroundColor, Font.PLAIN, 11f);
        durationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel metricColumnPanel = new JPanel();
        metricColumnPanel.setOpaque(false);
        metricColumnPanel.setLayout(new BoxLayout(metricColumnPanel, BoxLayout.Y_AXIS));
        metricColumnPanel.add(durationLabel);
        metricColumnPanel.add(percentLabel);

        Dimension columnSize = new Dimension(TIMELINE_SUMMARY_METRIC_COLUMN_WIDTH, metricColumnPanel.getPreferredSize().height);
        metricColumnPanel.setPreferredSize(columnSize);
        metricColumnPanel.setMinimumSize(columnSize);
        metricColumnPanel.setMaximumSize(columnSize);
        return metricColumnPanel;
    }

    private static long sumTimelineSecondsByState(
            DayActivityTimeline dayActivityTimeline,
            DayActivityState activityState
    ) {
        return dayActivityTimeline.getSegments().stream()
                .filter(segment -> segment.getActivityState() == activityState)
                .mapToLong(DayActivityTimelineSegment::getDurationSeconds)
                .sum();
    }

    private static void styleTimelineDayMetricLabel(
            JLabel metricLabel,
            Color foregroundColor,
            int fontStyle,
            float fontSize
    ) {
        metricLabel.setForeground(foregroundColor);
        metricLabel.setFont(metricLabel.getFont().deriveFont(fontStyle, fontSize));
        metricLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private int resolvePeriodColumnPreferredWidth() {
        return PERIOD_COLUMN_PREFERRED_WIDTH;
    }

    private static JPanel createEmptyStatePanel(JLabel emptyLabel) {
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setVerticalAlignment(SwingConstants.CENTER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 15f));
        UiTheme.styleMutedLabel(emptyLabel);

        JPanel emptyStatePanel = new JPanel(new GridBagLayout());
        emptyStatePanel.setOpaque(false);
        emptyStatePanel.add(emptyLabel);
        return emptyStatePanel;
    }

    private JPanel createTimelineLegendPanel() {
        UiTheme.styleMutedLabel(timelineLegendActiveLabel);
        UiTheme.styleMutedLabel(timelineLegendIdleLabel);
        UiTheme.styleMutedLabel(timelineLegendExcludedLabel);

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        legendPanel.add(createLegendItem(timelineLegendActiveSwatch, timelineLegendActiveLabel));
        legendPanel.add(createLegendItem(timelineLegendIdleSwatch, timelineLegendIdleLabel));
        legendPanel.add(timelineLegendExcludedItem);
        timelineLegendExcludedItem.setVisible(userSettings.isShowExceptionsOnTimeline());
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

    private static JPanel createTimelineSummaryVerticalSeparator() {
        JPanel separatorPanel = new JPanel();
        separatorPanel.setOpaque(true);
        separatorPanel.setBackground(UiTheme.BORDER);
        Dimension separatorSize = new Dimension(1, 28);
        separatorPanel.setPreferredSize(separatorSize);
        separatorPanel.setMinimumSize(separatorSize);
        separatorPanel.setMaximumSize(separatorSize);
        return separatorPanel;
    }

    private void rebuildFrozenColumnsScrollPane() {
        // После предыдущего split в таблице остаются только «плавающие» колонки —
        // сначала восстанавливаем полный набор из модели.
        statisticsTable.setAutoCreateColumnsFromModel(true);
        statisticsTable.setColumnModel(new DefaultTableColumnModel());
        statisticsTable.createDefaultColumnsFromModel();
        configureStatisticsColumns(statisticsTable);
        if (statisticsTable.getColumnModel().getColumnCount() <= FROZEN_COLUMN_COUNT) {
            configureNonFrozenTableRenderers(statisticsTable);
            statisticsScrollPane.setViewportView(statisticsTable);
            statisticsScrollPane.setRowHeaderView(null);
            statisticsScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
            SwingUtilities.invokeLater(this::stretchColumnsToViewport);
            return;
        }

        TableColumnModel scrollableColumnModel = new DefaultTableColumnModel();
        TableColumnModel frozenColumnModel = new DefaultTableColumnModel();
        TableColumnModel sourceColumnModel = statisticsTable.getColumnModel();

        while (sourceColumnModel.getColumnCount() > 0) {
            TableColumn tableColumn = sourceColumnModel.getColumn(0);
            sourceColumnModel.removeColumn(tableColumn);
            if (frozenColumnModel.getColumnCount() < FROZEN_COLUMN_COUNT) {
                frozenColumnModel.addColumn(tableColumn);
            } else {
                scrollableColumnModel.addColumn(tableColumn);
            }
        }

        JTable frozenTable = new JTable(statisticsTableModel, frozenColumnModel);
        frozenTable.setSelectionModel(statisticsTable.getSelectionModel());
        frozenTable.setRowHeight(statisticsTable.getRowHeight());
        frozenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UiTheme.styleUsageTable(frozenTable);
        configureFrozenTableRenderers(frozenTable);

        statisticsTable.setColumnModel(scrollableColumnModel);
        configureScrollableTableRenderers(statisticsTable);

        int frozenWidth = 0;
        for (int columnIndex = 0; columnIndex < frozenTable.getColumnCount(); columnIndex++) {
            frozenWidth += frozenTable.getColumnModel().getColumn(columnIndex).getPreferredWidth();
        }
        frozenTable.setPreferredScrollableViewportSize(new Dimension(frozenWidth, 0));

        JTableHeader frozenTableHeader = frozenTable.getTableHeader();
        frozenTableHeader.setReorderingAllowed(false);
        frozenTableHeader.setResizingAllowed(false);

        statisticsScrollPane.setViewportView(statisticsTable);
        statisticsScrollPane.setRowHeaderView(frozenTable);
        statisticsScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, frozenTableHeader);
        statisticsScrollPane.getRowHeader().setPreferredSize(new Dimension(frozenWidth, 0));
        SwingUtilities.invokeLater(this::stretchColumnsToViewport);
    }

    private void stretchColumnsToViewport() {
        if (Objects.isNull(statisticsScrollPane)) {
            return;
        }
        int availableWidth = statisticsScrollPane.getViewport().getWidth();
        if (availableWidth <= 0) {
            return;
        }
        TableColumnModel columnModel = statisticsTable.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        if (columnCount == 0) {
            return;
        }

        boolean frozenMode = Objects.nonNull(statisticsScrollPane.getRowHeader())
                && Objects.nonNull(statisticsScrollPane.getRowHeader().getView());
        int periodColumnPreferredWidth = resolvePeriodColumnPreferredWidth();

        int[] baseWidths = new int[columnCount];
        int totalBaseWidth = 0;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int baseWidth;
            if (frozenMode) {
                baseWidth = periodColumnPreferredWidth;
            } else if (columnIndex == 0) {
                baseWidth = APPLICATION_COLUMN_PREFERRED_WIDTH;
            } else if (columnIndex == 1) {
                baseWidth = TOTAL_COLUMN_PREFERRED_WIDTH;
            } else {
                baseWidth = periodColumnPreferredWidth;
            }
            baseWidths[columnIndex] = baseWidth;
            totalBaseWidth += baseWidth;
        }

        if (availableWidth <= totalBaseWidth) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                TableColumn tableColumn = columnModel.getColumn(columnIndex);
                tableColumn.setPreferredWidth(baseWidths[columnIndex]);
                tableColumn.setWidth(baseWidths[columnIndex]);
            }
        } else {
            int allocatedWidth = 0;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                int columnWidth;
                if (columnIndex == columnCount - 1) {
                    columnWidth = availableWidth - allocatedWidth;
                } else {
                    columnWidth = (int) Math.round(
                            (double) availableWidth * baseWidths[columnIndex] / totalBaseWidth
                    );
                    allocatedWidth += columnWidth;
                }
                TableColumn tableColumn = columnModel.getColumn(columnIndex);
                tableColumn.setPreferredWidth(columnWidth);
                tableColumn.setWidth(columnWidth);
            }
        }
        statisticsTable.doLayout();
        statisticsTable.getTableHeader().repaint();
    }

    private void configureNonFrozenTableRenderers(JTable table) {
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        DefaultTableCellRenderer applicationCellRenderer = new ApplicationNameCellRenderer();
        DefaultTableCellRenderer totalColumnRenderer = createCenteredBoldCapableRenderer(true, false);
        DefaultTableCellRenderer leftHeaderRenderer = createHeaderRenderer(SwingConstants.LEFT, false);
        DefaultTableCellRenderer centerHeaderRenderer = createHeaderRenderer(SwingConstants.CENTER, false);

        table.getColumnModel().getColumn(0).setCellRenderer(applicationCellRenderer);
        table.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setCellRenderer(totalColumnRenderer);
            table.getColumnModel().getColumn(1).setHeaderRenderer(centerHeaderRenderer);
        }
        for (int columnIndex = 2; columnIndex < table.getColumnModel().getColumnCount(); columnIndex++) {
            int periodBucketIndex = columnIndex - 2;
            boolean highlightCurrentPeriod = periodBucketIndex == currentPeriodBucketIndex;
            table.getColumnModel().getColumn(columnIndex).setCellRenderer(
                    createCenteredBoldCapableRenderer(false, highlightCurrentPeriod)
            );
            table.getColumnModel().getColumn(columnIndex).setHeaderRenderer(
                    createHeaderRenderer(SwingConstants.CENTER, highlightCurrentPeriod)
            );
        }
    }

    private void configureStatisticsColumns(JTable table) {
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        int periodColumnPreferredWidth = resolvePeriodColumnPreferredWidth();
        table.getColumnModel().getColumn(0).setPreferredWidth(APPLICATION_COLUMN_PREFERRED_WIDTH);
        table.getColumnModel().getColumn(0).setMinWidth(140);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(TOTAL_COLUMN_PREFERRED_WIDTH);
            table.getColumnModel().getColumn(1).setMinWidth(100);
        }
        for (int columnIndex = 2; columnIndex < table.getColumnModel().getColumnCount(); columnIndex++) {
            table.getColumnModel().getColumn(columnIndex).setPreferredWidth(periodColumnPreferredWidth);
            table.getColumnModel().getColumn(columnIndex).setMinWidth(72);
        }
    }

    private void configureFrozenTableRenderers(JTable frozenTable) {
        DefaultTableCellRenderer applicationCellRenderer = new ApplicationNameCellRenderer();
        DefaultTableCellRenderer totalColumnRenderer = createCenteredBoldCapableRenderer(true, false);
        DefaultTableCellRenderer leftHeaderRenderer = createHeaderRenderer(SwingConstants.LEFT, false);
        DefaultTableCellRenderer centerHeaderRenderer = createHeaderRenderer(SwingConstants.CENTER, false);

        frozenTable.getColumnModel().getColumn(0).setCellRenderer(applicationCellRenderer);
        frozenTable.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);
        if (frozenTable.getColumnModel().getColumnCount() > 1) {
            frozenTable.getColumnModel().getColumn(1).setCellRenderer(totalColumnRenderer);
            frozenTable.getColumnModel().getColumn(1).setHeaderRenderer(centerHeaderRenderer);
        }
    }

    private void configureScrollableTableRenderers(JTable scrollableTable) {
        for (int columnIndex = 0; columnIndex < scrollableTable.getColumnModel().getColumnCount(); columnIndex++) {
            boolean highlightCurrentPeriod = columnIndex == currentPeriodBucketIndex;
            scrollableTable.getColumnModel().getColumn(columnIndex).setCellRenderer(
                    createCenteredBoldCapableRenderer(false, highlightCurrentPeriod)
            );
            scrollableTable.getColumnModel().getColumn(columnIndex).setHeaderRenderer(
                    createHeaderRenderer(SwingConstants.CENTER, highlightCurrentPeriod)
            );
        }
    }

    private DefaultTableCellRenderer createCenteredBoldCapableRenderer(
            boolean alwaysBold,
            boolean highlightCurrentPeriod
    ) {
        DefaultTableCellRenderer centeredCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );
                if (component instanceof JLabel label) {
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                    boolean totalRow = table.getModel() instanceof ApplicationUsageMatrixTableModel matrixModel
                            && matrixModel.isTotalRow(row);
                    if (alwaysBold || totalRow) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setFont(table.getFont());
                    }
                    if (!isSelected) {
                        label.setOpaque(true);
                        if (highlightCurrentPeriod) {
                            label.setBackground(UiTheme.CURRENT_PERIOD_HIGHLIGHT);
                        } else {
                            label.setBackground(table.getBackground());
                        }
                        label.setForeground(table.getForeground());
                    }
                }
                return component;
            }
        };
        centeredCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centeredCellRenderer.setVerticalAlignment(SwingConstants.CENTER);
        if (highlightCurrentPeriod) {
            centeredCellRenderer.setBackground(UiTheme.CURRENT_PERIOD_HIGHLIGHT);
            centeredCellRenderer.setOpaque(true);
        }
        return centeredCellRenderer;
    }

    private DefaultTableCellRenderer createHeaderRenderer(int horizontalAlignment, boolean highlightCurrentPeriod) {
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );
                component.setBackground(
                        highlightCurrentPeriod
                                ? UiTheme.CURRENT_PERIOD_HIGHLIGHT
                                : UiTheme.SURFACE_2
                );
                component.setForeground(UiTheme.TEXT_SECONDARY);
                if (component instanceof JLabel label) {
                    label.setHorizontalAlignment(horizontalAlignment);
                    label.setOpaque(true);
                }
                return component;
            }
        };
        headerRenderer.setHorizontalAlignment(horizontalAlignment);
        headerRenderer.setBackground(
                highlightCurrentPeriod ? UiTheme.CURRENT_PERIOD_HIGHLIGHT : UiTheme.SURFACE_2
        );
        headerRenderer.setForeground(UiTheme.TEXT_SECONDARY);
        return headerRenderer;
    }

    private void onDownloadReportClicked() {
        ReportFormatItem selectedReportFormatItem = (ReportFormatItem) reportFormatComboBox.getSelectedItem();
        StatisticsReportFormat reportFormat = selectedReportFormatItem != null
                ? selectedReportFormatItem.reportFormat()
                : StatisticsReportFormat.EXCEL;

        File initialDirectory = resolveLastReportDirectory();
        File suggestedFile = new File(initialDirectory, buildDefaultReportFileName(reportFormat));

        JFileChooser fileChooser = new JFileChooser(initialDirectory);
        fileChooser.setDialogTitle(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT));
        fileChooser.setSelectedFile(suggestedFile);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                reportFormat == StatisticsReportFormat.PDF ? "PDF (*.pdf)" : "Excel (*.xlsx)",
                reportFormat.getFileExtension()
        ));
        int chooserResult = fileChooser.showSaveDialog(this);
        if (chooserResult != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (Objects.isNull(selectedFile)) {
            return;
        }
        Path reportFilePath = selectedFile.toPath();
        String expectedExtension = "." + reportFormat.getFileExtension();
        if (!StringUtils.endsWithIgnoreCase(reportFilePath.getFileName().toString(), expectedExtension)) {
            reportFilePath = reportFilePath.resolveSibling(reportFilePath.getFileName() + expectedExtension);
        }

        try {
            StatsPeriod exportStatsPeriod = selectedStatsPeriod == StatsPeriod.MONTH
                    ? StatsPeriod.WEEK
                    : selectedStatsPeriod;
            LocalDate exportAnchorDate = selectedStatsPeriod == StatsPeriod.MONTH
                    ? monthWeekAnchorDate
                    : periodAnchorDate;
            if (reportFormat == StatisticsReportFormat.PDF) {
                new StatisticsPdfReportWriter(
                        statisticsService,
                        userSettings.getMinorUsageThresholdMinutes()
                ).writeSelectedPeriodToFile(reportFilePath, exportStatsPeriod, exportAnchorDate);
            } else {
                new StatisticsExcelReportWriter(
                        statisticsService,
                        userSettings.getMinorUsageThresholdMinutes()
                ).writeSelectedPeriodToFile(reportFilePath, exportStatsPeriod, exportAnchorDate);
            }
            rememberReportDirectory(reportFilePath);
            UiDialogs.showMessage(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_SUCCESS),
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.error("schema={} Failed to export statistics report: {}", "local", exception.getMessage(), exception);
            UiDialogs.showMessage(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private File resolveLastReportDirectory() {
        String lastReportDirectoryPath = userSettings.getLastReportDirectoryPath();
        if (StringUtils.isNotBlank(lastReportDirectoryPath)) {
            File lastReportDirectory = new File(lastReportDirectoryPath);
            if (lastReportDirectory.isDirectory()) {
                return lastReportDirectory;
            }
        }
        return new JFileChooser().getCurrentDirectory();
    }

    private void rememberReportDirectory(Path reportFilePath) {
        Path parentDirectoryPath = reportFilePath.getParent();
        if (Objects.isNull(parentDirectoryPath)) {
            return;
        }
        userSettings.setLastReportDirectoryPath(parentDirectoryPath.toAbsolutePath().toString());
        userSettingsStore.save(userSettings);
    }

    private String buildDefaultReportFileName(StatisticsReportFormat reportFormat) {
        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String periodSuffix = selectedStatsPeriod.name().toLowerCase();
        return "WorkPulseTracker-report-" + periodSuffix + "-" + dateSuffix
                + "." + reportFormat.getFileExtension();
    }

    private record ReportFormatItem(StatisticsReportFormat reportFormat, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
