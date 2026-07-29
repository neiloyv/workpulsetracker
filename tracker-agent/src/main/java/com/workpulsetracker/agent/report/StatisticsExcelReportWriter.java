package com.workpulsetracker.agent.report;

import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Excel-отчёт: лист на период; таблицы (для месяца — по неделям) + круговая диаграмма в конце.
 */
public final class StatisticsExcelReportWriter {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsExcelReportWriter.class);

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
            CellStyle titleCellStyle = createBoldCellStyle(workbook, 14, HorizontalAlignment.LEFT);
            CellStyle sectionCellStyle = createBoldCellStyle(workbook, 12, HorizontalAlignment.LEFT);
            CellStyle headerCellStyle = createBoldCellStyle(workbook, 11, HorizontalAlignment.CENTER);
            CellStyle bodyCenterCellStyle = createCellStyle(workbook, 11, HorizontalAlignment.CENTER);
            CellStyle bodyLeftCellStyle = createCellStyle(workbook, 11, HorizontalAlignment.LEFT);
            CellStyle totalLeftCellStyle = createBoldCellStyle(workbook, 11, HorizontalAlignment.LEFT);
            CellStyle totalCenterCellStyle = createBoldCellStyle(workbook, 11, HorizontalAlignment.CENTER);

            StatisticsReportSection.buildAll(statisticsService, minorUsageThresholdMinutes)
                    .forEach(statisticsReportSection -> writePeriodSheet(
                            workbook,
                            statisticsReportSection,
                            titleCellStyle,
                            sectionCellStyle,
                            headerCellStyle,
                            bodyLeftCellStyle,
                            bodyCenterCellStyle,
                            totalLeftCellStyle,
                            totalCenterCellStyle
                    ));

            workbook.write(outputStream);
        }
    }

    private void writePeriodSheet(
            Workbook workbook,
            StatisticsReportSection statisticsReportSection,
            CellStyle titleCellStyle,
            CellStyle sectionCellStyle,
            CellStyle headerCellStyle,
            CellStyle bodyLeftCellStyle,
            CellStyle bodyCenterCellStyle,
            CellStyle totalLeftCellStyle,
            CellStyle totalCenterCellStyle
    ) {
        Sheet sheet = workbook.createSheet(sanitizeSheetName(statisticsReportSection.getPeriodTitle()));
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        createCell(
                titleRow,
                0,
                statisticsReportSection.getPeriodTitle()
                        + "  "
                        + Messages.get(MessageCodes.UI_STATS_TOTAL)
                        + " "
                        + DurationFormatter.formatHoursMinutes(statisticsReportSection.getTotalActiveSeconds()),
                titleCellStyle
        );

        int maxColumnCount = 2;
        for (StatisticsReportTable reportTable : statisticsReportSection.getReportTables()) {
            rowIndex++;
            if (reportTable.hasTitle()) {
                Row weekTitleRow = sheet.createRow(rowIndex++);
                createCell(weekTitleRow, 0, reportTable.getTitle(), sectionCellStyle);
            }
            rowIndex = writeMatrixTable(
                    sheet,
                    rowIndex,
                    reportTable.getApplicationUsageMatrix(),
                    headerCellStyle,
                    bodyLeftCellStyle,
                    bodyCenterCellStyle,
                    totalLeftCellStyle,
                    totalCenterCellStyle
            );
            maxColumnCount = Math.max(
                    maxColumnCount,
                    2 + reportTable.getApplicationUsageMatrix().getPeriodBuckets().size()
            );
        }

        for (int columnIndex = 0; columnIndex < maxColumnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }

        rowIndex++;
        addPieChart(workbook, sheet, statisticsReportSection, rowIndex);
    }

    private int writeMatrixTable(
            Sheet sheet,
            int startRowIndex,
            ApplicationUsageMatrix applicationUsageMatrix,
            CellStyle headerCellStyle,
            CellStyle bodyLeftCellStyle,
            CellStyle bodyCenterCellStyle,
            CellStyle totalLeftCellStyle,
            CellStyle totalCenterCellStyle
    ) {
        int rowIndex = startRowIndex;
        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, Messages.get(MessageCodes.UI_TABLE_APPLICATION), headerCellStyle);
        createCell(headerRow, 1, Messages.get(MessageCodes.UI_TABLE_TOTAL), headerCellStyle);
        for (int bucketIndex = 0; bucketIndex < applicationUsageMatrix.getPeriodBuckets().size(); bucketIndex++) {
            createCell(
                    headerRow,
                    bucketIndex + 2,
                    applicationUsageMatrix.getPeriodBuckets().get(bucketIndex).getLabel(),
                    headerCellStyle
            );
        }

        if (applicationUsageMatrix.getApplicationNames().isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex++);
            createCell(emptyRow, 0, Messages.get(MessageCodes.UI_STATS_EMPTY), bodyLeftCellStyle);
            return rowIndex;
        }

        for (int applicationIndex = 0;
             applicationIndex < applicationUsageMatrix.getApplicationNames().size();
             applicationIndex++) {
            Row dataRow = sheet.createRow(rowIndex++);
            writeApplicationRow(
                    dataRow,
                    applicationUsageMatrix,
                    applicationIndex,
                    bodyLeftCellStyle,
                    bodyCenterCellStyle
            );
        }
        Row footerTotalRow = sheet.createRow(rowIndex++);
        writeTotalRow(
                footerTotalRow,
                applicationUsageMatrix,
                totalLeftCellStyle,
                totalCenterCellStyle
        );
        return rowIndex;
    }

    private void addPieChart(
            Workbook workbook,
            Sheet sheet,
            StatisticsReportSection statisticsReportSection,
            int startRowIndex
    ) {
        try {
            byte[] pieChartPngBytes = ApplicationUsagePieChartImageRenderer.renderPng(
                    statisticsReportSection.getApplicationUsageSummaries(),
                    statisticsReportSection.getTotalActiveSeconds()
            );
            int pictureIndex = workbook.addPicture(pieChartPngBytes, Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor clientAnchor = workbook.getCreationHelper().createClientAnchor();
            clientAnchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            clientAnchor.setCol1(0);
            clientAnchor.setRow1(startRowIndex);
            Picture picture = drawing.createPicture(clientAnchor, pictureIndex);
            picture.resize();
        } catch (IOException ioException) {
            logger.warn("Failed to embed pie chart into Excel sheet {}: {}",
                    statisticsReportSection.getPeriodTitle(),
                    ioException.getMessage());
        }
    }

    private void writeApplicationRow(
            Row dataRow,
            ApplicationUsageMatrix applicationUsageMatrix,
            int applicationIndex,
            CellStyle bodyLeftCellStyle,
            CellStyle bodyCenterCellStyle
    ) {
        createCell(
                dataRow,
                0,
                applicationUsageMatrix.getApplicationNames().get(applicationIndex),
                bodyLeftCellStyle
        );
        createCell(
                dataRow,
                1,
                formatDurationCell(
                        applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                        applicationUsageMatrix.getTotalActiveSeconds()
                ),
                bodyCenterCellStyle
        );
        for (int bucketIndex = 0; bucketIndex < applicationUsageMatrix.getPeriodBuckets().size(); bucketIndex++) {
            createCell(
                    dataRow,
                    bucketIndex + 2,
                    formatDurationCell(
                            applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex),
                            applicationUsageMatrix.getTotalActiveSeconds()
                    ),
                    bodyCenterCellStyle
            );
        }
    }

    private void writeTotalRow(
            Row dataRow,
            ApplicationUsageMatrix applicationUsageMatrix,
            CellStyle totalLeftCellStyle,
            CellStyle totalCenterCellStyle
    ) {
        createCell(dataRow, 0, Messages.get(MessageCodes.UI_TABLE_TOTAL), totalLeftCellStyle);
        createCell(
                dataRow,
                1,
                formatDurationCell(
                        applicationUsageMatrix.getTotalActiveSeconds(),
                        applicationUsageMatrix.getTotalActiveSeconds()
                ),
                totalCenterCellStyle
        );
        for (int bucketIndex = 0; bucketIndex < applicationUsageMatrix.getPeriodBuckets().size(); bucketIndex++) {
            long bucketTotalSeconds = 0L;
            for (int applicationIndex = 0;
                 applicationIndex < applicationUsageMatrix.getApplicationNames().size();
                 applicationIndex++) {
                bucketTotalSeconds += applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex);
            }
            createCell(
                    dataRow,
                    bucketIndex + 2,
                    formatDurationCell(bucketTotalSeconds, applicationUsageMatrix.getTotalActiveSeconds()),
                    totalCenterCellStyle
            );
        }
    }

    private static String formatDurationCell(long durationSeconds, long totalActiveSeconds) {
        if (durationSeconds <= 0L) {
            return "—";
        }
        return DurationFormatter.formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
    }

    private static CellStyle createBoldCellStyle(
            Workbook workbook,
            int fontSize,
            HorizontalAlignment horizontalAlignment
    ) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) fontSize);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(horizontalAlignment);
        return cellStyle;
    }

    private static CellStyle createCellStyle(
            Workbook workbook,
            int fontSize,
            HorizontalAlignment horizontalAlignment
    ) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) fontSize);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(horizontalAlignment);
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
