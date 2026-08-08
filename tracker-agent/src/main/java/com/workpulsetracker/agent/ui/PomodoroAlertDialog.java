package com.workpulsetracker.agent.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagLayout;

/**
 * Центрированное always-on-top окно завершения фазы Pomodoro.
 */
public final class PomodoroAlertDialog {

    private PomodoroAlertDialog() {
    }

    public static void show(String title, String message) {
        JDialog dialog = new JDialog((Frame) null, title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        UiTheme.installRoundedWindowCorners(dialog);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(UiTheme.SURFACE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));

        JLabel messageLabel = new JLabel(
                "<html><body style='width:320px;text-align:center'>" + message + "</body></html>",
                SwingConstants.CENTER
        );
        messageLabel.setForeground(UiTheme.TEXT_PRIMARY);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 16f));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton okButton = new JButton(Messages.get(MessageCodes.UI_POMODORO_ALERT_OK));
        UiTheme.stylePrimaryButton(okButton);
        okButton.setPreferredSize(new Dimension(160, 44));
        okButton.addActionListener(actionEvent -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);

        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().putClientProperty(FlatClientProperties.STYLE, "background: #141422");
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, 200));
        dialog.setLocationRelativeTo(null);
        dialog.toFront();
        dialog.requestFocus();
        dialog.setVisible(true);
        dialog.dispose();
    }
}
