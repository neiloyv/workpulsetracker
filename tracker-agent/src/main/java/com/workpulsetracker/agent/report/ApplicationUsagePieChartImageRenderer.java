package com.workpulsetracker.agent.report;

import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.util.ApplicationDisplayNameResolver;
import com.workpulsetracker.agent.util.PercentageCalculator;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Рендер donut-диаграммы использования приложений в PNG (для Excel/PDF отчётов).
 */
public final class ApplicationUsagePieChartImageRenderer {

    private static final int MIN_LABEL_PERCENTAGE = 4;
    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 320;
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color HOLE = Color.WHITE;
    private static final Color BORDER = new Color(0xCF, 0xD4, 0xDE);
    private static final Color TEXT_PRIMARY = new Color(0x1F, 0x29, 0x37);
    private static final Color TEXT_SECONDARY = new Color(0x4B, 0x55, 0x63);
    private static final Color[] SLICE_COLORS = {
            new Color(0x74, 0x58, 0xFF),
            new Color(0x22, 0xC5, 0x5E),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0x3B, 0x82, 0xF6),
            new Color(0xEC, 0x48, 0x99),
            new Color(0x14, 0xB8, 0xA6),
            new Color(0xF9, 0x73, 0x16),
            new Color(0x8B, 0x5C, 0xF6),
            new Color(0xEF, 0x44, 0x44),
            new Color(0x06, 0xB6, 0xD4)
    };

    private ApplicationUsagePieChartImageRenderer() {
    }

    public static byte[] renderPng(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            long totalActiveSeconds
    ) throws IOException {
        return renderPng(applicationUsageSummaries, totalActiveSeconds, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static byte[] renderPng(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            long totalActiveSeconds,
            int imageWidth,
            int imageHeight
    ) throws IOException {
        BufferedImage bufferedImage = renderImage(
                applicationUsageSummaries,
                totalActiveSeconds,
                imageWidth,
                imageHeight
        );
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static BufferedImage renderImage(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            long totalActiveSeconds,
            int imageWidth,
            int imageHeight
    ) {
        int safeWidth = Math.max(imageWidth, 320);
        int safeHeight = Math.max(imageHeight, 200);
        BufferedImage bufferedImage = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.setColor(BACKGROUND);
            graphics2D.fillRect(0, 0, safeWidth, safeHeight);

            List<Slice> slices = buildSlices(applicationUsageSummaries, totalActiveSeconds);
            if (slices.isEmpty()) {
                paintEmptyState(graphics2D, safeWidth, safeHeight);
                return bufferedImage;
            }

            int legendWidth = Math.min(240, Math.max(140, safeWidth / 3));
            int chartAreaWidth = safeWidth - legendWidth - 16;
            int chartSize = Math.min(chartAreaWidth - 16, safeHeight - 24);
            chartSize = Math.max(chartSize, 120);

            int chartX = Math.max(8, (chartAreaWidth - chartSize) / 2);
            int chartY = Math.max(8, (safeHeight - chartSize) / 2);
            paintDonut(graphics2D, slices, chartX, chartY, chartSize);
            paintLegend(graphics2D, slices, chartAreaWidth + 8, legendWidth, safeHeight);
        } finally {
            graphics2D.dispose();
        }
        return bufferedImage;
    }

    private static List<Slice> buildSlices(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            long totalActiveSeconds
    ) {
        if (Objects.isNull(applicationUsageSummaries)
                || applicationUsageSummaries.isEmpty()
                || totalActiveSeconds <= 0L) {
            return Collections.emptyList();
        }
        List<ApplicationUsageSummary> positiveSummaries = applicationUsageSummaries.stream()
                .filter(applicationUsageSummary -> applicationUsageSummary.getDurationSeconds() > 0L)
                .collect(Collectors.toCollection(ArrayList::new));
        if (positiveSummaries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> percentages = positiveSummaries.stream()
                .map(applicationUsageSummary -> PercentageCalculator.calculatePercentage(
                        applicationUsageSummary.getDurationSeconds(),
                        totalActiveSeconds
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        adjustPercentagesToHundred(percentages);

        return IntStream.range(0, positiveSummaries.size())
                .filter(index -> percentages.get(index) > 0)
                .mapToObj(index -> new Slice(
                        ApplicationDisplayNameResolver.resolveDisplayName(
                                positiveSummaries.get(index).getApplicationName()
                        ),
                        percentages.get(index),
                        SLICE_COLORS[index % SLICE_COLORS.length]
                ))
                .collect(Collectors.toList());
    }

    private static void adjustPercentagesToHundred(List<Integer> percentages) {
        int percentageSum = percentages.stream().mapToInt(Integer::intValue).sum();
        if (percentageSum == 100 || percentages.isEmpty()) {
            return;
        }
        int largestIndex = IntStream.range(0, percentages.size())
                .boxed()
                .max((leftIndex, rightIndex) -> Integer.compare(
                        percentages.get(leftIndex),
                        percentages.get(rightIndex)
                ))
                .orElse(0);
        percentages.set(largestIndex, Math.max(0, percentages.get(largestIndex) + (100 - percentageSum)));
    }

    private static void paintEmptyState(Graphics2D graphics2D, int imageWidth, int imageHeight) {
        String emptyMessage = Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS);
        graphics2D.setColor(TEXT_SECONDARY);
        graphics2D.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int textX = Math.max(0, (imageWidth - fontMetrics.stringWidth(emptyMessage)) / 2);
        int textY = Math.max(fontMetrics.getAscent(), imageHeight / 2);
        graphics2D.drawString(emptyMessage, textX, textY);
    }

    private static void paintDonut(
            Graphics2D graphics2D,
            List<Slice> slices,
            int chartX,
            int chartY,
            int chartSize
    ) {
        double startAngle = 90.0d;
        List<Double> midAngles = new ArrayList<>();
        for (Slice slice : slices) {
            double extentAngle = -(slice.percentage() * 3.6d);
            graphics2D.setColor(slice.color());
            graphics2D.fill(new Arc2D.Double(
                    chartX,
                    chartY,
                    chartSize,
                    chartSize,
                    startAngle,
                    extentAngle,
                    Arc2D.PIE
            ));
            midAngles.add(startAngle + extentAngle / 2.0d);
            startAngle += extentAngle;
        }

        int holeInset = Math.max(22, chartSize / 3);
        int holeSize = chartSize - (holeInset * 2);
        graphics2D.setColor(HOLE);
        graphics2D.fillOval(chartX + holeInset, chartY + holeInset, holeSize, holeSize);

        graphics2D.setColor(BORDER);
        graphics2D.setStroke(new BasicStroke(1.2f));
        graphics2D.drawOval(chartX, chartY, chartSize, chartSize);
        graphics2D.drawOval(chartX + holeInset, chartY + holeInset, holeSize, holeSize);

        String centerLabel = "100%";
        graphics2D.setColor(TEXT_PRIMARY);
        graphics2D.setFont(new Font("Segoe UI", Font.BOLD, 14));
        FontMetrics centerFontMetrics = graphics2D.getFontMetrics();
        int labelX = chartX + (chartSize - centerFontMetrics.stringWidth(centerLabel)) / 2;
        int labelY = chartY + (chartSize + centerFontMetrics.getAscent() - centerFontMetrics.getDescent()) / 2;
        graphics2D.drawString(centerLabel, labelX, labelY);

        paintSlicePercentLabels(graphics2D, slices, midAngles, chartX, chartY, chartSize, holeInset);
    }

    private static void paintSlicePercentLabels(
            Graphics2D graphics2D,
            List<Slice> slices,
            List<Double> midAngles,
            int chartX,
            int chartY,
            int chartSize,
            int holeInset
    ) {
        double centerX = chartX + chartSize / 2.0d;
        double centerY = chartY + chartSize / 2.0d;
        double outerRadius = chartSize / 2.0d;
        double innerRadius = Math.max(outerRadius - holeInset, outerRadius * 0.35d);
        double labelRadius = (outerRadius + innerRadius) / 2.0d;
        graphics2D.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        IntStream.range(0, slices.size()).forEach(sliceIndex -> {
            Slice slice = slices.get(sliceIndex);
            if (slice.percentage() < MIN_LABEL_PERCENTAGE) {
                return;
            }
            double midAngleRadians = Math.toRadians(midAngles.get(sliceIndex));
            int textX = (int) Math.round(centerX + labelRadius * Math.cos(midAngleRadians));
            int textY = (int) Math.round(centerY - labelRadius * Math.sin(midAngleRadians));
            String percentLabel = slice.percentage() + "%";
            int drawX = textX - fontMetrics.stringWidth(percentLabel) / 2;
            int drawY = textY + (fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;

            graphics2D.setColor(new Color(0, 0, 0, 110));
            graphics2D.drawString(percentLabel, drawX + 1, drawY + 1);
            graphics2D.setColor(Color.WHITE);
            graphics2D.drawString(percentLabel, drawX, drawY);
        });
    }

    private static void paintLegend(
            Graphics2D graphics2D,
            List<Slice> slices,
            int legendLeft,
            int legendWidth,
            int imageHeight
    ) {
        int rowHeight = 20;
        int maxRows = Math.max(1, (imageHeight - 16) / rowHeight);
        int legendTop = Math.max(8, (imageHeight - Math.min(slices.size(), maxRows) * rowHeight) / 2);
        graphics2D.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        IntStream.range(0, Math.min(slices.size(), maxRows)).forEach(sliceIndex -> {
            Slice slice = slices.get(sliceIndex);
            int itemY = legendTop + sliceIndex * rowHeight;
            graphics2D.setColor(slice.color());
            graphics2D.fillRoundRect(legendLeft, itemY + 4, 10, 10, 3, 3);

            String legendText = truncateLegendText(
                    slice.applicationName() + "  " + slice.percentage() + "%",
                    fontMetrics,
                    legendWidth - 20
            );
            graphics2D.setColor(TEXT_SECONDARY);
            graphics2D.drawString(legendText, legendLeft + 16, itemY + fontMetrics.getAscent() + 2);
        });
    }

    private static String truncateLegendText(String text, FontMetrics fontMetrics, int maxWidth) {
        if (fontMetrics.stringWidth(text) <= maxWidth || maxWidth <= 0) {
            return text;
        }
        String ellipsis = "...";
        int endIndex = text.length();
        while (endIndex > 0 && fontMetrics.stringWidth(text.substring(0, endIndex) + ellipsis) > maxWidth) {
            endIndex--;
        }
        return endIndex <= 0 ? ellipsis : text.substring(0, endIndex) + ellipsis;
    }

    private record Slice(String applicationName, int percentage, Color color) {
        private Slice {
            applicationName = StringUtils.defaultString(applicationName);
        }
    }
}
