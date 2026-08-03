package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;

/**
 * Вкладка аккаунта: email, access key и тариф.
 */
public final class AccountPanel extends JPanel {

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final AgentAccessClient agentAccessClient;
    private final Runnable settingsChangedListener;

    private final JLabel accountTitleLabel = new JLabel();
    private final JLabel emailLabel = new JLabel();
    private final JLabel emailValueLabel = new JLabel();
    private final JLabel accessKeyLabel = new JLabel();
    private final JLabel accessKeyValueLabel = new JLabel();
    private final JLabel accessKeyHintLabel = new JLabel();
    private final JLabel tariffLabel = new JLabel();
    private final JLabel tariffValueLabel = new JLabel();
    private final JButton editAccessKeyButton = new JButton();
    private final JPanel accountCard = new JPanel();

    public AccountPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            AgentAccessClient agentAccessClient,
            Runnable settingsChangedListener
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.agentAccessClient = agentAccessClient;
        this.settingsChangedListener = settingsChangedListener;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        syncAccountLabels();
    }

    private void buildContent() {
        accountCard.setLayout(new BoxLayout(accountCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(accountCard);

        accountTitleLabel.setFont(accountTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        accountTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        UiTheme.styleMutedLabel(emailLabel);
        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        JPanel emailRow = createLabeledValueRow(emailLabel, emailValueLabel, null);

        UiTheme.styleMutedLabel(accessKeyLabel);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
        editAccessKeyButton.addActionListener(actionEvent -> onEditAccessKey());
        JPanel accessKeyRow = createLabeledValueRow(accessKeyLabel, accessKeyValueLabel, editAccessKeyButton);

        accessKeyHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(accessKeyHintLabel);

        UiTheme.styleMutedLabel(tariffLabel);
        tariffValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        JPanel tariffRow = createLabeledValueRow(tariffLabel, tariffValueLabel, null);

        accountCard.add(accountTitleLabel);
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(emailRow);
        accountCard.add(Box.createVerticalStrut(12));
        accountCard.add(accessKeyRow);
        accountCard.add(Box.createVerticalStrut(8));
        accountCard.add(accessKeyHintLabel);
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(createDivider());
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(tariffRow);
        accountCard.add(Box.createVerticalGlue());

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(accountCard, BorderLayout.NORTH);
        add(wrapperPanel, BorderLayout.CENTER);
        retranslate();
    }

    private JPanel createLabeledValueRow(JLabel captionLabel, JLabel valueLabel, JButton actionButton) {
        JPanel captionAndValuePanel = new JPanel();
        captionAndValuePanel.setOpaque(false);
        captionAndValuePanel.setLayout(new BoxLayout(captionAndValuePanel, BoxLayout.Y_AXIS));
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        captionAndValuePanel.add(captionLabel);
        captionAndValuePanel.add(Box.createVerticalStrut(4));
        captionAndValuePanel.add(valueLabel);

        JPanel rowPanel = new JPanel(new BorderLayout(16, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        rowPanel.add(captionAndValuePanel, BorderLayout.CENTER);
        if (Objects.nonNull(actionButton)) {
            JPanel actionWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actionWrapper.setOpaque(false);
            actionWrapper.add(actionButton);
            rowPanel.add(actionWrapper, BorderLayout.EAST);
        }
        return rowPanel;
    }

    private JPanel createDivider() {
        JPanel dividerPanel = new JPanel();
        dividerPanel.setOpaque(true);
        dividerPanel.setBackground(UiTheme.BORDER);
        dividerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        dividerPanel.setPreferredSize(new Dimension(1, 1));
        dividerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return dividerPanel;
    }

    public void retranslate() {
        accountTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCOUNT));
        emailLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_EMAIL));
        accessKeyLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY));
        accessKeyHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_HINT)
                        + "</body></html>"
        );
        editAccessKeyButton.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT));
        tariffLabel.setText(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF));
        tariffValueLabel.setText(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF_FREE));
        syncAccountLabels();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(accountCard);
        accountTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        tariffValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(emailLabel);
        UiTheme.styleMutedLabel(accessKeyLabel);
        UiTheme.styleMutedLabel(accessKeyHintLabel);
        UiTheme.styleMutedLabel(tariffLabel);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
    }

    private void syncAccountLabels() {
        emailValueLabel.setText(
                StringUtils.isNotBlank(userSettings.getEmail())
                        ? userSettings.getEmail()
                        : Messages.get(MessageCodes.UI_SETTINGS_EMAIL_EMPTY)
        );
        accessKeyValueLabel.setText(
                StringUtils.isNotBlank(userSettings.getActivationKey())
                        ? userSettings.getActivationKey()
                        : Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EMPTY)
        );
        editAccessKeyButton.setEnabled(StringUtils.isNotBlank(userSettings.getEmail()));
    }

    private void onEditAccessKey() {
        String email = userSettings.getEmail();
        if (StringUtils.isBlank(email)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_ACTIVATION_INVALID),
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JPasswordField accessKeyPasswordField = new JPasswordField(28);
        if (StringUtils.isNotBlank(userSettings.getActivationKey())) {
            accessKeyPasswordField.setText(userSettings.getActivationKey());
        }

        JPanel dialogContentPanel = new JPanel(new BorderLayout(0, 8));
        dialogContentPanel.add(
                new JLabel(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_HINT)),
                BorderLayout.NORTH
        );
        dialogContentPanel.add(accessKeyPasswordField, BorderLayout.CENTER);

        int dialogResult = UiDialogs.showConfirm(
                dialogContentPanel,
                Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (dialogResult != JOptionPane.OK_OPTION) {
            return;
        }

        String accessKey = new String(accessKeyPasswordField.getPassword()).trim();
        if (StringUtils.isBlank(accessKey)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_REQUIRED),
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (!agentAccessClient.validateAccess(email, accessKey)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_ACTIVATION_INVALID),
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        userSettings.updateAccessKey(accessKey);
        AgentAccessClient.AgentAuthResult agentAuthResult = agentAccessClient.authenticate(
                email,
                accessKey,
                System.getenv("COMPUTERNAME"),
                null
        );
        if (Objects.nonNull(agentAuthResult)) {
            userSettings.applyAgentAuth(
                    agentAuthResult.accessToken(),
                    agentAuthResult.hardwareId(),
                    agentAuthResult.workerId(),
                    agentAuthResult.deviceId()
            );
        }
        userSettingsStore.save(userSettings);
        syncAccountLabels();
        settingsChangedListener.run();
        UiDialogs.showMessage(
                Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_UPDATED),
                Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
