package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
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
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Первый запуск: привязка к веб-аккаунту или автономный Free Solo.
 */
public final class ActivationDialog extends JDialog {

    private final AgentAccessClient agentAccessClient;
    private final AtomicBoolean confirmed = new AtomicBoolean(false);
    private final AtomicBoolean localSoloSelected = new AtomicBoolean(false);
    private final JTextField emailTextField = new JTextField();
    private final JPasswordField accessKeyPasswordField = new JPasswordField();
    private AgentAccessClient.AgentAuthResult agentAuthResult;

    public ActivationDialog(JFrame ownerFrame, AgentAccessClient agentAccessClient) {
        super(ownerFrame, Messages.get(MessageCodes.UI_ACTIVATION_TITLE), true);
        this.agentAccessClient = agentAccessClient;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(460, 380));
        setLocationRelativeTo(null);
        buildContent();
        pack();
        UiTheme.installRoundedWindowCorners(this);
    }

    private void buildContent() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBackground(UiTheme.BACKGROUND);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));

        JPanel cardPanel = new JPanel();
        UiTheme.styleSurfaceCard(cardPanel);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html><body style='width:340px'>"
                        + Messages.get(MessageCodes.UI_ACTIVATION_DESCRIPTION)
                        + "</body></html>"
        );
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
        UiTheme.styleMutedLabel(descriptionLabel);

        JLabel emailLabel = new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_EMAIL_LABEL));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(emailLabel);

        emailTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        emailTextField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel keyLabel = new JLabel(Messages.get(MessageCodes.UI_ACTIVATION_KEY_LABEL));
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(keyLabel);

        accessKeyPasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        accessKeyPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(descriptionLabel);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(emailLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(emailTextField);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(keyLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(accessKeyPasswordField);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.setOpaque(false);

        JButton useLocallyButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_USE_LOCALLY));
        UiTheme.styleSecondaryButton(useLocallyButton);
        useLocallyButton.addActionListener(actionEvent -> onUseLocally());
        buttonsPanel.add(useLocallyButton);

        JButton activateButton = new JButton(Messages.get(MessageCodes.UI_ACTIVATION_ACTIVATE));
        UiTheme.stylePrimaryButton(activateButton);
        activateButton.addActionListener(actionEvent -> onActivate());
        buttonsPanel.add(activateButton);

        rootPanel.add(cardPanel, BorderLayout.CENTER);
        rootPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(rootPanel);
    }

    private void onUseLocally() {
        localSoloSelected.set(true);
        confirmed.set(true);
        dispose();
    }

    private void onActivate() {
        String email = emailTextField.getText();
        String accessKey = new String(accessKeyPasswordField.getPassword());
        if (StringUtils.isBlank(email)) {
            showWarning(Messages.get(MessageCodes.UI_ACTIVATION_EMAIL_REQUIRED));
            return;
        }
        if (StringUtils.isBlank(accessKey)) {
            showWarning(Messages.get(MessageCodes.UI_ACTIVATION_KEY_REQUIRED));
            return;
        }
        AgentAccessClient.AgentAuthResult authResult = agentAccessClient.authenticate(
                email.trim(),
                accessKey.trim(),
                System.getenv("COMPUTERNAME"),
                null
        );
        if (Objects.isNull(authResult) || Objects.isNull(authResult.accessToken())) {
            showWarning(Messages.get(MessageCodes.UI_ACTIVATION_INVALID));
            return;
        }
        this.agentAuthResult = authResult;
        localSoloSelected.set(false);
        confirmed.set(true);
        dispose();
    }

    private void showWarning(String message) {
        UiDialogs.showMessage(
                message,
                Messages.get(MessageCodes.UI_ACTIVATION_TITLE),
                JOptionPane.WARNING_MESSAGE
        );
    }

    public boolean showAndWait() {
        setVisible(true);
        return confirmed.get();
    }

    public boolean isLocalSoloSelected() {
        return localSoloSelected.get();
    }

    public String getEmail() {
        return emailTextField.getText().trim();
    }

    public String getAccessKey() {
        return new String(accessKeyPasswordField.getPassword()).trim();
    }

    public AgentAccessClient.AgentAuthResult getAgentAuthResult() {
        return agentAuthResult;
    }
}
