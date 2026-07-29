package com.workpulsetracker.agent.ui;

import java.awt.Color;

/**
 * Общая палитра цветов приложений (pie chart, timeline).
 */
public final class ApplicationUsageColorPalette {

    private static final Color[] COLORS = {
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

    private ApplicationUsageColorPalette() {
    }

    public static Color colorForIndex(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }

    public static int size() {
        return COLORS.length;
    }
}
