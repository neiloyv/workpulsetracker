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

    /**
     * @return {@code true}, если изменилась структура колонок и нужен полный rebuild UI
     */
    public boolean setMatrix(ApplicationUsageMatrix applicationUsageMatrix) {
        ApplicationUsageMatrix previousMatrix = this.applicationUsageMatrix;
        boolean structureChanged = !hasSameColumnStructure(previousMatrix, applicationUsageMatrix);
        boolean contentChanged = structureChanged || !hasSameContent(previousMatrix, applicationUsageMatrix);
        this.applicationUsageMatrix = applicationUsageMatrix;
        if (!contentChanged) {
            return false;
        }
        if (structureChanged) {
            fireTableStructureChanged();
            return true;
        }
        fireTableDataChanged();
        return false;
    }

    public void retranslate() {
        periodTotalColumnName = Messages.get(MessageCodes.UI_TABLE_TOTAL);
    }

    /**
     * @return {@code true}, если изменилось имя первой колонки
     */
    public boolean setFirstColumnName(String firstColumnName) {
        String resolvedFirstColumnName = Objects.requireNonNullElseGet(
                firstColumnName,
                () -> Messages.get(MessageCodes.UI_TABLE_APPLICATION)
        );
        if (Objects.equals(this.applicationColumnName, resolvedFirstColumnName)) {
            return false;
        }
        this.applicationColumnName = resolvedFirstColumnName;
        return true;
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
        int bucketIndex = columnIndex - 2;
        return formatDurationCell(
                applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex),
                applicationUsageMatrix.getBucketTotalSeconds(bucketIndex)
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
        long bucketTotalSeconds = applicationUsageMatrix.getBucketTotalSeconds(columnIndex - 2);
        return formatDurationCell(bucketTotalSeconds, bucketTotalSeconds);
    }

    private static String formatDurationCell(long durationSeconds, long totalActiveSeconds) {
        if (durationSeconds <= 0L) {
            return "—";
        }
        return DurationFormatter.formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
    }

    private static boolean hasSameColumnStructure(
            ApplicationUsageMatrix leftMatrix,
            ApplicationUsageMatrix rightMatrix
    ) {
        if (Objects.isNull(leftMatrix) || Objects.isNull(rightMatrix)) {
            return false;
        }
        List<PeriodBucket> leftBuckets = leftMatrix.getPeriodBuckets();
        List<PeriodBucket> rightBuckets = rightMatrix.getPeriodBuckets();
        if (leftBuckets.size() != rightBuckets.size()) {
            return false;
        }
        return IntStream.range(0, leftBuckets.size())
                .allMatch(bucketIndex -> Objects.equals(
                        leftBuckets.get(bucketIndex).getLabel(),
                        rightBuckets.get(bucketIndex).getLabel()
                ));
    }

    private static boolean hasSameContent(
            ApplicationUsageMatrix leftMatrix,
            ApplicationUsageMatrix rightMatrix
    ) {
        if (Objects.isNull(leftMatrix) || Objects.isNull(rightMatrix)) {
            return false;
        }
        if (leftMatrix.getTotalActiveSeconds() != rightMatrix.getTotalActiveSeconds()) {
            return false;
        }
        List<String> leftApplicationNames = leftMatrix.getApplicationNames();
        List<String> rightApplicationNames = rightMatrix.getApplicationNames();
        if (!leftApplicationNames.equals(rightApplicationNames)) {
            return false;
        }
        int bucketCount = leftMatrix.getPeriodBuckets().size();
        return IntStream.range(0, leftApplicationNames.size())
                .allMatch(applicationIndex ->
                        leftMatrix.getApplicationTotalSeconds(applicationIndex)
                                == rightMatrix.getApplicationTotalSeconds(applicationIndex)
                                && IntStream.range(0, bucketCount)
                                .allMatch(bucketIndex ->
                                        leftMatrix.getDurationSeconds(applicationIndex, bucketIndex)
                                                == rightMatrix.getDurationSeconds(applicationIndex, bucketIndex)
                                )
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
