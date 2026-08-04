package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.mode.AgentFeature;
import com.workpulsetracker.agent.mode.FeatureGateService;
import com.workpulsetracker.agent.report.StatisticsExcelReportWriter;
import com.workpulsetracker.agent.report.StatisticsPdfReportWriter;
import com.workpulsetracker.agent.report.StatisticsReportFormat;
import com.workpulsetracker.agent.stats.ApplicationUsageBrowserGrouper;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.stats.StatisticsSnapshot;
import com.workpulsetracker.agent.stats.StatsPeriod;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Вкладка локальной статистики: период, таблица с закреплёнными колонками, выгрузка отчёта.
 */
public final class StatisticsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsPanel.class);
    private static final int FROZEN_COLUMN_COUNT = 2;
    private static final int APPLICATION_COLUMN_PREFERRED_WIDTH = 180;
    private static final int TOTAL_COLUMN_PREFERRED_WIDTH = 120;
    private static final int PERIOD_COLUMN_PREFERRED_WIDTH = 110;

    private final StatisticsService statisticsService;
    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final FeatureGateService featureGateService;

    private final JComboBox<StatsPeriodItem> periodComboBox = new JComboBox<>();
    private final JLabel periodLabel = new JLabel();
    private final JLabel totalCaptionLabel = new JLabel();
    private final JLabel totalTimeValueLabel = new JLabel("0:00");
    private final JLabel tableCaptionLabel = new JLabel();
    private final JLabel reportFormatLabel = new JLabel();
    private final JLabel dateFromLabel = new JLabel();
    private final JLabel dateToLabel = new JLabel();
    private final JComboBox<ReportFormatItem> reportFormatComboBox = new JComboBox<>();
    private final JButton downloadReportButton = new JButton();
    private final JSpinner dateFromSpinner = createDateSpinner(LocalDate.now().minusWeeks(2));
    private final JSpinner dateToSpinner = createDateSpinner(LocalDate.now());
    private final JPanel headerCard = new JPanel(new BorderLayout(8, 10));
    private final JPanel customRangePanel = new JPanel(new BorderLayout(12, 0));
    private final JPanel tablePanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel footerCard = new JPanel(new BorderLayout(12, 0));
    private final ApplicationUsageMatrixTableModel statisticsTableModel = new ApplicationUsageMatrixTableModel();
    private final JTable statisticsTable = new JTable(statisticsTableModel);
    private JScrollPane statisticsScrollPane;
    private boolean suppressPeriodChangeEvents;

    public StatisticsPanel(
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore
    ) {
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.featureGateService = new FeatureGateService(userSettings);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        refresh();
    }

    private void buildContent() {
        rebuildPeriodOptions(false);
        periodComboBox.addActionListener(actionEvent -> onPeriodSelectionChanged());

        dateFromSpinner.addChangeListener(changeEvent -> {
            if (isCustomPeriodSelected()) {
                refresh();
            }
        });
        dateToSpinner.addChangeListener(changeEvent -> {
            if (isCustomPeriodSelected()) {
                refresh();
            }
        });

        UiTheme.styleSurfaceCard(headerCard);

        JPanel periodBlock = new JPanel(new BorderLayout(8, 0));
        periodBlock.setOpaque(false);
        UiTheme.styleMutedLabel(periodLabel);
        periodBlock.add(periodLabel, BorderLayout.WEST);
        periodBlock.add(periodComboBox, BorderLayout.CENTER);

        JPanel totalBlock = new JPanel(new BorderLayout(8, 0));
        totalBlock.setOpaque(false);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        totalTimeValueLabel.setFont(totalTimeValueLabel.getFont().deriveFont(Font.BOLD, 20f));
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        totalTimeValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalBlock.add(totalCaptionLabel, BorderLayout.WEST);
        totalBlock.add(totalTimeValueLabel, BorderLayout.CENTER);

        JPanel topRow = new JPanel(new GridLayout(1, 2, 16, 0));
        topRow.setOpaque(false);
        topRow.add(periodBlock);
        topRow.add(totalBlock);

        customRangePanel.setOpaque(false);
        customRangePanel.setVisible(false);
        customRangePanel.setLayout(new GridLayout(1, 2, 16, 0));
        UiTheme.styleMutedLabel(dateFromLabel);
        UiTheme.styleMutedLabel(dateToLabel);

        JPanel fromPanel = new JPanel(new BorderLayout(8, 0));
        fromPanel.setOpaque(false);
        fromPanel.add(dateFromLabel, BorderLayout.WEST);
        fromPanel.add(dateFromSpinner, BorderLayout.CENTER);

        JPanel toPanel = new JPanel(new BorderLayout(8, 0));
        toPanel.setOpaque(false);
        toPanel.add(dateToLabel, BorderLayout.WEST);
        toPanel.add(dateToSpinner, BorderLayout.CENTER);

        customRangePanel.add(fromPanel);
        customRangePanel.add(toPanel);

        JPanel headerCenterPanel = new JPanel(new BorderLayout(0, 10));
        headerCenterPanel.setOpaque(false);
        headerCenterPanel.add(topRow, BorderLayout.NORTH);
        headerCenterPanel.add(customRangePanel, BorderLayout.SOUTH);

        headerCard.add(headerCenterPanel, BorderLayout.CENTER);

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
        tablePanel.add(tableCaptionLabel, BorderLayout.NORTH);
        tablePanel.add(statisticsScrollPane, BorderLayout.CENTER);

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
        add(tablePanel, BorderLayout.CENTER);
        add(footerCard, BorderLayout.SOUTH);
        retranslate();
    }

    private void onPeriodSelectionChanged() {
        if (suppressPeriodChangeEvents) {
            return;
        }
        customRangePanel.setVisible(isCustomPeriodSelected());
        revalidate();
        refresh();
    }

    private boolean isCustomPeriodSelected() {
        StatsPeriodItem selectedPeriodItem = (StatsPeriodItem) periodComboBox.getSelectedItem();
        return Objects.nonNull(selectedPeriodItem) && selectedPeriodItem.statsPeriod() == StatsPeriod.CUSTOM;
    }

    private void rebuildPeriodOptions(boolean preserveSelection) {
        StatsPeriod previouslySelectedPeriod = null;
        if (preserveSelection) {
            StatsPeriodItem selectedPeriodItem = (StatsPeriodItem) periodComboBox.getSelectedItem();
            if (Objects.nonNull(selectedPeriodItem)) {
                previouslySelectedPeriod = selectedPeriodItem.statsPeriod();
            }
        }

        suppressPeriodChangeEvents = true;
        try {
            periodComboBox.removeAllItems();
            periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.WEEK, MessageCodes.UI_STATS_PERIOD_WEEK));
            periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.MONTH, MessageCodes.UI_STATS_PERIOD_MONTH));
            boolean extendedHistoryAllowed = featureGateService.isFeatureAllowed(AgentFeature.EXTENDED_HISTORY_EXPORT);
            if (extendedHistoryAllowed) {
                periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.YEAR, MessageCodes.UI_STATS_PERIOD_YEAR));
                periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.ALL_TIME, MessageCodes.UI_STATS_PERIOD_ALL));
            }
            periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.CUSTOM, MessageCodes.UI_STATS_PERIOD_CUSTOM));

            if (Objects.nonNull(previouslySelectedPeriod)) {
                for (int itemIndex = 0; itemIndex < periodComboBox.getItemCount(); itemIndex++) {
                    StatsPeriodItem statsPeriodItem = periodComboBox.getItemAt(itemIndex);
                    if (Objects.equals(statsPeriodItem.statsPeriod(), previouslySelectedPeriod)) {
                        periodComboBox.setSelectedIndex(itemIndex);
                        customRangePanel.setVisible(isCustomPeriodSelected());
                        return;
                    }
                }
            }
            periodComboBox.setSelectedIndex(0);
            customRangePanel.setVisible(false);
        } finally {
            suppressPeriodChangeEvents = false;
        }
    }

    /**
     * Пересобирает периоды после смены LOCAL_SOLO / NETWORK_SYNC.
     */
    public void onOperationModeChanged() {
        rebuildPeriodOptions(true);
        refresh();
    }

    public void retranslate() {
        periodLabel.setText(Messages.get(MessageCodes.UI_STATS_PERIOD));
        totalCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_TOTAL));
        tableCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_BY_APP));
        reportFormatLabel.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_FORMAT));
        downloadReportButton.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT));
        dateFromLabel.setText(Messages.get(MessageCodes.UI_STATS_PERIOD_FROM));
        dateToLabel.setText(Messages.get(MessageCodes.UI_STATS_PERIOD_TO));
        rebuildPeriodOptions(true);
        periodComboBox.repaint();
        reportFormatComboBox.repaint();
        statisticsTableModel.retranslate();
        refresh();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(headerCard);
        UiTheme.styleSurfaceCard(tablePanel);
        UiTheme.styleSurfaceCard(footerCard);
        UiTheme.styleMutedLabel(periodLabel);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        UiTheme.styleMutedLabel(tableCaptionLabel);
        UiTheme.styleMutedLabel(reportFormatLabel);
        UiTheme.styleMutedLabel(dateFromLabel);
        UiTheme.styleMutedLabel(dateToLabel);
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleSecondaryButton(downloadReportButton);
        UiTheme.styleUsageTable(statisticsTable);
        statisticsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        if (Objects.nonNull(statisticsScrollPane)) {
            statisticsScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        }
        refresh();
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        StatsPeriodItem selectedPeriodItem = (StatsPeriodItem) periodComboBox.getSelectedItem();
        StatsPeriod statsPeriod = selectedPeriodItem != null
                ? selectedPeriodItem.statsPeriod()
                : StatsPeriod.WEEK;
        if ((statsPeriod == StatsPeriod.YEAR || statsPeriod == StatsPeriod.ALL_TIME)
                && !featureGateService.isFeatureAllowed(AgentFeature.EXTENDED_HISTORY_EXPORT)) {
            rebuildPeriodOptions(false);
            statsPeriod = StatsPeriod.WEEK;
        }
        LocalDate rangeStartDate = readSpinnerDate(dateFromSpinner);
        LocalDate rangeEndDate = readSpinnerDate(dateToSpinner);

        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshot(
                statsPeriod,
                rangeStartDate,
                rangeEndDate
        );
        totalTimeValueLabel.setText(
                DurationFormatter.formatHoursMinutes(statisticsSnapshot.getTotalActiveSeconds())
        );

        ApplicationUsageMatrix applicationUsageMatrix = ApplicationUsageFilter.groupMinorApplications(
                ApplicationUsageBrowserGrouper.collapseBrowserApplications(
                        statisticsService.buildApplicationUsageMatrix(statsPeriod, rangeStartDate, rangeEndDate)
                ),
                userSettings.getMinorUsageThresholdMinutes()
        );
        statisticsTableModel.setMatrix(applicationUsageMatrix);
        rebuildFrozenColumnsScrollPane();
    }

    private void rebuildFrozenColumnsScrollPane() {
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

    /**
     * Растягивает колонки на всю ширину viewport, если места больше «базовой» ширины.
     * Если колонок много — остаётся горизонтальный скролл.
     */
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

        int[] baseWidths = new int[columnCount];
        int totalBaseWidth = 0;
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int baseWidth;
            if (frozenMode) {
                baseWidth = PERIOD_COLUMN_PREFERRED_WIDTH;
            } else if (columnIndex == 0) {
                baseWidth = APPLICATION_COLUMN_PREFERRED_WIDTH;
            } else if (columnIndex == 1) {
                baseWidth = TOTAL_COLUMN_PREFERRED_WIDTH;
            } else {
                baseWidth = PERIOD_COLUMN_PREFERRED_WIDTH;
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
        DefaultTableCellRenderer totalColumnRenderer = createCenteredBoldCapableRenderer(true);
        DefaultTableCellRenderer leftHeaderRenderer = createHeaderRenderer(SwingConstants.LEFT);
        DefaultTableCellRenderer centerHeaderRenderer = createHeaderRenderer(SwingConstants.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(applicationCellRenderer);
        table.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setCellRenderer(totalColumnRenderer);
            table.getColumnModel().getColumn(1).setHeaderRenderer(centerHeaderRenderer);
        }
        for (int columnIndex = 2; columnIndex < table.getColumnModel().getColumnCount(); columnIndex++) {
            table.getColumnModel().getColumn(columnIndex).setCellRenderer(createCenteredBoldCapableRenderer(false));
            table.getColumnModel().getColumn(columnIndex).setHeaderRenderer(centerHeaderRenderer);
        }
    }

    private void configureStatisticsColumns(JTable table) {
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(APPLICATION_COLUMN_PREFERRED_WIDTH);
        table.getColumnModel().getColumn(0).setMinWidth(140);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(TOTAL_COLUMN_PREFERRED_WIDTH);
            table.getColumnModel().getColumn(1).setMinWidth(100);
        }
        for (int columnIndex = 2; columnIndex < table.getColumnModel().getColumnCount(); columnIndex++) {
            table.getColumnModel().getColumn(columnIndex).setPreferredWidth(PERIOD_COLUMN_PREFERRED_WIDTH);
            table.getColumnModel().getColumn(columnIndex).setMinWidth(88);
        }
    }

    private void configureFrozenTableRenderers(JTable frozenTable) {
        DefaultTableCellRenderer applicationCellRenderer = new ApplicationNameCellRenderer();
        DefaultTableCellRenderer totalColumnRenderer = createCenteredBoldCapableRenderer(true);
        DefaultTableCellRenderer leftHeaderRenderer = createHeaderRenderer(SwingConstants.LEFT);
        DefaultTableCellRenderer centerHeaderRenderer = createHeaderRenderer(SwingConstants.CENTER);

        frozenTable.getColumnModel().getColumn(0).setCellRenderer(applicationCellRenderer);
        frozenTable.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);
        if (frozenTable.getColumnModel().getColumnCount() > 1) {
            frozenTable.getColumnModel().getColumn(1).setCellRenderer(totalColumnRenderer);
            frozenTable.getColumnModel().getColumn(1).setHeaderRenderer(centerHeaderRenderer);
        }
    }

    private void configureScrollableTableRenderers(JTable scrollableTable) {
        DefaultTableCellRenderer centeredCellRenderer = createCenteredBoldCapableRenderer(false);
        DefaultTableCellRenderer centerHeaderRenderer = createHeaderRenderer(SwingConstants.CENTER);
        for (int columnIndex = 0; columnIndex < scrollableTable.getColumnModel().getColumnCount(); columnIndex++) {
            scrollableTable.getColumnModel().getColumn(columnIndex).setCellRenderer(centeredCellRenderer);
            scrollableTable.getColumnModel().getColumn(columnIndex).setHeaderRenderer(centerHeaderRenderer);
        }
    }

    private DefaultTableCellRenderer createCenteredBoldCapableRenderer(boolean alwaysBold) {
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
                }
                return component;
            }
        };
        centeredCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centeredCellRenderer.setVerticalAlignment(SwingConstants.CENTER);
        return centeredCellRenderer;
    }

    private DefaultTableCellRenderer createHeaderRenderer(int horizontalAlignment) {
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(horizontalAlignment);
        headerRenderer.setBackground(UiTheme.SURFACE_2);
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
            if (reportFormat == StatisticsReportFormat.PDF) {
                new StatisticsPdfReportWriter(
                        statisticsService,
                        userSettings.getMinorUsageThresholdMinutes()
                ).writeToFile(reportFilePath);
            } else {
                new StatisticsExcelReportWriter(
                        statisticsService,
                        userSettings.getMinorUsageThresholdMinutes()
                ).writeToFile(reportFilePath);
            }
            rememberReportDirectory(reportFilePath);
            UiDialogs.showMessage(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_SUCCESS),
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.error("Failed to export statistics report: {}", exception.getMessage(), exception);
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
        return "WorkPulseTracker-report-" + dateSuffix + "." + reportFormat.getFileExtension();
    }

    private static JSpinner createDateSpinner(LocalDate localDate) {
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        SpinnerDateModel spinnerDateModel = new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(spinnerDateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        dateSpinner.setMaximumSize(new Dimension(140, 32));
        dateSpinner.setPreferredSize(new Dimension(140, 32));
        return dateSpinner;
    }

    private static LocalDate readSpinnerDate(JSpinner dateSpinner) {
        Object spinnerValue = dateSpinner.getValue();
        if (!(spinnerValue instanceof Date dateValue)) {
            return LocalDate.now();
        }
        return dateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private record StatsPeriodItem(StatsPeriod statsPeriod, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }

    private record ReportFormatItem(StatisticsReportFormat reportFormat, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
