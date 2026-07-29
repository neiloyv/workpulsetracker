package com.workpulsetracker.agent.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * PDF-отчёт по статистике: отдельная страница на каждый период.
 */
public final class StatisticsPdfReportWriter {

    private final StatisticsService statisticsService;
    private final int minorUsageThresholdMinutes;

    public StatisticsPdfReportWriter(StatisticsService statisticsService, int minorUsageThresholdMinutes) {
        this.statisticsService = Objects.requireNonNull(statisticsService);
        this.minorUsageThresholdMinutes = Math.max(minorUsageThresholdMinutes, 0);
    }

    public void writeToFile(Path reportFilePath) throws IOException {
        Objects.requireNonNull(reportFilePath);
        try (OutputStream outputStream = Files.newOutputStream(reportFilePath)) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            try {
                writePeriodPage(document, StatsPeriod.WEEK, Messages.get(MessageCodes.UI_STATS_PERIOD_WEEK));
                document.newPage();
                writePeriodPage(document, StatsPeriod.MONTH, Messages.get(MessageCodes.UI_STATS_PERIOD_MONTH));
                document.newPage();
                writePeriodPage(document, StatsPeriod.YEAR, Messages.get(MessageCodes.UI_STATS_PERIOD_YEAR));
                document.newPage();
                writePeriodPage(document, StatsPeriod.ALL_TIME, Messages.get(MessageCodes.UI_STATS_PERIOD_ALL));
            } catch (DocumentException documentException) {
                throw new IOException("Failed to build PDF report", documentException);
            } finally {
                document.close();
            }
        }
    }

    private void writePeriodPage(Document document, StatsPeriod statsPeriod, String periodTitle)
            throws DocumentException {
        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshot(statsPeriod);
        List<ApplicationUsageSummary> applicationUsageSummaries = ApplicationUsageFilter.groupMinorApplications(
                statisticsSnapshot.getApplicationUsageSummaries(),
                minorUsageThresholdMinutes
        );
        ApplicationUsageMatrix applicationUsageMatrix = ApplicationUsageFilter.groupMinorApplications(
                statisticsService.buildApplicationUsageMatrix(statsPeriod),
                minorUsageThresholdMinutes
        );

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        document.add(new Paragraph(periodTitle, titleFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                Messages.get(MessageCodes.UI_STATS_TOTAL) + " "
                        + DurationFormatter.formatHoursMinutes(statisticsSnapshot.getTotalActiveSeconds()),
                bodyFont
        ));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(Messages.get(MessageCodes.UI_STATS_BY_APP), sectionFont));
        document.add(new Paragraph(" "));
        document.add(buildSummaryTable(
                applicationUsageSummaries,
                statisticsSnapshot.getTotalActiveSeconds(),
                headerFont,
                bodyFont
        ));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(Messages.get(MessageCodes.UI_STATS_MATRIX), sectionFont));
        document.add(new Paragraph(" "));
        document.add(buildMatrixTable(applicationUsageMatrix, headerFont, bodyFont));
    }

    private PdfPTable buildSummaryTable(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            long totalActiveSeconds,
            Font headerFont,
            Font bodyFont
    ) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f});
        addHeaderCell(table, Messages.get(MessageCodes.UI_TABLE_APPLICATION), headerFont);
        addHeaderCell(table, Messages.get(MessageCodes.UI_TABLE_TIME), headerFont);

        if (applicationUsageSummaries.isEmpty()) {
            addBodyCell(table, Messages.get(MessageCodes.UI_STATS_EMPTY), bodyFont, Element.ALIGN_LEFT);
            addBodyCell(table, "", bodyFont, Element.ALIGN_CENTER);
            return table;
        }

        applicationUsageSummaries.forEach(applicationUsageSummary -> {
            addBodyCell(table, applicationUsageSummary.getApplicationName(), bodyFont, Element.ALIGN_LEFT);
            addBodyCell(
                    table,
                    DurationFormatter.formatHoursMinutesWithPercent(
                            applicationUsageSummary.getDurationSeconds(),
                            totalActiveSeconds
                    ),
                    bodyFont,
                    Element.ALIGN_CENTER
            );
        });
        return table;
    }

    private PdfPTable buildMatrixTable(
            ApplicationUsageMatrix applicationUsageMatrix,
            Font headerFont,
            Font bodyFont
    ) {
        int columnCount = 1 + applicationUsageMatrix.getPeriodBuckets().size();
        PdfPTable table = new PdfPTable(Math.max(columnCount, 1));
        table.setWidthPercentage(100);

        addHeaderCell(table, Messages.get(MessageCodes.UI_TABLE_APPLICATION), headerFont);
        applicationUsageMatrix.getPeriodBuckets().forEach(periodBucket ->
                addHeaderCell(table, periodBucket.getLabel(), headerFont)
        );

        if (applicationUsageMatrix.getApplicationNames().isEmpty()) {
            addBodyCell(table, Messages.get(MessageCodes.UI_STATS_EMPTY), bodyFont, Element.ALIGN_LEFT);
            for (int columnIndex = 1; columnIndex < columnCount; columnIndex++) {
                addBodyCell(table, "", bodyFont, Element.ALIGN_CENTER);
            }
            return table;
        }

        for (int applicationIndex = 0;
             applicationIndex < applicationUsageMatrix.getApplicationNames().size();
             applicationIndex++) {
            addBodyCell(
                    table,
                    applicationUsageMatrix.getApplicationNames().get(applicationIndex),
                    bodyFont,
                    Element.ALIGN_LEFT
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
                addBodyCell(table, cellValue, bodyFont, Element.ALIGN_CENTER);
            }
        }
        return table;
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(StringUtils.defaultString(value), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(0xEE, 0xF0, 0xF5));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, Font font, int horizontalAlignment) {
        PdfPCell cell = new PdfPCell(new Phrase(StringUtils.defaultString(value), font));
        cell.setHorizontalAlignment(horizontalAlignment);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
