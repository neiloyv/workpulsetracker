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
import java.util.stream.IntStream;

/**
 * Единая таблица статистики:
 * приложение | итог за период | разбивка (дни / недели / месяцы / годы).
 * Строка Total — внизу.
 */
public final class ApplicationUsageMatrixTableModel extends AbstractTableModel {

    private static final int APPLICATION_COLUMN_INDEX = 0;
    private static final int PERIOD_TOTAL_COLUMN_INDEX = 1;

    private ApplicationUsageMatrix applicationUsageMatrix;
    private String applicationColumnName = Messages.get(MessageCodes.UI_TABLE_APPLICATION);
    private String periodTotalColumnName = Messages.get(MessageCodes.UI_TABLE_TOTAL);

    public void setMatrix(ApplicationUsageMatrix applicationUsageMatrix) {
        this.applicationUsageMatrix = applicationUsageMatrix;
        fireTableStructureChanged();
    }

    public void retranslate() {
        applicationColumnName = Messages.get(MessageCodes.UI_TABLE_APPLICATION);
        periodTotalColumnName = Messages.get(MessageCodes.UI_TABLE_TOTAL);
        fireTableStructureChanged();
    }

    public boolean isTotalRow(int rowIndex) {
        return !isEmpty() && rowIndex == getRowCount() - 1;
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
        int applicationCount = getApplicationNames().size();
        return applicationCount == 0 ? 0 : applicationCount + 1;
    }

    @Override
    public int getColumnCount() {
        return 2 + getPeriodBuckets().size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == APPLICATION_COLUMN_INDEX) {
            return applicationColumnName;
        }
        if (columnIndex == PERIOD_TOTAL_COLUMN_INDEX) {
            return periodTotalColumnName;
        }
        return getPeriodBuckets().get(columnIndex - 2).getLabel();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (Objects.isNull(applicationUsageMatrix)) {
            return "";
        }
        if (isTotalRow(rowIndex)) {
            return getTotalRowValue(columnIndex);
        }
        int applicationIndex = rowIndex;
        if (columnIndex == APPLICATION_COLUMN_INDEX) {
            return applicationUsageMatrix.getApplicationNames().get(applicationIndex);
        }
        if (columnIndex == PERIOD_TOTAL_COLUMN_INDEX) {
            return formatDurationCell(
                    applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                    applicationUsageMatrix.getTotalActiveSeconds()
            );
        }
        return formatDurationCell(
                applicationUsageMatrix.getDurationSeconds(applicationIndex, columnIndex - 2),
                applicationUsageMatrix.getTotalActiveSeconds()
        );
    }

    private Object getTotalRowValue(int columnIndex) {
        if (columnIndex == APPLICATION_COLUMN_INDEX) {
            return Messages.get(MessageCodes.UI_TABLE_TOTAL);
        }
        if (columnIndex == PERIOD_TOTAL_COLUMN_INDEX) {
            return formatDurationCell(
                    applicationUsageMatrix.getTotalActiveSeconds(),
                    applicationUsageMatrix.getTotalActiveSeconds()
            );
        }
        return formatDurationCell(
                getBucketTotalSeconds(columnIndex - 2),
                applicationUsageMatrix.getTotalActiveSeconds()
        );
    }

    private long getBucketTotalSeconds(int bucketIndex) {
        return IntStream.range(0, getApplicationNames().size())
                .mapToLong(applicationIndex ->
                        applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex)
                )
                .sum();
    }

    private static String formatDurationCell(long durationSeconds, long totalActiveSeconds) {
        if (durationSeconds <= 0L) {
            return "—";
        }
        return DurationFormatter.formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public boolean isEmpty() {
        return getApplicationNames().isEmpty();
    }
}
