package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.PeriodBucket;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Матрица статистики: приложение × колонки периода (время + процент в ячейке).
 */
public final class ApplicationUsageMatrixTableModel extends AbstractTableModel {

    private ApplicationUsageMatrix applicationUsageMatrix;
    private String applicationColumnName = Messages.get(MessageCodes.UI_TABLE_APPLICATION);

    public void setMatrix(ApplicationUsageMatrix applicationUsageMatrix) {
        this.applicationUsageMatrix = applicationUsageMatrix;
        fireTableStructureChanged();
    }

    public void retranslate() {
        applicationColumnName = Messages.get(MessageCodes.UI_TABLE_APPLICATION);
        fireTableStructureChanged();
    }

    private List<PeriodBucket> getPeriodBuckets() {
        if (Objects.isNull(applicationUsageMatrix)) {
            return Collections.emptyList();
        }
        return applicationUsageMatrix.getPeriodBuckets();
    }

    private List<String> getApplicationNames() {
        if (Objects.isNull(applicationUsageMatrix)) {
            return Collections.emptyList();
        }
        return applicationUsageMatrix.getApplicationNames();
    }

    @Override
    public int getRowCount() {
        return getApplicationNames().size();
    }

    @Override
    public int getColumnCount() {
        return 1 + getPeriodBuckets().size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return applicationColumnName;
        }
        return getPeriodBuckets().get(columnIndex - 1).getLabel();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (Objects.isNull(applicationUsageMatrix)) {
            return "";
        }
        if (columnIndex == 0) {
            return applicationUsageMatrix.getApplicationNames().get(rowIndex);
        }
        long durationSeconds = applicationUsageMatrix.getDurationSeconds(rowIndex, columnIndex - 1);
        if (durationSeconds <= 0L) {
            return "—";
        }
        return DurationFormatter.formatHoursMinutesWithPercent(
                durationSeconds,
                applicationUsageMatrix.getTotalActiveSeconds()
        );
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public boolean isEmpty() {
        return getApplicationNames().isEmpty();
    }
}
