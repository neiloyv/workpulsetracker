package com.workpulsetracker.agent.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Современная тема UI на FlatLaf + общие стили кнопок/панелей.
 */
public final class UiTheme {

    private static final Logger logger = LoggerFactory.getLogger(UiTheme.class);

    public static final Color BACKGROUND = new Color(0xF4, 0xF7, 0xF8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(0x1F, 0x2A, 0x30);
    public static final Color TEXT_SECONDARY = new Color(0x5B, 0x6B, 0x73);
    public static final Color ACCENT = new Color(0x0F, 0x76, 0x6E);
    public static final Color ACCENT_HOVER = new Color(0x0D, 0x5F, 0x59);
    public static final Color WARNING = new Color(0xC2, 0x41, 0x0C);
    public static final Color WARNING_HOVER = new Color(0x9A, 0x34, 0x0A);
    public static final Color BORDER = new Color(0xD7, 0xE0, 0xE5);

    private UiTheme() {
    }

    public static void install() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 999);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
            UIManager.put("TabbedPane.selectedBackground", SURFACE);
            UIManager.put("Panel.background", BACKGROUND);
            UIManager.put("TabbedPane.background", BACKGROUND);
            UIManager.put("Viewport.background", SURFACE);
            UIManager.put("List.background", SURFACE);
            UIManager.put("ScrollPane.background", SURFACE);
            UIManager.put("Label.foreground", TEXT_PRIMARY);
            UIManager.put("Button.background", ACCENT);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.hoverBackground", ACCENT_HOVER);
            UIManager.put("Button.focusedBackground", ACCENT_HOVER);
            UIManager.put("Button.default.background", ACCENT);
            UIManager.put("Button.default.foreground", Color.WHITE);
            UIManager.put("Button.default.hoverBackground", ACCENT_HOVER);
            UIManager.put("Component.focusColor", ACCENT);
            UIManager.put("Component.borderColor", BORDER);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception exception) {
            logger.warn("Failed to install FlatLaf theme: {}", exception.getMessage());
        }
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + "background: #0F766E;"
                        + "foreground: #FFFFFF;"
                        + "hoverBackground: #0D5F59;"
                        + "pressedBackground: #0A4F4A;"
                        + "font: bold +2;"
                        + "margin: 12,36,12,36");
    }

    public static void styleDangerButton(JButton button) {
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + "background: #C2410C;"
                        + "foreground: #FFFFFF;"
                        + "hoverBackground: #9A340A;"
                        + "pressedBackground: #7C2D12;"
                        + "font: bold +2;"
                        + "margin: 12,36,12,36");
    }

    public static void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + "background: #FFFFFF;"
                        + "foreground: #1F2A30;"
                        + "borderColor: #D7E0E5;"
                        + "hoverBackground: #EEF3F5;"
                        + "pressedBackground: #E2EAEF;"
                        + "margin: 8,18,8,18");
    }

    public static void styleSurfaceCard(JComponent component) {
        component.setBackground(SURFACE);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        component.setOpaque(true);
    }

    public static void styleMutedLabel(JLabel label) {
        label.setForeground(TEXT_SECONDARY);
    }

    public static void styleTimerLabel(JLabel label) {
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font("Segoe UI", Font.BOLD, 48));
        label.setHorizontalAlignment(JLabel.CENTER);
    }
}
