package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.DayActivityTimeline;
import com.workpulsetracker.agent.stats.DayActivityTimelineSegment;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Горизонтальный таймлайн активности приложений за день.
 */
public final class DayActivityTimelinePanel extends JPanel {

    private static final int TRACK_HEIGHT = 28;
    private static final int TRACK_TOP = 8;
    private static final int HOUR_LABEL_AREA_HEIGHT = 20;
    private static final int MIN_SEGMENT_WIDTH_PX = 4;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private DayActivityTimeline dayActivityTimeline = DayActivityTimeline.empty(
            LocalDateTime.now().toLocalDate().atStartOfDay(),
            LocalDateTime.now()
    );
    private String emptyMessage = Messages.get(MessageCodes.UI_MAIN_NO_APPLICATIONS);
    private Map<String, Color> colorByApplicationName = Map.of();
    private int hoveredSegmentIndex = -1;

    public DayActivityTimelinePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(100, TRACK_TOP + TRACK_HEIGHT + HOUR_LABEL_AREA_HEIGHT + 4));
        setMinimumSize(new Dimension(80, TRACK_TOP + TRACK_HEIGHT + HOUR_LABEL_AREA_HEIGHT + 4));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                int segmentIndex = findSegmentIndexAtX(mouseEvent.getX());
                if (segmentIndex != hoveredSegmentIndex) {
                    hoveredSegmentIndex = segmentIndex;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                if (hoveredSegmentIndex >= 0) {
                    hoveredSegmentIndex = -1;
                    repaint();
                }
            }
        };
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    public void setTimeline(DayActivityTimeline dayActivityTimeline) {
        this.dayActivityTimeline = Objects.nonNull(dayActivityTimeline)
                ? dayActivityTimeline
                : DayActivityTimeline.empty(
                        LocalDateTime.now().toLocalDate().atStartOfDay(),
                        LocalDateTime.now()
                );
        this.colorByApplicationName = buildColorMap(this.dayActivityTimeline.getSegments());
        this.hoveredSegmentIndex = -1;
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
    public String getToolTipText(MouseEvent mouseEvent) {
        int segmentIndex = findSegmentIndexAtX(mouseEvent.getX());
        if (segmentIndex < 0 || segmentIndex >= dayActivityTimeline.getSegments().size()) {
            return null;
        }
        DayActivityTimelineSegment segment = dayActivityTimeline.getSegments().get(segmentIndex);
        return segment.getApplicationName()
                + "  "
                + TIME_FORMATTER.format(segment.getStartDateTime())
                + "–"
                + TIME_FORMATTER.format(segment.getEndDateTime())
                + "  "
                + DurationFormatter.formatSeconds(segment.getDurationSeconds());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int trackWidth = Math.max(getWidth() - 4, 1);
            int trackX = 2;
            graphics2D.setColor(UiTheme.SURFACE_2);
            graphics2D.fill(new RoundRectangle2D.Double(
                    trackX,
                    TRACK_TOP,
                    trackWidth,
                    TRACK_HEIGHT,
                    10,
                    10
            ));

            if (dayActivityTimeline.isEmpty()) {
                paintEmptyState(graphics2D, trackX, trackWidth);
                paintHourLabels(graphics2D, trackX, trackWidth);
                return;
            }

            long rangeSeconds = Math.max(
                    1L,
                    Duration.between(
                            dayActivityTimeline.getRangeStartDateTime(),
                            dayActivityTimeline.getRangeEndDateTime()
                    ).getSeconds()
            );
            List<DayActivityTimelineSegment> segments = dayActivityTimeline.getSegments();
            for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
                DayActivityTimelineSegment segment = segments.get(segmentIndex);
                int segmentX = trackX + toPixelOffset(segment.getStartDateTime(), rangeSeconds, trackWidth);
                int segmentEndX = trackX + toPixelOffset(segment.getEndDateTime(), rangeSeconds, trackWidth);
                int segmentWidth = Math.max(MIN_SEGMENT_WIDTH_PX, segmentEndX - segmentX);
                Color segmentColor = colorByApplicationName.getOrDefault(
                        segment.getApplicationName(),
                        ApplicationUsageColorPalette.colorForIndex(0)
                );
                if (segmentIndex == hoveredSegmentIndex) {
                    graphics2D.setColor(segmentColor.brighter());
                } else {
                    graphics2D.setColor(segmentColor);
                }
                graphics2D.fillRect(segmentX, TRACK_TOP + 2, segmentWidth, TRACK_HEIGHT - 4);
            }

            graphics2D.setColor(UiTheme.BORDER);
            graphics2D.draw(new RoundRectangle2D.Double(
                    trackX,
                    TRACK_TOP,
                    trackWidth,
                    TRACK_HEIGHT,
                    10,
                    10
            ));
            paintHourLabels(graphics2D, trackX, trackWidth);
        } finally {
            graphics2D.dispose();
        }
    }

    private void paintEmptyState(Graphics2D graphics2D, int trackX, int trackWidth) {
        graphics2D.setColor(UiTheme.TEXT_SECONDARY);
        graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int textX = trackX + Math.max(0, (trackWidth - fontMetrics.stringWidth(emptyMessage)) / 2);
        int textY = TRACK_TOP + (TRACK_HEIGHT + fontMetrics.getAscent() - fontMetrics.getDescent()) / 2;
        graphics2D.drawString(emptyMessage, textX, textY);
    }

    private void paintHourLabels(Graphics2D graphics2D, int trackX, int trackWidth) {
        LocalDateTime rangeStartDateTime = dayActivityTimeline.getRangeStartDateTime();
        LocalDateTime rangeEndDateTime = dayActivityTimeline.getRangeEndDateTime();
        long rangeSeconds = Math.max(1L, Duration.between(rangeStartDateTime, rangeEndDateTime).getSeconds());

        graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int labelY = TRACK_TOP + TRACK_HEIGHT + fontMetrics.getAscent() + 4;

        long tickStepMinutes;
        if (rangeSeconds <= 30 * 60L) {
            tickStepMinutes = 5L;
        } else if (rangeSeconds <= 2 * 3600L) {
            tickStepMinutes = 15L;
        } else if (rangeSeconds <= 6 * 3600L) {
            tickStepMinutes = 30L;
        } else {
            tickStepMinutes = 60L;
        }

        long startEpochMinute = rangeStartDateTime.getHour() * 60L
                + rangeStartDateTime.getMinute();
        long alignedMinute = ((startEpochMinute + tickStepMinutes - 1) / tickStepMinutes) * tickStepMinutes;
        LocalDateTime tickDateTime = rangeStartDateTime
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(alignedMinute);
        if (tickDateTime.isBefore(rangeStartDateTime)) {
            tickDateTime = tickDateTime.plusMinutes(tickStepMinutes);
        }

        while (!tickDateTime.isAfter(rangeEndDateTime)) {
            int tickX = trackX + toPixelOffset(tickDateTime, rangeSeconds, trackWidth);
            String tickLabel = tickStepMinutes >= 60L
                    ? String.valueOf(tickDateTime.getHour())
                    : TIME_FORMATTER.format(tickDateTime);
            int labelWidth = fontMetrics.stringWidth(tickLabel);
            graphics2D.setColor(UiTheme.BORDER);
            graphics2D.drawLine(tickX, TRACK_TOP + TRACK_HEIGHT - 4, tickX, TRACK_TOP + TRACK_HEIGHT);
            graphics2D.setColor(UiTheme.TEXT_SECONDARY);
            graphics2D.drawString(tickLabel, tickX - labelWidth / 2, labelY);
            tickDateTime = tickDateTime.plusMinutes(tickStepMinutes);
        }
    }

    private int toPixelOffset(LocalDateTime dateTime, long rangeSeconds, int trackWidth) {
        long offsetSeconds = Duration.between(dayActivityTimeline.getRangeStartDateTime(), dateTime).getSeconds();
        offsetSeconds = Math.max(0L, Math.min(rangeSeconds, offsetSeconds));
        return (int) Math.round((double) offsetSeconds * trackWidth / rangeSeconds);
    }

    private int findSegmentIndexAtX(int mouseX) {
        if (dayActivityTimeline.isEmpty()) {
            return -1;
        }
        int trackWidth = Math.max(getWidth() - 4, 1);
        int trackX = 2;
        if (mouseX < trackX || mouseX > trackX + trackWidth) {
            return -1;
        }
        long rangeSeconds = Math.max(
                1L,
                Duration.between(
                        dayActivityTimeline.getRangeStartDateTime(),
                        dayActivityTimeline.getRangeEndDateTime()
                ).getSeconds()
        );
        List<DayActivityTimelineSegment> segments = dayActivityTimeline.getSegments();
        for (int segmentIndex = segments.size() - 1; segmentIndex >= 0; segmentIndex--) {
            DayActivityTimelineSegment segment = segments.get(segmentIndex);
            int segmentX = trackX + toPixelOffset(segment.getStartDateTime(), rangeSeconds, trackWidth);
            int segmentEndX = trackX + toPixelOffset(segment.getEndDateTime(), rangeSeconds, trackWidth);
            int segmentWidth = Math.max(MIN_SEGMENT_WIDTH_PX, segmentEndX - segmentX);
            if (mouseX >= segmentX && mouseX < segmentX + segmentWidth) {
                return segmentIndex;
            }
        }
        return -1;
    }

    private static Map<String, Color> buildColorMap(List<DayActivityTimelineSegment> segments) {
        Map<String, Long> durationByApplicationName = segments.stream()
                .collect(Collectors.groupingBy(
                        DayActivityTimelineSegment::getApplicationName,
                        Collectors.summingLong(DayActivityTimelineSegment::getDurationSeconds)
                ));
        List<String> rankedApplicationNames = durationByApplicationName.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        Map<String, Color> colorByApplicationName = new HashMap<>();
        for (int applicationIndex = 0; applicationIndex < rankedApplicationNames.size(); applicationIndex++) {
            colorByApplicationName.put(
                    rankedApplicationNames.get(applicationIndex),
                    ApplicationUsageColorPalette.colorForIndex(applicationIndex)
            );
        }
        return colorByApplicationName;
    }
}
