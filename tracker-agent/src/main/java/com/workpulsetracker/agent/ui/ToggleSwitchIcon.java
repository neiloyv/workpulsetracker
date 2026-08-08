package com.workpulsetracker.agent.ui;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Иконка в виде современного toggle switch (как в браузере / ОС).
 */
final class ToggleSwitchIcon implements Icon {

    private static final int TRACK_WIDTH = 40;
    private static final int TRACK_HEIGHT = 22;
    private static final int KNOB_SIZE = 16;
    private static final int HORIZONTAL_PADDING = 3;
    private static final int VERTICAL_PADDING = 3;

    private static final Color TRACK_OFF = new Color(0x2F, 0x2F, 0x47);
    private static final Color TRACK_OFF_HOVER = new Color(0x3A, 0x3A, 0x58);
    private static final Color TRACK_ON = UiTheme.ACCENT;
    private static final Color TRACK_ON_HOVER = UiTheme.ACCENT_HOVER;
    private static final Color TRACK_DISABLED = new Color(0x24, 0x24, 0x36);
    private static final Color KNOB = Color.WHITE;
    private static final Color KNOB_DISABLED = new Color(0x9E, 0x9E, 0xB5);

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = false;
            boolean enabled = component.isEnabled();
            boolean rollover = false;
            if (component instanceof AbstractButton abstractButton) {
                ButtonModel buttonModel = abstractButton.getModel();
                selected = buttonModel.isSelected();
                enabled = buttonModel.isEnabled();
                rollover = buttonModel.isRollover();
            }

            Color trackColor = resolveTrackColor(selected, enabled, rollover);
            Color knobColor = enabled ? KNOB : KNOB_DISABLED;

            float trackArc = TRACK_HEIGHT;
            RoundRectangle2D.Float trackShape = new RoundRectangle2D.Float(
                    x,
                    y + VERTICAL_PADDING,
                    TRACK_WIDTH,
                    TRACK_HEIGHT,
                    trackArc,
                    trackArc
            );
            graphics2D.setColor(trackColor);
            graphics2D.fill(trackShape);

            float knobX = selected
                    ? x + TRACK_WIDTH - HORIZONTAL_PADDING - KNOB_SIZE
                    : x + HORIZONTAL_PADDING;
            float knobY = y + VERTICAL_PADDING + ((TRACK_HEIGHT - KNOB_SIZE) / 2f);
            graphics2D.setColor(knobColor);
            graphics2D.fill(new Ellipse2D.Float(knobX, knobY, KNOB_SIZE, KNOB_SIZE));
        } finally {
            graphics2D.dispose();
        }
    }

    private static Color resolveTrackColor(boolean selected, boolean enabled, boolean rollover) {
        if (!enabled) {
            return selected ? new Color(0x3A, 0x35, 0x60) : TRACK_DISABLED;
        }
        if (selected) {
            return rollover ? TRACK_ON_HOVER : TRACK_ON;
        }
        return rollover ? TRACK_OFF_HOVER : TRACK_OFF;
    }

    @Override
    public int getIconWidth() {
        return TRACK_WIDTH;
    }

    @Override
    public int getIconHeight() {
        return TRACK_HEIGHT + (VERTICAL_PADDING * 2);
    }

    static void install(JComponent component) {
        if (!(component instanceof AbstractButton abstractButton)) {
            return;
        }
        ToggleSwitchIcon toggleSwitchIcon = new ToggleSwitchIcon();
        abstractButton.setIcon(toggleSwitchIcon);
        abstractButton.setSelectedIcon(toggleSwitchIcon);
        abstractButton.setDisabledIcon(toggleSwitchIcon);
        abstractButton.setDisabledSelectedIcon(toggleSwitchIcon);
        abstractButton.setRolloverIcon(toggleSwitchIcon);
        abstractButton.setRolloverSelectedIcon(toggleSwitchIcon);
        abstractButton.setPressedIcon(toggleSwitchIcon);
        abstractButton.setRolloverEnabled(true);
    }
}
