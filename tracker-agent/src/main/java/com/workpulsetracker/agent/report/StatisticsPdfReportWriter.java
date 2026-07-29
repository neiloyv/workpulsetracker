package com.workpulsetracker.agent.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.workpulsetracker.agent.stats.ApplicationUsageMatrix;
import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * PDF-отчёт: страница на период (книжная ориентация); таблицы + круговая диаграмма в конце.
 */
public final class StatisticsPdfReportWriter {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsPdfReportWriter.class);

    private final StatisticsService statisticsService;
    private final int minorUsageThresholdMinutes;

    public StatisticsPdfReportWriter(StatisticsService statisticsService, int minorUsageThresholdMinutes) {
        this.statisticsService = Objects.requireNonNull(statisticsService);
        this.minorUsageThresholdMinutes = Math.max(minorUsageThresholdMinutes, 0);
    }

    public void writeToFile(Path reportFilePath) throws IOException {
        Objects.requireNonNull(reportFilePath);
        try (OutputStream outputStream = Files.newOutputStream(reportFilePath)) {
            Document document = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            try {
                ReportFonts reportFonts = ReportFonts.create();
                List<StatisticsReportSection> statisticsReportSections =
                        StatisticsReportSection.buildAll(statisticsService, minorUsageThresholdMinutes);
                for (int sectionIndex = 0; sectionIndex < statisticsReportSections.size(); sectionIndex++) {
                    if (sectionIndex > 0) {
                        document.newPage();
                    }
                    writePeriodPage(document, statisticsReportSections.get(sectionIndex), reportFonts);
                }
            } catch (DocumentException documentException) {
                throw new IOException("Failed to build PDF report", documentException);
            } finally {
                document.close();
            }
        }
    }

    private void writePeriodPage(
            Document document,
            StatisticsReportSection statisticsReportSection,
            ReportFonts reportFonts
    ) throws DocumentException {
        Paragraph titleParagraph = new Paragraph();
        titleParagraph.add(new Phrase(statisticsReportSection.getPeriodTitle(), reportFonts.titleFont()));
        titleParagraph.add(new Phrase(
                "  "
                        + Messages.get(MessageCodes.UI_STATS_TOTAL)
                        + " "
                        + DurationFormatter.formatHoursMinutes(statisticsReportSection.getTotalActiveSeconds()),
                reportFonts.bodyFont()
        ));
        document.add(titleParagraph);
        document.add(new Paragraph(" "));

        for (StatisticsReportTable reportTable : statisticsReportSection.getReportTables()) {
            if (reportTable.hasTitle()) {
                document.add(new Paragraph(reportTable.getTitle(), reportFonts.sectionFont()));
                document.add(new Paragraph(" "));
            }
            document.add(buildStatisticsTable(reportTable.getApplicationUsageMatrix(), reportFonts));
            document.add(new Paragraph(" "));
        }

        addPieChart(document, statisticsReportSection);
    }

    private void addPieChart(
            Document document,
            StatisticsReportSection statisticsReportSection
    ) throws DocumentException {
        try {
            byte[] pieChartPngBytes = ApplicationUsagePieChartImageRenderer.renderPng(
                    statisticsReportSection.getApplicationUsageSummaries(),
                    statisticsReportSection.getTotalActiveSeconds()
            );
            Image pieChartImage = Image.getInstance(pieChartPngBytes);
            pieChartImage.scaleToFit(480f, 250f);
            pieChartImage.setAlignment(Element.ALIGN_CENTER);
            document.add(pieChartImage);
        } catch (IOException ioException) {
            logger.warn("Failed to embed pie chart into PDF section {}: {}",
                    statisticsReportSection.getPeriodTitle(),
                    ioException.getMessage());
        }
    }

    private PdfPTable buildStatisticsTable(
            ApplicationUsageMatrix applicationUsageMatrix,
            ReportFonts reportFonts
    ) {
        int columnCount = 2 + applicationUsageMatrix.getPeriodBuckets().size();
        PdfPTable table = new PdfPTable(Math.max(columnCount, 2));
        table.setWidthPercentage(100);

        addHeaderCell(table, Messages.get(MessageCodes.UI_TABLE_APPLICATION), reportFonts.headerFont());
        addHeaderCell(table, Messages.get(MessageCodes.UI_TABLE_TOTAL), reportFonts.headerFont());
        applicationUsageMatrix.getPeriodBuckets().forEach(periodBucket ->
                addHeaderCell(table, periodBucket.getLabel(), reportFonts.headerFont())
        );

        if (applicationUsageMatrix.getApplicationNames().isEmpty()) {
            addBodyCell(table, Messages.get(MessageCodes.UI_STATS_EMPTY), reportFonts.bodyFont(), Element.ALIGN_LEFT);
            for (int columnIndex = 1; columnIndex < columnCount; columnIndex++) {
                addBodyCell(table, "", reportFonts.bodyFont(), Element.ALIGN_CENTER);
            }
            return table;
        }

        for (int applicationIndex = 0;
             applicationIndex < applicationUsageMatrix.getApplicationNames().size();
             applicationIndex++) {
            addBodyCell(
                    table,
                    applicationUsageMatrix.getApplicationNames().get(applicationIndex),
                    reportFonts.bodyFont(),
                    Element.ALIGN_LEFT
            );
            addBodyCell(
                    table,
                    formatDurationCell(
                            applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                            applicationUsageMatrix.getTotalActiveSeconds()
                    ),
                    reportFonts.bodyFont(),
                    Element.ALIGN_CENTER
            );
            for (int bucketIndex = 0;
                 bucketIndex < applicationUsageMatrix.getPeriodBuckets().size();
                 bucketIndex++) {
                addBodyCell(
                        table,
                        formatDurationCell(
                                applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex),
                                applicationUsageMatrix.getTotalActiveSeconds()
                        ),
                        reportFonts.bodyFont(),
                        Element.ALIGN_CENTER
                );
            }
        }

        addBodyCell(table, Messages.get(MessageCodes.UI_TABLE_TOTAL), reportFonts.headerFont(), Element.ALIGN_LEFT);
        addBodyCell(
                table,
                formatDurationCell(
                        applicationUsageMatrix.getTotalActiveSeconds(),
                        applicationUsageMatrix.getTotalActiveSeconds()
                ),
                reportFonts.headerFont(),
                Element.ALIGN_CENTER
        );
        for (int bucketIndex = 0; bucketIndex < applicationUsageMatrix.getPeriodBuckets().size(); bucketIndex++) {
            long bucketTotalSeconds = 0L;
            for (int applicationIndex = 0;
                 applicationIndex < applicationUsageMatrix.getApplicationNames().size();
                 applicationIndex++) {
                bucketTotalSeconds += applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex);
            }
            addBodyCell(
                    table,
                    formatDurationCell(bucketTotalSeconds, applicationUsageMatrix.getTotalActiveSeconds()),
                    reportFonts.headerFont(),
                    Element.ALIGN_CENTER
            );
        }
        return table;
    }

    private static String formatDurationCell(long durationSeconds, long totalActiveSeconds) {
        if (durationSeconds <= 0L) {
            return "—";
        }
        return DurationFormatter.formatHoursMinutesWithPercent(durationSeconds, totalActiveSeconds);
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

    private record ReportFonts(Font titleFont, Font sectionFont, Font bodyFont, Font headerFont) {

        private static ReportFonts create() {
            BaseFont baseFont = resolveUnicodeBaseFont();
            return new ReportFonts(
                    new Font(baseFont, 16, Font.BOLD),
                    new Font(baseFont, 12, Font.BOLD),
                    new Font(baseFont, 9, Font.NORMAL),
                    new Font(baseFont, 9, Font.BOLD)
            );
        }

        private static BaseFont resolveUnicodeBaseFont() {
            String[] fontCandidates = {
                    System.getenv("WINDIR") + "\\Fonts\\segoeui.ttf",
                    System.getenv("WINDIR") + "\\Fonts\\arial.ttf",
                    "C:\\Windows\\Fonts\\segoeui.ttf",
                    "C:\\Windows\\Fonts\\arial.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
            };
            for (String fontCandidate : fontCandidates) {
                if (StringUtils.isBlank(fontCandidate) || fontCandidate.startsWith("null")) {
                    continue;
                }
                try {
                    if (Files.exists(Path.of(fontCandidate))) {
                        return BaseFont.createFont(fontCandidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    }
                } catch (Exception exception) {
                    logger.debug("Unable to load PDF font {}: {}", fontCandidate, exception.getMessage());
                }
            }
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create PDF font", exception);
            }
        }
    }
}
