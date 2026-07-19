package com.timetracker.agent.ui;

import com.timetracker.common.i18n.Messages;
import com.timetracker.common.i18n.MessageCodes;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
        setMinimumSize(new Dimension(420, 220));
        setLocationRelativeTo(ownerFrame);
        buildContent();
        pack();
    }

    private void buildContent() {
        JPanel rootPanel = new JPanel(new BorderLayout(8, 8));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel descriptionLabel = new JLabel(
                "<html>" + Messages.get(MessageCodes.UI_ACTIVATION_DESCRIPTION) + "</html>"
        );
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel keyPanel = new JPanel(new BorderLayout(6, 6));
        keyPanel.add(new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_KEY_LABEL)), BorderLayout.NORTH);
        activationKeyTextField.setColumns(24);
        keyPanel.add(activationKeyTextField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton activateButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_ACTIVATE));
        JButton localOnlyButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_LOCAL_ONLY));

        activateButton.addActionListener(actionEvent -> onActivate());
        localOnlyButton.addActionListener(actionEvent -> onLocalOnly());

        buttonsPanel.add(localOnlyButton);
        buttonsPanel.add(activateButton);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        centerPanel.add(descriptionLabel);
        centerPanel.add(keyPanel);

        rootPanel.add(centerPanel, BorderLayout.CENTER);
        rootPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    private void onActivate() {
        String activationKey = activationKeyTextField.getText();
        if (org.apache.commons.lang3.StringUtils.isBlank(activationKey)) {
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
