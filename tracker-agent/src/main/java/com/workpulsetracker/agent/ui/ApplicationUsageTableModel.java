package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.agent.util.PercentageCalculator;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Таблица использования приложений.
 * DETAILED — приложение / время / % (главная вкладка).
 * COMPACT — приложение / {@code H:MM (N%)} (статистика).
 */
public final class ApplicationUsageTableModel extends AbstractTableModel {

    public enum DisplayMode {
        DETAILED,
        COMPACT
    }

    private final DisplayMode displayMode;
    private final List<ApplicationUsageSummary> applicationUsageSummaries = new ArrayList<>();
    private long totalActiveSeconds;
    private String[] columnNames;

    public ApplicationUsageTableModel() {
        this(DisplayMode.DETAILED);
    }

    public ApplicationUsageTableModel(DisplayMode displayMode) {
        this.displayMode = Objects.requireNonNull(displayMode);
        this.columnNames = buildColumnNames();
    }

    public void setRows(List<ApplicationUsageSummary> applicationUsageSummaries, long totalActiveSeconds) {
        this.applicationUsageSummaries.clear();
        if (Objects.nonNull(applicationUsageSummaries)) {
            this.applicationUsageSummaries.addAll(applicationUsageSummaries);
        }
        this.totalActiveSeconds = totalActiveSeconds;
        fireTableDataChanged();
    }

    public void retranslate() {
        columnNames = buildColumnNames();
        fireTableStructureChanged();
    }

    private String[] buildColumnNames() {
        if (displayMode == DisplayMode.COMPACT) {
            return new String[]{
                    Messages.get(MessageCodes.UI_TABLE_APPLICATION),
                    Messages.get(MessageCodes.UI_TABLE_TIME)
            };
        }
        return new String[]{
                Messages.get(MessageCodes.UI_TABLE_APPLICATION),
                Messages.get(MessageCodes.UI_TABLE_TIME),
                Messages.get(MessageCodes.UI_TABLE_PERCENT)
        };
    }

    @Override
    public int getRowCount() {
        return applicationUsageSummaries.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ApplicationUsageSummary applicationUsageSummary = applicationUsageSummaries.get(rowIndex);
        if (displayMode == DisplayMode.COMPACT) {
            return switch (columnIndex) {
                case 0 -> applicationUsageSummary.getApplicationName();
                case 1 -> DurationFormatter.formatHoursMinutesWithPercent(
                        applicationUsageSummary.getDurationSeconds(),
                        totalActiveSeconds
                );
                default -> "";
            };
        }
        return switch (columnIndex) {
            case 0 -> applicationUsageSummary.getApplicationName();
            case 1 -> DurationFormatter.formatSeconds(applicationUsageSummary.getDurationSeconds());
            case 2 -> PercentageCalculator.calculatePercentage(
                    applicationUsageSummary.getDurationSeconds(),
                    totalActiveSeconds
            ) + "%";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public boolean isEmpty() {
        return applicationUsageSummaries.isEmpty();
    }

    public static void configureColumnWidths(JTable table) {
        int columnCount = table.getColumnModel().getColumnCount();
        if (columnCount < 2) {
            return;
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(columnCount == 2 ? 120 : 90);
        if (columnCount >= 3) {
            table.getColumnModel().getColumn(2).setPreferredWidth(70);
        }
    }

    /**
     * Первая колонка (программы) — слева, остальные — по центру.
     */
    public static void configureColumnAlignment(JTable table) {
        int columnCount = table.getColumnModel().getColumnCount();
        if (columnCount == 0) {
            return;
        }

        DefaultTableCellRenderer leftCellRenderer = new ApplicationNameCellRenderer();

        DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer();
        centerCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerCellRenderer.setVerticalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer leftHeaderRenderer = new DefaultTableCellRenderer();
        leftHeaderRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        leftHeaderRenderer.setBackground(UiTheme.SURFACE_2);
        leftHeaderRenderer.setForeground(UiTheme.TEXT_SECONDARY);

        DefaultTableCellRenderer centerHeaderRenderer = new DefaultTableCellRenderer();
        centerHeaderRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerHeaderRenderer.setBackground(UiTheme.SURFACE_2);
        centerHeaderRenderer.setForeground(UiTheme.TEXT_SECONDARY);

        table.getColumnModel().getColumn(0).setCellRenderer(leftCellRenderer);
        table.getColumnModel().getColumn(0).setHeaderRenderer(leftHeaderRenderer);
        for (int columnIndex = 1; columnIndex < columnCount; columnIndex++) {
            table.getColumnModel().getColumn(columnIndex).setCellRenderer(centerCellRenderer);
            table.getColumnModel().getColumn(columnIndex).setHeaderRenderer(centerHeaderRenderer);
        }
    }
}
