package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageSummary;
import com.workpulsetracker.agent.util.PercentageCalculator;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Круговая (donut) диаграмма использования приложений в процентах.
 * Проценты на секторах, легенда справа.
 */
public final class ApplicationUsagePieChartPanel extends JPanel {

    private static final int MIN_LABEL_PERCENTAGE = 4;

    private List<ApplicationUsageSummary> applicationUsageSummaries = Collections.emptyList();
    private long totalActiveSeconds;
    private String emptyMessage = Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS);

    public ApplicationUsagePieChartPanel() {
        setOpaque(false);
    }

    public void setUsageData(List<ApplicationUsageSummary> applicationUsageSummaries, long totalActiveSeconds) {
        this.applicationUsageSummaries = Objects.isNull(applicationUsageSummaries)
                ? Collections.emptyList()
                : List.copyOf(applicationUsageSummaries);
        this.totalActiveSeconds = Math.max(totalActiveSeconds, 0L);
        repaint();
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = StringUtils.defaultIfBlank(
                emptyMessage,
                Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS)
        );
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            List<Slice> slices = buildSlices();
            if (slices.isEmpty()) {
                paintEmptyState(graphics2D);
                return;
            }

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int legendWidth = Math.min(180, Math.max(110, panelWidth / 3));
            int chartAreaWidth = panelWidth - legendWidth - 12;
            int chartSize = Math.min(chartAreaWidth - 12, panelHeight - 12);
            chartSize = Math.max(chartSize, 72);

            int chartX = Math.max(4, (chartAreaWidth - chartSize) / 2);
            int chartY = Math.max(4, (panelHeight - chartSize) / 2);
            paintDonut(graphics2D, slices, chartX, chartY, chartSize);
            paintLegend(graphics2D, slices, chartAreaWidth + 8, legendWidth);
        } finally {
            graphics2D.dispose();
        }
    }

    private List<Slice> buildSlices() {
        if (applicationUsageSummaries.isEmpty() || totalActiveSeconds <= 0L) {
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
                        positiveSummaries.get(index).getApplicationName(),
                        percentages.get(index),
                        ApplicationUsageColorPalette.colorForIndex(index)
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

    private void paintEmptyState(Graphics2D graphics2D) {
        graphics2D.setColor(UiTheme.TEXT_SECONDARY);
        graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(emptyMessage);
        int textX = Math.max(0, (getWidth() - textWidth) / 2);
        int textY = Math.max(fontMetrics.getAscent(), getHeight() / 2);
        graphics2D.drawString(emptyMessage, textX, textY);
    }

    private void paintDonut(Graphics2D graphics2D, List<Slice> slices, int chartX, int chartY, int chartSize) {
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

        int holeInset = Math.max(18, chartSize / 3);
        int holeSize = chartSize - (holeInset * 2);
        graphics2D.setColor(UiTheme.SURFACE);
        graphics2D.fillOval(chartX + holeInset, chartY + holeInset, holeSize, holeSize);

        graphics2D.setColor(UiTheme.BORDER);
        graphics2D.setStroke(new BasicStroke(1.2f));
        graphics2D.drawOval(chartX, chartY, chartSize, chartSize);
        graphics2D.drawOval(chartX + holeInset, chartY + holeInset, holeSize, holeSize);

        String centerLabel = "100%";
        graphics2D.setColor(UiTheme.TEXT_PRIMARY);
        graphics2D.setFont(getFont().deriveFont(Font.BOLD, 15f));
        FontMetrics centerFontMetrics = graphics2D.getFontMetrics();
        int labelX = chartX + (chartSize - centerFontMetrics.stringWidth(centerLabel)) / 2;
        int labelY = chartY + (chartSize + centerFontMetrics.getAscent() - centerFontMetrics.getDescent()) / 2;
        graphics2D.drawString(centerLabel, labelX, labelY);

        paintSlicePercentLabels(graphics2D, slices, midAngles, chartX, chartY, chartSize, holeInset);
    }

    private void paintSlicePercentLabels(
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
        graphics2D.setFont(getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        for (int sliceIndex = 0; sliceIndex < slices.size(); sliceIndex++) {
            Slice slice = slices.get(sliceIndex);
            if (slice.percentage() < MIN_LABEL_PERCENTAGE) {
                continue;
            }
            double midAngleDegrees = midAngles.get(sliceIndex);
            double midAngleRadians = Math.toRadians(midAngleDegrees);
            int textX = (int) Math.round(centerX + labelRadius * Math.cos(midAngleRadians));
            int textY = (int) Math.round(centerY - labelRadius * Math.sin(midAngleRadians));
            String percentLabel = slice.percentage() + "%";
            int textWidth = fontMetrics.stringWidth(percentLabel);
            int drawX = textX - textWidth / 2;
            int drawY = textY + (fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;

            graphics2D.setColor(new Color(0, 0, 0, 110));
            graphics2D.drawString(percentLabel, drawX + 1, drawY + 1);
            graphics2D.setColor(Color.WHITE);
            graphics2D.drawString(percentLabel, drawX, drawY);
        }
    }

    private void paintLegend(Graphics2D graphics2D, List<Slice> slices, int legendLeft, int legendWidth) {
        int rowHeight = 22;
        int availableHeight = getHeight() - 8;
        int maxRows = Math.max(1, availableHeight / rowHeight);
        int legendTop = Math.max(4, (getHeight() - Math.min(slices.size(), maxRows) * rowHeight) / 2);

        graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        for (int sliceIndex = 0; sliceIndex < slices.size() && sliceIndex < maxRows; sliceIndex++) {
            Slice slice = slices.get(sliceIndex);
            int itemY = legendTop + sliceIndex * rowHeight;

            graphics2D.setColor(slice.color());
            graphics2D.fillRoundRect(legendLeft, itemY + 4, 10, 10, 3, 3);

            String legendText = truncateLegendText(
                    slice.applicationName() + "  " + slice.percentage() + "%",
                    fontMetrics,
                    legendWidth - 20
            );
            graphics2D.setColor(UiTheme.TEXT_SECONDARY);
            graphics2D.drawString(legendText, legendLeft + 16, itemY + fontMetrics.getAscent() + 2);
        }
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
    }
}
