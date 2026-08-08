package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.agent.util.ApplicationDisplayNameResolver;

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
        String displayApplicationName = ApplicationDisplayNameResolver.resolveDisplayName(applicationName);
        boolean totalRow = table.getModel() instanceof ApplicationUsageTableModel usageTableModel
                && usageTableModel.isTotalRow(row)
                || table.getModel() instanceof ApplicationUsageMatrixTableModel matrixTableModel
                && matrixTableModel.isTotalRow(row);

        if (totalRow) {
            label.setText(displayApplicationName);
            label.setIcon(null);
            label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
            return label;
        }

        label.setFont(table.getFont());
        if (table.getModel() instanceof ApplicationUsageTableModel usageTableModel) {
            String iconApplicationName = usageTableModel.getIconApplicationName(row);
            label.setIcon(ApplicationIconService.getInstance().getIcon(iconApplicationName));
            if (usageTableModel.isChildRow(row)) {
                label.setText("    " + displayApplicationName);
            } else if (usageTableModel.isExpandableRow(row)) {
                String expandMarker = usageTableModel.isExpandedRow(row) ? "▼ " : "▶ ";
                label.setText(expandMarker + displayApplicationName);
            } else {
                label.setText(displayApplicationName);
            }
            return label;
        }

        label.setText(displayApplicationName);
        label.setIcon(ApplicationIconService.getInstance().getIcon(applicationName));
        return label;
    }
}
