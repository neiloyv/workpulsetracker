package com.workpulsetracker.agent.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;

/**
 * Тёмная тема UI на FlatLaf. Палитра согласована с tracker-ui.
 */
public final class UiTheme {

    private static final Logger logger = LoggerFactory.getLogger(UiTheme.class);

    public static final Color BACKGROUND = new Color(0x0A, 0x0A, 0x14);
    public static final Color SURFACE = new Color(0x14, 0x14, 0x22);
    public static final Color SURFACE_2 = new Color(0x1A, 0x1A, 0x2B);
    public static final Color TEXT_PRIMARY = new Color(0xED, 0xED, 0xF6);
    public static final Color TEXT_SECONDARY = new Color(0x9E, 0x9E, 0xB5);
    public static final Color ACCENT = new Color(0x74, 0x58, 0xFF);
    public static final Color ACCENT_HOVER = new Color(0x63, 0x38, 0xF2);
    public static final Color WARNING = new Color(0xC2, 0x41, 0x0C);
    public static final Color WARNING_HOVER = new Color(0x9A, 0x34, 0x0A);
    public static final Color BORDER = new Color(0x2F, 0x2F, 0x47);

    private UiTheme() {
    }

    public static void install() {
        try {
            // Native decorations keep OS resize borders (left/right/top/bottom) working.
            FlatLaf.setUseNativeWindowDecorations(true);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            FlatDarkLaf.setup();

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
            UIManager.put("Table.background", SURFACE);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", ACCENT);
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.gridColor", BORDER);
            UIManager.put("TableHeader.background", SURFACE_2);
            UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
            UIManager.put("ScrollPane.background", SURFACE);
            UIManager.put("ComboBox.background", SURFACE);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
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
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("TitlePane.background", BACKGROUND);
            UIManager.put("TitlePane.foreground", TEXT_PRIMARY);
            UIManager.put("TitlePane.inactiveBackground", BACKGROUND);
            UIManager.put("TitlePane.inactiveForeground", TEXT_SECONDARY);
            UIManager.put("RootPane.background", BACKGROUND);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception exception) {
            logger.warn("Failed to install FlatLaf theme: {}", exception.getMessage());
        }
    }

    /**
     * Настраивает декорации окна FlatLaf с возможностью ресайза по краям.
     * Не форсируем setUndecorated/setShape — иначе на Windows пропадает системный ресайз.
     */
    public static void installRoundedWindowCorners(Window window) {
        if (window instanceof JFrame frame) {
            frame.setResizable(true);
            frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            return;
        }
        if (window instanceof JDialog dialog) {
            dialog.setResizable(true);
            dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        }
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + "background: #7458FF;"
                        + "foreground: #FFFFFF;"
                        + "hoverBackground: #6338F2;"
                        + "pressedBackground: #5329D1;"
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
                        + "background: #141422;"
                        + "foreground: #EDEDF6;"
                        + "borderColor: #2F2F47;"
                        + "hoverBackground: #1A1A2B;"
                        + "pressedBackground: #1A1A2B;"
                        + "margin: 8,18,8,18");
    }

    public static void styleCompactSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + "background: #141422;"
                        + "foreground: #EDEDF6;"
                        + "borderColor: #2F2F47;"
                        + "hoverBackground: #1A1A2B;"
                        + "pressedBackground: #1A1A2B;"
                        + "margin: 6,14,6,14");
    }

    public static void styleToggleSwitch(javax.swing.JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setText("");
        checkBox.putClientProperty(FlatClientProperties.STYLE_CLASS, "toggleSwitch");
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

    public static void styleUsageTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setBackground(SURFACE_2);
        tableHeader.setForeground(TEXT_SECONDARY);
        tableHeader.setReorderingAllowed(false);
    }
}
