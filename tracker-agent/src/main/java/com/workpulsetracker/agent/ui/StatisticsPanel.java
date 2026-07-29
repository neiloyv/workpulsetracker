package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.report.StatisticsExcelReportWriter;
import com.workpulsetracker.agent.report.StatisticsPdfReportWriter;
import com.workpulsetracker.agent.report.StatisticsReportFormat;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.stats.StatisticsSnapshot;
import com.workpulsetracker.agent.stats.StatsPeriod;
import com.workpulsetracker.agent.storage.UserSettings;
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
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * Вкладка локальной статистики: неделя / месяц / год / всё время.
 */
public final class StatisticsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsPanel.class);

    private final StatisticsService statisticsService;
    private final UserSettings userSettings;

    private final JComboBox<StatsPeriodItem> periodComboBox = new JComboBox<>();
    private final JLabel periodLabel = new JLabel();
    private final JLabel totalCaptionLabel = new JLabel();
    private final JLabel totalTimeValueLabel = new JLabel("0:00");
    private final JLabel summaryCaptionLabel = new JLabel();
    private final JLabel matrixCaptionLabel = new JLabel();
    private final JLabel reportFormatLabel = new JLabel();
    private final JComboBox<ReportFormatItem> reportFormatComboBox = new JComboBox<>();
    private final JButton downloadReportButton = new JButton();
    private final JPanel headerCard = new JPanel(new BorderLayout(8, 10));
    private final JPanel summaryPanel = new JPanel(new BorderLayout(4, 8));
    private final JPanel matrixPanel = new JPanel(new BorderLayout(4, 8));
    private final ApplicationUsageTableModel summaryTableModel =
            new ApplicationUsageTableModel(ApplicationUsageTableModel.DisplayMode.COMPACT);
    private final ApplicationUsageMatrixTableModel matrixTableModel = new ApplicationUsageMatrixTableModel();
    private final JTable summaryTable = new JTable(summaryTableModel);
    private final JTable matrixTable = new JTable(matrixTableModel);

    public StatisticsPanel(StatisticsService statisticsService, UserSettings userSettings) {
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        refresh();
    }

    private void buildContent() {
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.WEEK, MessageCodes.UI_STATS_PERIOD_WEEK));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.MONTH, MessageCodes.UI_STATS_PERIOD_MONTH));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.YEAR, MessageCodes.UI_STATS_PERIOD_YEAR));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.ALL_TIME, MessageCodes.UI_STATS_PERIOD_ALL));
        periodComboBox.setSelectedIndex(0);
        periodComboBox.addActionListener(actionEvent -> refresh());

        UiTheme.styleSurfaceCard(headerCard);

        JPanel periodRow = new JPanel(new BorderLayout(8, 8));
        periodRow.setOpaque(false);
        UiTheme.styleMutedLabel(periodLabel);
        periodRow.add(periodLabel, BorderLayout.WEST);
        periodRow.add(periodComboBox, BorderLayout.CENTER);

        JPanel totalRow = new JPanel(new BorderLayout(8, 0));
        totalRow.setOpaque(false);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        totalTimeValueLabel.setFont(totalTimeValueLabel.getFont().deriveFont(Font.BOLD, 22f));
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        totalRow.add(totalCaptionLabel, BorderLayout.WEST);
        totalRow.add(totalTimeValueLabel, BorderLayout.CENTER);

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

        JPanel reportFormatRow = new JPanel(new BorderLayout(8, 0));
        reportFormatRow.setOpaque(false);
        UiTheme.styleMutedLabel(reportFormatLabel);
        reportFormatRow.add(reportFormatLabel, BorderLayout.WEST);
        reportFormatRow.add(reportFormatComboBox, BorderLayout.CENTER);

        JPanel actionsRow = new JPanel(new BorderLayout(12, 0));
        actionsRow.setOpaque(false);
        actionsRow.add(reportFormatRow, BorderLayout.CENTER);
        actionsRow.add(downloadReportButton, BorderLayout.EAST);

        JPanel headerCenterPanel = new JPanel(new BorderLayout(0, 10));
        headerCenterPanel.setOpaque(false);
        headerCenterPanel.add(totalRow, BorderLayout.NORTH);
        headerCenterPanel.add(actionsRow, BorderLayout.SOUTH);

        headerCard.add(periodRow, BorderLayout.NORTH);
        headerCard.add(headerCenterPanel, BorderLayout.SOUTH);

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 12, 12));
        tablesPanel.setOpaque(false);

        UiTheme.styleSurfaceCard(summaryPanel);
        UiTheme.styleMutedLabel(summaryCaptionLabel);
        UiTheme.styleUsageTable(summaryTable);
        ApplicationUsageTableModel.configureColumnWidths(summaryTable);
        ApplicationUsageTableModel.configureColumnAlignment(summaryTable);
        summaryPanel.add(summaryCaptionLabel, BorderLayout.NORTH);
        summaryPanel.add(new JScrollPane(summaryTable), BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(matrixPanel);
        UiTheme.styleMutedLabel(matrixCaptionLabel);
        UiTheme.styleUsageTable(matrixTable);
        matrixTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        matrixPanel.add(matrixCaptionLabel, BorderLayout.NORTH);
        matrixPanel.add(new JScrollPane(matrixTable), BorderLayout.CENTER);

        tablesPanel.add(summaryPanel);
        tablesPanel.add(matrixPanel);

        add(headerCard, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);
        retranslate();
    }

    public void retranslate() {
        periodLabel.setText(Messages.get(MessageCodes.UI_STATS_PERIOD));
        totalCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_TOTAL));
        summaryCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_BY_APP));
        matrixCaptionLabel.setText(Messages.get(MessageCodes.UI_STATS_MATRIX));
        reportFormatLabel.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_FORMAT));
        downloadReportButton.setText(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT));
        periodComboBox.repaint();
        reportFormatComboBox.repaint();
        summaryTableModel.retranslate();
        matrixTableModel.retranslate();
        ApplicationUsageTableModel.configureColumnWidths(summaryTable);
        ApplicationUsageTableModel.configureColumnAlignment(summaryTable);
        refresh();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(headerCard);
        UiTheme.styleSurfaceCard(summaryPanel);
        UiTheme.styleSurfaceCard(matrixPanel);
        UiTheme.styleMutedLabel(periodLabel);
        UiTheme.styleMutedLabel(totalCaptionLabel);
        UiTheme.styleMutedLabel(summaryCaptionLabel);
        UiTheme.styleMutedLabel(matrixCaptionLabel);
        UiTheme.styleMutedLabel(reportFormatLabel);
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleSecondaryButton(downloadReportButton);
        UiTheme.styleUsageTable(summaryTable);
        UiTheme.styleUsageTable(matrixTable);
        matrixTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
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

        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshot(statsPeriod);
        totalTimeValueLabel.setText(
                DurationFormatter.formatHoursMinutes(statisticsSnapshot.getTotalActiveSeconds())
        );

        summaryTableModel.setRows(
                ApplicationUsageFilter.groupMinorApplications(
                        statisticsSnapshot.getApplicationUsageSummaries(),
                        userSettings.getMinorUsageThresholdMinutes()
                ),
                statisticsSnapshot.getTotalActiveSeconds()
        );
        if (summaryTableModel.isEmpty()) {
            summaryTableModel.setRows(
                    Collections.singletonList(
                            new ApplicationUsageSummary(Messages.get(MessageCodes.UI_STATS_EMPTY), 0L)
                    ),
                    0L
            );
        }
        ApplicationUsageTableModel.configureColumnAlignment(summaryTable);

        ApplicationUsageMatrix applicationUsageMatrix = ApplicationUsageFilter.groupMinorApplications(
                statisticsService.buildApplicationUsageMatrix(statsPeriod),
                userSettings.getMinorUsageThresholdMinutes()
        );
        matrixTableModel.setMatrix(applicationUsageMatrix);
        configureMatrixColumns();
    }

    private void onDownloadReportClicked() {
        ReportFormatItem selectedReportFormatItem = (ReportFormatItem) reportFormatComboBox.getSelectedItem();
        StatisticsReportFormat reportFormat = selectedReportFormatItem != null
                ? selectedReportFormatItem.reportFormat()
                : StatisticsReportFormat.EXCEL;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT));
        fileChooser.setSelectedFile(new File(buildDefaultReportFileName(reportFormat)));
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
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_SUCCESS),
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.error("Failed to export statistics report: {}", exception.getMessage(), exception);
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_STATS_DOWNLOAD_REPORT),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String buildDefaultReportFileName(StatisticsReportFormat reportFormat) {
        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "WorkPulseTracker-report-" + dateSuffix + "." + reportFormat.getFileExtension();
    }

    private void configureMatrixColumns() {
        if (matrixTable.getColumnModel().getColumnCount() == 0) {
            return;
        }

        DefaultTableCellRenderer applicationCellRenderer = new ApplicationNameCellRenderer();

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
                }
                return component;
            }
        };
        centeredCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centeredCellRenderer.setVerticalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer leftHeaderRenderer = new DefaultTableCellRenderer();
        leftHeaderRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftHeaderRenderer.setBackground(UiTheme.SURFACE_2);
        leftHeaderRenderer.setForeground(UiTheme.TEXT_SECONDARY);

        DefaultTableCellRenderer centeredHeaderRenderer = new DefaultTableCellRenderer();
        centeredHeaderRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centeredHeaderRenderer.setBackground(UiTheme.SURFACE_2);
        centeredHeaderRenderer.setForeground(UiTheme.TEXT_SECONDARY);

        matrixTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        matrixTable.getColumnModel().getColumn(0).setMinWidth(140);
        matrixTable.getColumnModel().getColumn(0).setCellRenderer(applicationCellRenderer);
        matrixTable.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);

        for (int columnIndex = 1; columnIndex < matrixTable.getColumnModel().getColumnCount(); columnIndex++) {
            matrixTable.getColumnModel().getColumn(columnIndex).setPreferredWidth(110);
            matrixTable.getColumnModel().getColumn(columnIndex).setMinWidth(88);
            matrixTable.getColumnModel().getColumn(columnIndex).setCellRenderer(centeredCellRenderer);
            matrixTable.getColumnModel().getColumn(columnIndex).setHeaderRenderer(centeredHeaderRenderer);
        }
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
