package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.icons.ApplicationIconService;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.util.Objects;

/**
 * Ячейка с иконкой приложения и названием (с раскрытием групп браузера).
 */
public final class ApplicationNameCellRenderer extends DefaultTableCellRenderer {

    public ApplicationNameCellRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.CENTER);
        setIconTextGap(8);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column
        );
        String applicationName = Objects.nonNull(value) ? String.valueOf(value) : "";
        boolean totalRow = table.getModel() instanceof ApplicationUsageTableModel usageTableModel
                && usageTableModel.isTotalRow(row)
                || table.getModel() instanceof ApplicationUsageMatrixTableModel matrixTableModel
                && matrixTableModel.isTotalRow(row);

        if (totalRow) {
            label.setText(applicationName);
            label.setIcon(null);
            label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
            return label;
        }

        label.setFont(table.getFont());
        if (table.getModel() instanceof ApplicationUsageTableModel usageTableModel) {
            String iconApplicationName = usageTableModel.getIconApplicationName(row);
            label.setIcon(ApplicationIconService.getInstance().getIcon(iconApplicationName));
            if (usageTableModel.isChildRow(row)) {
                label.setText("    " + applicationName);
            } else if (usageTableModel.isExpandableRow(row)) {
                String expandMarker = usageTableModel.isExpandedRow(row) ? "▼ " : "▶ ";
                label.setText(expandMarker + applicationName);
            } else {
                label.setText(applicationName);
            }
            return label;
        }

        label.setText(applicationName);
        label.setIcon(ApplicationIconService.getInstance().getIcon(applicationName));
        return label;
    }
}
