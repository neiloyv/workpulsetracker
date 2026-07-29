package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.icons.ApplicationIconService;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.util.Objects;

/**
 * Ячейка с иконкой приложения и названием.
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
        label.setText(applicationName);
        label.setIcon(ApplicationIconService.getInstance().getIcon(applicationName));
        return label;
    }
}
