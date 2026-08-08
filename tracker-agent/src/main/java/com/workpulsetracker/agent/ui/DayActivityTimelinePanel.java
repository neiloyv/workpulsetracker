package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.DayActivityState;
import com.workpulsetracker.agent.stats.DayActivityTimeline;
import com.workpulsetracker.agent.stats.DayActivityTimelineSegment;
import com.workpulsetracker.agent.util.DurationFormatter;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

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
import java.util.List;
import java.util.Objects;

/**
 * Суточный таймлайн 00:00–24:00: зелёный = ACTIVE, серый = IDLE, янтарный = EXCLUDED, прозрачный = PC Off.
 */
public final class DayActivityTimelinePanel extends JPanel {

    private static final int TRACK_HEIGHT = 28;
    private static final int TRACK_TOP = 8;
    private static final int HOUR_LABEL_AREA_HEIGHT = 22;
    private static final int TRACK_BOTTOM_PADDING = 4;

    /**
     * Высота зоны полоски таймлайна (для выравнивания подписи даты).
     */
    public static int trackAreaHeight() {
        return TRACK_TOP + TRACK_HEIGHT + TRACK_BOTTOM_PADDING;
    }

    public static int trackTop() {
        return TRACK_TOP;
    }

    public static int trackHeight() {
        return TRACK_HEIGHT;
    }

    public static int trackBottomPadding() {
        return TRACK_BOTTOM_PADDING;
    }

    /**
     * Высота зоны подписей часов (для выравнивания дня недели).
     */
    public static int hourLabelAreaHeight() {
        return HOUR_LABEL_AREA_HEIGHT;
    }
    private static final int MIN_SEGMENT_WIDTH_PX = 2;
    private static final long SECONDS_PER_DAY = 24L * 3600L;
    private static final int LABEL_HOUR_STEP = 2;
    private static final int SUB_HOUR_TICK_MINUTES = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Color ACTIVE_COLOR = new Color(0x22, 0xC5, 0x5E);
    private static final Color IDLE_COLOR = new Color(0x6B, 0x6B, 0x80);
    private static final Color EXCLUDED_COLOR = new Color(0xF5, 0x9E, 0x0B);

    public static Color activeColor() {
        return ACTIVE_COLOR;
    }

    public static Color idleColor() {
        return IDLE_COLOR;
    }

    public static Color excludedColor() {
        return EXCLUDED_COLOR;
    }
    private static final Color HOUR_TICK_COLOR = new Color(0x5A, 0x5A, 0x78);
    private static final Color SUB_HOUR_TICK_COLOR = new Color(0x3E, 0x3E, 0x58);

    private DayActivityTimeline dayActivityTimeline = DayActivityTimeline.empty(
            LocalDateTime.now().toLocalDate().atStartOfDay(),
            LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay()
    );
    private int hoveredSegmentIndex = -1;
    private boolean showHourLabels = true;

    public DayActivityTimelinePanel() {
        setOpaque(false);
        updatePreferredHeight();
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
                        LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay()
                );
        this.hoveredSegmentIndex = -1;
        repaint();
    }

    public void setShowHourLabels(boolean showHourLabels) {
        this.showHourLabels = showHourLabels;
        updatePreferredHeight();
        revalidate();
        repaint();
    }

    private void updatePreferredHeight() {
        int preferredHeight = trackAreaHeight();
        if (showHourLabels) {
            preferredHeight += hourLabelAreaHeight();
        }
        setPreferredSize(new Dimension(100, preferredHeight));
        setMinimumSize(new Dimension(80, preferredHeight));
    }

    @SuppressWarnings("unused")
    public void setEmptyMessage(String emptyMessage) {
        // Сообщение не рисуется: пустой трек = PC Off на всей оси 00:00–24:00.
    }

    @Override
    public String getToolTipText(MouseEvent mouseEvent) {
        int segmentIndex = findSegmentIndexAtX(mouseEvent.getX());
        if (segmentIndex < 0 || segmentIndex >= dayActivityTimeline.getSegments().size()) {
            return null;
        }
        DayActivityTimelineSegment segment = dayActivityTimeline.getSegments().get(segmentIndex);
        String stateLabel = switch (segment.getActivityState()) {
            case ACTIVE -> Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE);
            case IDLE -> Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE);
            case EXCLUDED -> Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED);
        };
        return stateLabel
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
            int trackTop = TRACK_TOP;

            // Прозрачный/пустой фон трека = PC Off / агент не работал.
            graphics2D.setColor(new Color(UiTheme.SURFACE_2.getRed(), UiTheme.SURFACE_2.getGreen(),
                    UiTheme.SURFACE_2.getBlue(), 90));
            graphics2D.fill(new RoundRectangle2D.Double(
                    trackX,
                    trackTop,
                    trackWidth,
                    TRACK_HEIGHT,
                    10,
                    10
            ));

            List<DayActivityTimelineSegment> segments = dayActivityTimeline.getSegments();
            for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
                DayActivityTimelineSegment segment = segments.get(segmentIndex);
                int segmentX = trackX + toPixelOffset(segment.getStartDateTime(), trackWidth);
                int segmentEndX = trackX + toPixelOffset(segment.getEndDateTime(), trackWidth);
                int segmentWidth = Math.max(MIN_SEGMENT_WIDTH_PX, segmentEndX - segmentX);
                Color segmentColor = switch (segment.getActivityState()) {
                    case ACTIVE -> ACTIVE_COLOR;
                    case IDLE -> IDLE_COLOR;
                    case EXCLUDED -> EXCLUDED_COLOR;
                };
                if (segmentIndex == hoveredSegmentIndex) {
                    graphics2D.setColor(segmentColor.brighter());
                } else {
                    graphics2D.setColor(segmentColor);
                }
                graphics2D.fillRect(segmentX, trackTop + 2, segmentWidth, TRACK_HEIGHT - 4);
            }

            paintGrid(graphics2D, trackX, trackWidth, trackTop);

            graphics2D.setColor(UiTheme.BORDER);
            graphics2D.draw(new RoundRectangle2D.Double(
                    trackX,
                    trackTop,
                    trackWidth,
                    TRACK_HEIGHT,
                    10,
                    10
            ));
            if (showHourLabels) {
                paintHourLabels(graphics2D, trackX, trackWidth, trackTop);
            }
        } finally {
            graphics2D.dispose();
        }
    }

    private void paintGrid(Graphics2D graphics2D, int trackX, int trackWidth, int trackTop) {
        LocalDateTime rangeStartDateTime = dayActivityTimeline.getRangeStartDateTime();
        for (int minuteOfDay = 0; minuteOfDay <= 24 * 60; minuteOfDay += SUB_HOUR_TICK_MINUTES) {
            LocalDateTime tickDateTime = rangeStartDateTime.plusMinutes(minuteOfDay);
            int tickX = trackX + toPixelOffset(tickDateTime, trackWidth);
            boolean isHourTick = minuteOfDay % 60 == 0;
            if (isHourTick) {
                graphics2D.setColor(HOUR_TICK_COLOR);
                graphics2D.drawLine(tickX, trackTop + 2, tickX, trackTop + TRACK_HEIGHT - 2);
            } else {
                graphics2D.setColor(SUB_HOUR_TICK_COLOR);
                graphics2D.drawLine(tickX, trackTop + TRACK_HEIGHT / 2, tickX, trackTop + TRACK_HEIGHT - 2);
            }
        }
    }

    private void paintHourLabels(Graphics2D graphics2D, int trackX, int trackWidth, int trackTop) {
        LocalDateTime rangeStartDateTime = dayActivityTimeline.getRangeStartDateTime();
        graphics2D.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int labelY = trackTop + TRACK_HEIGHT + fontMetrics.getAscent() + 4;

        for (int hour = 0; hour <= 24; hour += LABEL_HOUR_STEP) {
            LocalDateTime tickDateTime = rangeStartDateTime.plusHours(hour);
            int tickX = trackX + toPixelOffset(tickDateTime, trackWidth);
            String tickLabel = hour == 24 ? "24:00" : TIME_FORMATTER.format(tickDateTime);
            int labelWidth = fontMetrics.stringWidth(tickLabel);
            int labelX;
            if (hour == 0) {
                labelX = tickX;
            } else if (hour == 24) {
                labelX = tickX - labelWidth;
            } else {
                labelX = tickX - labelWidth / 2;
            }
            graphics2D.setColor(UiTheme.TEXT_SECONDARY);
            graphics2D.drawString(tickLabel, labelX, labelY);
        }
    }

    private int toPixelOffset(LocalDateTime dateTime, int trackWidth) {
        long offsetSeconds = Duration.between(dayActivityTimeline.getRangeStartDateTime(), dateTime).getSeconds();
        offsetSeconds = Math.max(0L, Math.min(SECONDS_PER_DAY, offsetSeconds));
        return (int) Math.round((double) offsetSeconds * trackWidth / SECONDS_PER_DAY);
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
        List<DayActivityTimelineSegment> segments = dayActivityTimeline.getSegments();
        for (int segmentIndex = segments.size() - 1; segmentIndex >= 0; segmentIndex--) {
            DayActivityTimelineSegment segment = segments.get(segmentIndex);
            int segmentX = trackX + toPixelOffset(segment.getStartDateTime(), trackWidth);
            int segmentEndX = trackX + toPixelOffset(segment.getEndDateTime(), trackWidth);
            int segmentWidth = Math.max(MIN_SEGMENT_WIDTH_PX, segmentEndX - segmentX);
            if (mouseX >= segmentX && mouseX < segmentX + segmentWidth) {
                return segmentIndex;
            }
        }
        return -1;
    }
}
