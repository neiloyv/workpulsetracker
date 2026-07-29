package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageGroup;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Таблица использования приложений.
 * DETAILED — приложение / время / % (главная вкладка).
 * COMPACT — приложение / {@code H:MM (N%)}.
 * Браузеры с сайтами — раскрываемые группы. Строка Total — внизу.
 */
public final class ApplicationUsageTableModel extends AbstractTableModel {

    public enum DisplayMode {
        DETAILED,
        COMPACT
    }

    private final DisplayMode displayMode;
    private final List<ApplicationUsageGroup> applicationUsageGroups = new ArrayList<>();
    private final Set<String> expandedApplicationNames = new HashSet<>();
    private final List<VisibleRow> visibleRows = new ArrayList<>();
    private long totalActiveSeconds;
    private String[] columnNames;

    public ApplicationUsageTableModel() {
        this(DisplayMode.DETAILED);
    }

    public ApplicationUsageTableModel(DisplayMode displayMode) {
        this.displayMode = Objects.requireNonNull(displayMode);
        this.columnNames = buildColumnNames();
    }

    public void setGroups(List<ApplicationUsageGroup> applicationUsageGroups, long totalActiveSeconds) {
        this.applicationUsageGroups.clear();
        if (Objects.nonNull(applicationUsageGroups)) {
            this.applicationUsageGroups.addAll(applicationUsageGroups);
        }
        this.totalActiveSeconds = totalActiveSeconds;
        Set<String> availableExpandableNames = this.applicationUsageGroups.stream()
                .filter(ApplicationUsageGroup::isExpandable)
                .map(ApplicationUsageGroup::getApplicationName)
                .collect(Collectors.toSet());
        expandedApplicationNames.retainAll(availableExpandableNames);
        rebuildVisibleRows();
        fireTableDataChanged();
    }

    public void setRows(List<ApplicationUsageSummary> applicationUsageSummaries, long totalActiveSeconds) {
        List<ApplicationUsageGroup> groups = Objects.isNull(applicationUsageSummaries)
                ? List.of()
                : applicationUsageSummaries.stream()
                        .map(applicationUsageSummary -> ApplicationUsageGroup.leaf(
                                applicationUsageSummary.getApplicationName(),
                                applicationUsageSummary.getDurationSeconds()
                        ))
                        .collect(Collectors.toList());
        setGroups(groups, totalActiveSeconds);
    }

    public void retranslate() {
        columnNames = buildColumnNames();
        fireTableStructureChanged();
        rebuildVisibleRows();
        fireTableDataChanged();
    }

    public boolean isTotalRow(int rowIndex) {
        return rowIndex == getRowCount() - 1;
    }

    public boolean isChildRow(int rowIndex) {
        if (isTotalRow(rowIndex) || rowIndex < 0 || rowIndex >= visibleRows.size()) {
            return false;
        }
        return visibleRows.get(rowIndex).child();
    }

    public boolean isExpandableRow(int rowIndex) {
        if (isTotalRow(rowIndex) || rowIndex < 0 || rowIndex >= visibleRows.size()) {
            return false;
        }
        VisibleRow visibleRow = visibleRows.get(rowIndex);
        return !visibleRow.child() && visibleRow.applicationUsageGroup().isExpandable();
    }

    public boolean isExpandedRow(int rowIndex) {
        if (!isExpandableRow(rowIndex)) {
            return false;
        }
        return expandedApplicationNames.contains(
                visibleRows.get(rowIndex).applicationUsageGroup().getApplicationName()
        );
    }

    public String getIconApplicationName(int rowIndex) {
        if (isTotalRow(rowIndex) || rowIndex < 0 || rowIndex >= visibleRows.size()) {
            return "";
        }
        return visibleRows.get(rowIndex).applicationUsageGroup().getApplicationName();
    }

    public void toggleExpanded(int rowIndex) {
        if (!isExpandableRow(rowIndex)) {
            return;
        }
        String applicationName = visibleRows.get(rowIndex).applicationUsageGroup().getApplicationName();
        if (expandedApplicationNames.contains(applicationName)) {
            expandedApplicationNames.remove(applicationName);
        } else {
            expandedApplicationNames.add(applicationName);
        }
        rebuildVisibleRows();
        fireTableDataChanged();
    }

    private void rebuildVisibleRows() {
        visibleRows.clear();
        applicationUsageGroups.forEach(applicationUsageGroup -> {
            visibleRows.add(new VisibleRow(applicationUsageGroup, null, false));
            if (applicationUsageGroup.isExpandable()
                    && expandedApplicationNames.contains(applicationUsageGroup.getApplicationName())) {
                applicationUsageGroup.getSiteChildren().forEach(siteChild ->
                        visibleRows.add(new VisibleRow(applicationUsageGroup, siteChild, true))
                );
            }
        });
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
        return visibleRows.size() + 1;
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
        if (isTotalRow(rowIndex)) {
            return getTotalRowValue(columnIndex);
        }
        VisibleRow visibleRow = visibleRows.get(rowIndex);
        long durationSeconds = visibleRow.child()
                ? visibleRow.siteChild().getDurationSeconds()
                : visibleRow.applicationUsageGroup().getDurationSeconds();
        String applicationName = visibleRow.child()
                ? visibleRow.siteChild().getApplicationName()
                : visibleRow.applicationUsageGroup().getApplicationName();

        if (displayMode == DisplayMode.COMPACT) {
            return switch (columnIndex) {
                case 0 -> applicationName;
                case 1 -> DurationFormatter.formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
                default -> "";
            };
        }
        return switch (columnIndex) {
            case 0 -> applicationName;
            case 1 -> DurationFormatter.formatSeconds(durationSeconds);
            case 2 -> PercentageCalculator.calculatePercentage(durationSeconds, totalActiveSeconds) + "%";
            default -> "";
        };
    }

    private Object getTotalRowValue(int columnIndex) {
        if (displayMode == DisplayMode.COMPACT) {
            return switch (columnIndex) {
                case 0 -> Messages.get(MessageCodes.UI_TABLE_TOTAL);
                case 1 -> DurationFormatter.formatHoursMinutesWithPercent(
                        totalActiveSeconds,
                        totalActiveSeconds
                );
                default -> "";
            };
        }
        return switch (columnIndex) {
            case 0 -> Messages.get(MessageCodes.UI_TABLE_TOTAL);
            case 1 -> DurationFormatter.formatSeconds(totalActiveSeconds);
            case 2 -> PercentageCalculator.calculatePercentage(totalActiveSeconds, totalActiveSeconds) + "%";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public boolean isEmpty() {
        return applicationUsageGroups.isEmpty();
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

        DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable valueTable,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                java.awt.Component component = super.getTableCellRendererComponent(
                        valueTable, value, isSelected, hasFocus, row, column
                );
                if (component instanceof javax.swing.JLabel label
                        && valueTable.getModel() instanceof ApplicationUsageTableModel tableModel
                        && tableModel.isTotalRow(row)) {
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
                }
                return component;
            }
        };
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

    private record VisibleRow(
            ApplicationUsageGroup applicationUsageGroup,
            ApplicationUsageSummary siteChild,
            boolean child
    ) {
    }
}
