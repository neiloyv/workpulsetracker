package com.workpulsetracker.agent.report;

import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.stats.StatisticsSnapshot;
import com.workpulsetracker.agent.stats.StatsPeriod;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Excel-отчёт по статистике: отдельный лист на каждый период.
 */
public final class StatisticsExcelReportWriter {

    private final StatisticsService statisticsService;
    private final int minorUsageThresholdMinutes;

    public StatisticsExcelReportWriter(StatisticsService statisticsService, int minorUsageThresholdMinutes) {
        this.statisticsService = Objects.requireNonNull(statisticsService);
        this.minorUsageThresholdMinutes = Math.max(minorUsageThresholdMinutes, 0);
    }

    public void writeToFile(Path reportFilePath) throws IOException {
        Objects.requireNonNull(reportFilePath);
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(reportFilePath)) {
            CellStyle titleCellStyle = createBoldCellStyle(workbook, 14);
            CellStyle headerCellStyle = createBoldCellStyle(workbook, 11);
            CellStyle sectionCellStyle = createBoldCellStyle(workbook, 12);

            writePeriodSheet(
                    workbook,
                    StatsPeriod.WEEK,
                    Messages.get(MessageCodes.UI_STATS_PERIOD_WEEK),
                    titleCellStyle,
                    headerCellStyle,
                    sectionCellStyle
            );
            writePeriodSheet(
                    workbook,
                    StatsPeriod.MONTH,
                    Messages.get(MessageCodes.UI_STATS_PERIOD_MONTH),
                    titleCellStyle,
                    headerCellStyle,
                    sectionCellStyle
            );
            writePeriodSheet(
                    workbook,
                    StatsPeriod.YEAR,
                    Messages.get(MessageCodes.UI_STATS_PERIOD_YEAR),
                    titleCellStyle,
                    headerCellStyle,
                    sectionCellStyle
            );
            writePeriodSheet(
                    workbook,
                    StatsPeriod.ALL_TIME,
                    Messages.get(MessageCodes.UI_STATS_PERIOD_ALL),
                    titleCellStyle,
                    headerCellStyle,
                    sectionCellStyle
            );

            workbook.write(outputStream);
        }
    }

    private void writePeriodSheet(
            Workbook workbook,
            StatsPeriod statsPeriod,
            String periodTitle,
            CellStyle titleCellStyle,
            CellStyle headerCellStyle,
            CellStyle sectionCellStyle
    ) {
        Sheet sheet = workbook.createSheet(sanitizeSheetName(periodTitle));
        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshot(statsPeriod);
        List<ApplicationUsageSummary> applicationUsageSummaries = ApplicationUsageFilter.groupMinorApplications(
                statisticsSnapshot.getApplicationUsageSummaries(),
                minorUsageThresholdMinutes
        );
        ApplicationUsageMatrix applicationUsageMatrix = ApplicationUsageFilter.groupMinorApplications(
                statisticsService.buildApplicationUsageMatrix(statsPeriod),
                minorUsageThresholdMinutes
        );

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, periodTitle, titleCellStyle);

        Row totalRow = sheet.createRow(rowIndex++);
        createCell(
                totalRow,
                0,
                Messages.get(MessageCodes.UI_STATS_TOTAL) + " "
                        + DurationFormatter.formatHoursMinutes(statisticsSnapshot.getTotalActiveSeconds()),
                null
        );

        rowIndex++;
        Row summarySectionRow = sheet.createRow(rowIndex++);
        createCell(summarySectionRow, 0, Messages.get(MessageCodes.UI_STATS_BY_APP), sectionCellStyle);

        Row summaryHeaderRow = sheet.createRow(rowIndex++);
        createCell(summaryHeaderRow, 0, Messages.get(MessageCodes.UI_TABLE_APPLICATION), headerCellStyle);
        createCell(summaryHeaderRow, 1, Messages.get(MessageCodes.UI_TABLE_TIME), headerCellStyle);

        if (applicationUsageSummaries.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex++);
            createCell(emptyRow, 0, Messages.get(MessageCodes.UI_STATS_EMPTY), null);
        } else {
            for (ApplicationUsageSummary applicationUsageSummary : applicationUsageSummaries) {
                Row dataRow = sheet.createRow(rowIndex++);
                createCell(dataRow, 0, applicationUsageSummary.getApplicationName(), null);
                createCell(
                        dataRow,
                        1,
                        DurationFormatter.formatHoursMinutesWithPercent(
                                applicationUsageSummary.getDurationSeconds(),
                                statisticsSnapshot.getTotalActiveSeconds()
                        ),
                        null
                );
            }
        }

        rowIndex++;
        Row matrixSectionRow = sheet.createRow(rowIndex++);
        createCell(matrixSectionRow, 0, Messages.get(MessageCodes.UI_STATS_MATRIX), sectionCellStyle);

        Row matrixHeaderRow = sheet.createRow(rowIndex++);
        createCell(matrixHeaderRow, 0, Messages.get(MessageCodes.UI_TABLE_APPLICATION), headerCellStyle);
        for (int bucketIndex = 0; bucketIndex < applicationUsageMatrix.getPeriodBuckets().size(); bucketIndex++) {
            createCell(
                    matrixHeaderRow,
                    bucketIndex + 1,
                    applicationUsageMatrix.getPeriodBuckets().get(bucketIndex).getLabel(),
                    headerCellStyle
            );
        }

        if (applicationUsageMatrix.getApplicationNames().isEmpty()) {
            Row emptyMatrixRow = sheet.createRow(rowIndex++);
            createCell(emptyMatrixRow, 0, Messages.get(MessageCodes.UI_STATS_EMPTY), null);
        } else {
            for (int applicationIndex = 0;
                 applicationIndex < applicationUsageMatrix.getApplicationNames().size();
                 applicationIndex++) {
                Row dataRow = sheet.createRow(rowIndex++);
                createCell(
                        dataRow,
                        0,
                        applicationUsageMatrix.getApplicationNames().get(applicationIndex),
                        null
                );
                for (int bucketIndex = 0;
                     bucketIndex < applicationUsageMatrix.getPeriodBuckets().size();
                     bucketIndex++) {
                    long durationSeconds = applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex);
                    String cellValue = durationSeconds <= 0L
                            ? "—"
                            : DurationFormatter.formatHoursMinutesWithPercent(
                            durationSeconds,
                            applicationUsageMatrix.getTotalActiveSeconds()
                    );
                    createCell(dataRow, bucketIndex + 1, cellValue, null);
                }
            }
        }

        int columnCount = Math.max(2, 1 + applicationUsageMatrix.getPeriodBuckets().size());
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }

    private static CellStyle createBoldCellStyle(Workbook workbook, int fontSize) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) fontSize);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        return cellStyle;
    }

    private static void createCell(Row row, int columnIndex, String value, CellStyle cellStyle) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(StringUtils.defaultString(value));
        if (Objects.nonNull(cellStyle)) {
            cell.setCellStyle(cellStyle);
        }
    }

    private static String sanitizeSheetName(String sheetName) {
        String sanitizedSheetName = StringUtils.defaultIfBlank(sheetName, "Sheet")
                .replaceAll("[\\\\/*?:\\[\\]]", " ")
                .trim();
        if (sanitizedSheetName.length() > 31) {
            return sanitizedSheetName.substring(0, 31);
        }
        return sanitizedSheetName;
    }
}
