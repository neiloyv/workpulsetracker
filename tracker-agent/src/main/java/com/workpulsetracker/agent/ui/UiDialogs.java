package com.workpulsetracker.agent.ui;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;

/**
 * Диалоги всегда по центру экрана (не относительно родительского окна).
 */
public final class UiDialogs {

    private UiDialogs() {
    }

    public static void showMessage(Object message, String title, int messageType) {
        JOptionPane optionPane = new JOptionPane(message, messageType);
        showCentered(optionPane, title);
    }

    public static int showConfirm(Object message, String title, int optionType, int messageType) {
        JOptionPane optionPane = new JOptionPane(message, messageType, optionType);
        showCentered(optionPane, title);
        Object selectedValue = optionPane.getValue();
        if (Objects.isNull(selectedValue) || !(selectedValue instanceof Integer)) {
            return JOptionPane.CLOSED_OPTION;
        }
        return (Integer) selectedValue;
    }

    private static void showCentered(JOptionPane optionPane, String title) {
        JDialog dialog = optionPane.createDialog((Component) null, title);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        dialog.dispose();
    }
}
