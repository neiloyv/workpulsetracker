package com.timetracker.agent.ui;

import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Первый запуск: ввод ключа привязки к веб-аккаунту или работа только локально.
 */
public final class ActivationDialog extends JDialog {

    private final AtomicBoolean confirmed = new AtomicBoolean(false);
    private final JTextField activationKeyTextField = new JTextField();
    private boolean localOnlySelected;

    public ActivationDialog(JFrame ownerFrame) {
        super(ownerFrame, Messages.get(MessageCodes.UI_ACTIVATION_TITLE), true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(440, 280));
        setLocationRelativeTo(ownerFrame);
        buildContent();
        pack();
    }

    private void buildContent() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBackground(UiTheme.BACKGROUND);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));

        JPanel cardPanel = new JPanel();
        UiTheme.styleSurfaceCard(cardPanel);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html><body style='width:320px'>"
                        + Messages.get(MessageCodes.UI_ACTIVATION_DESCRIPTION)
                        + "</body></html>"
        );
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
        UiTheme.styleMutedLabel(descriptionLabel);

        JLabel keyLabel = new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_KEY_LABEL));
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(keyLabel);

        activationKeyTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        activationKeyTextField.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(descriptionLabel);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(keyLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(activationKeyTextField);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.setOpaque(false);
        JButton localOnlyButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_LOCAL_ONLY));
        JButton activateButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_ACTIVATE));
        UiTheme.styleSecondaryButton(localOnlyButton);
        UiTheme.stylePrimaryButton(activateButton);
        activateButton.addActionListener(actionEvent -> onActivate());
        localOnlyButton.addActionListener(actionEvent -> onLocalOnly());
        buttonsPanel.add(localOnlyButton);
        buttonsPanel.add(activateButton);

        rootPanel.add(cardPanel, BorderLayout.CENTER);
        rootPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    private void onActivate() {
        String activationKey = activationKeyTextField.getText();
        if (StringUtils.isBlank(activationKey)) {
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_ACTIVATION_KEY_REQUIRED),
                    Messages.get(MessageCodes.UI_ACTIVATION_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        localOnlySelected = false;
        confirmed.set(true);
        dispose();
    }

    private void onLocalOnly() {
        localOnlySelected = true;
        confirmed.set(true);
        dispose();
    }

    public boolean showAndWait() {
        setVisible(true);
        return confirmed.get();
    }

    public boolean isLocalOnlySelected() {
        return localOnlySelected;
    }

    public String getActivationKey() {
        return activationKeyTextField.getText().trim();
    }
}
