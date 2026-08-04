package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.mode.AgentFeature;
import com.workpulsetracker.agent.mode.FeatureGateService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;

/**
 * Вкладка аккаунта: Free Solo / NETWORK_SYNC, подключение и отключение облака.
 */
public final class AccountPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(AccountPanel.class);
    private static final Color LOCAL_BANNER_BACKGROUND = new Color(0x2A, 0x2A, 0x3F);
    private static final Color SYNCED_BANNER_BACKGROUND = new Color(0x1A, 0x3A, 0x2E);

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final AgentAccessClient agentAccessClient;
    private final AgentSyncClient agentSyncClient;
    private final FeatureGateService featureGateService;
    private final Runnable settingsChangedListener;

    private final JLabel accountTitleLabel = new JLabel();
    private final JLabel modeBannerLabel = new JLabel();
    private final JLabel soloEmailLabel = new JLabel();
    private final JTextField emailInputField = new JTextField();
    private final JLabel soloAccessKeyLabel = new JLabel();
    private final JPasswordField accessKeyInputField = new JPasswordField();
    private final JLabel accessKeyHintLabel = new JLabel();
    private final JButton connectWebAccountButton = new JButton();

    private final JLabel syncedEmailLabel = new JLabel();
    private final JLabel emailValueLabel = new JLabel();
    private final JLabel syncedAccessKeyLabel = new JLabel();
    private final JLabel accessKeyValueLabel = new JLabel();
    private final JLabel linkedDetailsLabel = new JLabel();
    private final JButton editAccessKeyButton = new JButton();
    private final JButton disconnectCloudButton = new JButton();

    private final JLabel tariffLabel = new JLabel();
    private final JLabel tariffValueLabel = new JLabel();
    private final JPanel accountCard = new JPanel();
    private final JPanel soloConnectPanel = new JPanel();
    private final JPanel syncedDetailsPanel = new JPanel();

    public AccountPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            AgentAccessClient agentAccessClient,
            AgentSyncClient agentSyncClient,
            Runnable settingsChangedListener
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.agentAccessClient = agentAccessClient;
        this.agentSyncClient = agentSyncClient;
        this.featureGateService = new FeatureGateService(userSettings);
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

        modeBannerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeBannerLabel.setOpaque(true);
        modeBannerLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        modeBannerLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        soloConnectPanel.setOpaque(false);
        soloConnectPanel.setLayout(new BoxLayout(soloConnectPanel, BoxLayout.Y_AXIS));
        soloConnectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        UiTheme.styleMutedLabel(soloEmailLabel);
        emailInputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        emailInputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(soloAccessKeyLabel);
        accessKeyInputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        accessKeyInputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        accessKeyHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(accessKeyHintLabel);
        UiTheme.stylePrimaryButton(connectWebAccountButton);
        connectWebAccountButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        connectWebAccountButton.addActionListener(actionEvent -> onConnectWebAccount());

        soloConnectPanel.add(soloEmailLabel);
        soloConnectPanel.add(Box.createVerticalStrut(6));
        soloConnectPanel.add(emailInputField);
        soloConnectPanel.add(Box.createVerticalStrut(12));
        soloConnectPanel.add(soloAccessKeyLabel);
        soloConnectPanel.add(Box.createVerticalStrut(6));
        soloConnectPanel.add(accessKeyInputField);
        soloConnectPanel.add(Box.createVerticalStrut(8));
        soloConnectPanel.add(accessKeyHintLabel);
        soloConnectPanel.add(Box.createVerticalStrut(12));
        soloConnectPanel.add(connectWebAccountButton);

        syncedDetailsPanel.setOpaque(false);
        syncedDetailsPanel.setLayout(new BoxLayout(syncedDetailsPanel, BoxLayout.Y_AXIS));
        syncedDetailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
        editAccessKeyButton.addActionListener(actionEvent -> onEditAccessKey());
        linkedDetailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(linkedDetailsLabel);
        UiTheme.styleSecondaryButton(disconnectCloudButton);
        disconnectCloudButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        disconnectCloudButton.addActionListener(actionEvent -> onDisconnectCloud());

        syncedDetailsPanel.add(createLabeledValueRow(syncedEmailLabel, emailValueLabel, null));
        syncedDetailsPanel.add(Box.createVerticalStrut(12));
        syncedDetailsPanel.add(createLabeledValueRow(syncedAccessKeyLabel, accessKeyValueLabel, editAccessKeyButton));
        syncedDetailsPanel.add(Box.createVerticalStrut(8));
        syncedDetailsPanel.add(linkedDetailsLabel);
        syncedDetailsPanel.add(Box.createVerticalStrut(12));
        syncedDetailsPanel.add(disconnectCloudButton);

        UiTheme.styleMutedLabel(tariffLabel);
        tariffValueLabel.setForeground(UiTheme.TEXT_PRIMARY);

        accountCard.add(accountTitleLabel);
        accountCard.add(Box.createVerticalStrut(12));
        accountCard.add(modeBannerLabel);
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(soloConnectPanel);
        accountCard.add(syncedDetailsPanel);
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(createDivider());
        accountCard.add(Box.createVerticalStrut(16));
        accountCard.add(createLabeledValueRow(tariffLabel, tariffValueLabel, null));
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
        soloEmailLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_EMAIL));
        syncedEmailLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_EMAIL));
        soloAccessKeyLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY));
        syncedAccessKeyLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY));
        accessKeyHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_HINT)
                        + "</body></html>"
        );
        editAccessKeyButton.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT));
        connectWebAccountButton.setText(Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB));
        disconnectCloudButton.setText(Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_CLOUD));
        tariffLabel.setText(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF));
        syncAccountLabels();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(accountCard);
        accountTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        tariffValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(soloEmailLabel);
        UiTheme.styleMutedLabel(syncedEmailLabel);
        UiTheme.styleMutedLabel(soloAccessKeyLabel);
        UiTheme.styleMutedLabel(syncedAccessKeyLabel);
        UiTheme.styleMutedLabel(accessKeyHintLabel);
        UiTheme.styleMutedLabel(linkedDetailsLabel);
        UiTheme.styleMutedLabel(tariffLabel);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
        UiTheme.stylePrimaryButton(connectWebAccountButton);
        UiTheme.styleSecondaryButton(disconnectCloudButton);
        syncAccountLabels();
    }

    private void syncAccountLabels() {
        boolean localSoloMode = featureGateService.isLocalSoloMode();
        soloConnectPanel.setVisible(localSoloMode);
        syncedDetailsPanel.setVisible(!localSoloMode);

        if (localSoloMode) {
            modeBannerLabel.setText(
                    "<html><body style='width:420px'><b>"
                            + Messages.get(MessageCodes.UI_ACCOUNT_MODE_LOCAL_BANNER)
                            + "</b></body></html>"
            );
            modeBannerLabel.setBackground(LOCAL_BANNER_BACKGROUND);
            modeBannerLabel.setForeground(UiTheme.TEXT_PRIMARY);
            tariffValueLabel.setText(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF_FREE_SOLO));
        } else {
            modeBannerLabel.setText(
                    "<html><body style='width:420px'><b>"
                            + Messages.get(MessageCodes.UI_ACCOUNT_MODE_SYNCED_BANNER)
                            + "</b></body></html>"
            );
            modeBannerLabel.setBackground(SYNCED_BANNER_BACKGROUND);
            modeBannerLabel.setForeground(UiTheme.TEXT_PRIMARY);
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
            linkedDetailsLabel.setText(buildLinkedDetailsText());
            tariffValueLabel.setText(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF_PRO_SYNCED));
            editAccessKeyButton.setEnabled(StringUtils.isNotBlank(userSettings.getEmail()));
        }
        revalidate();
        repaint();
    }

    private String buildLinkedDetailsText() {
        StringBuilder linkedDetailsBuilder = new StringBuilder("<html><body style='width:420px'>");
        if (Objects.nonNull(userSettings.getWorkerId())) {
            linkedDetailsBuilder
                    .append(Messages.get(MessageCodes.UI_ACCOUNT_LINKED_WORKER, userSettings.getWorkerId()))
                    .append("<br/>");
        }
        if (Objects.nonNull(userSettings.getDeviceId())) {
            linkedDetailsBuilder
                    .append(Messages.get(MessageCodes.UI_ACCOUNT_LINKED_DEVICE, userSettings.getDeviceId()))
                    .append("<br/>");
        }
        if (StringUtils.isNotBlank(userSettings.getHardwareId())) {
            linkedDetailsBuilder.append(
                    Messages.get(MessageCodes.UI_ACCOUNT_LINKED_HARDWARE, userSettings.getHardwareId())
            );
        }
        linkedDetailsBuilder.append("</body></html>");
        return linkedDetailsBuilder.toString();
    }

    private void onConnectWebAccount() {
        String email = emailInputField.getText().trim();
        String accessKey = new String(accessKeyInputField.getPassword()).trim();
        if (StringUtils.isBlank(email)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_ACTIVATION_EMAIL_REQUIRED),
                    Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (StringUtils.isBlank(accessKey)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_REQUIRED),
                    Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        AgentAccessClient.AgentAuthResult agentAuthResult = agentAccessClient.authenticate(
                email,
                accessKey,
                System.getenv("COMPUTERNAME"),
                null
        );
        if (Objects.isNull(agentAuthResult) || Objects.isNull(agentAuthResult.accessToken())) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_ACTIVATION_INVALID),
                    Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        userSettings.applyCredentials(email, accessKey);
        userSettings.applyAgentAuth(
                agentAuthResult.accessToken(),
                agentAuthResult.hardwareId(),
                agentAuthResult.workerId(),
                agentAuthResult.deviceId()
        );
        userSettingsStore.save(userSettings);
        accessKeyInputField.setText("");
        syncAccountLabels();
        settingsChangedListener.run();
        triggerBackgroundSyncFlush();
        UiDialogs.showMessage(
                Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_SUCCESS),
                Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onDisconnectCloud() {
        int confirmResult = UiDialogs.showConfirm(
                Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_CONFIRM),
                Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_CLOUD),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmResult != JOptionPane.YES_OPTION) {
            return;
        }
        userSettings.applyLocalOnlyMode();
        userSettingsStore.save(userSettings);
        syncAccountLabels();
        settingsChangedListener.run();
        UiDialogs.showMessage(
                Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_SUCCESS),
                Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_CLOUD),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void triggerBackgroundSyncFlush() {
        if (!featureGateService.isFeatureAllowed(AgentFeature.SYNC_TO_CLOUD)) {
            return;
        }
        Thread syncFlushThread = new Thread(() -> {
            try {
                agentSyncClient.uploadTelemetry(userSettings);
                logger.info("Immediate telemetry flush after cloud connect completed");
            } catch (Exception exception) {
                logger.warn("Immediate telemetry flush after cloud connect failed: {}", exception.getMessage());
            }
        }, "account-connect-sync-flush");
        syncFlushThread.setDaemon(true);
        syncFlushThread.start();
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
        AgentAccessClient.AgentAuthResult agentAuthResult = agentAccessClient.authenticate(
                email,
                accessKey,
                System.getenv("COMPUTERNAME"),
                null
        );
        if (Objects.isNull(agentAuthResult) || Objects.isNull(agentAuthResult.accessToken())) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_ACTIVATION_INVALID),
                    Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        userSettings.updateAccessKey(accessKey);
        userSettings.applyAgentAuth(
                agentAuthResult.accessToken(),
                agentAuthResult.hardwareId(),
                agentAuthResult.workerId(),
                agentAuthResult.deviceId()
        );
        userSettingsStore.save(userSettings);
        syncAccountLabels();
        settingsChangedListener.run();
        triggerBackgroundSyncFlush();
        UiDialogs.showMessage(
                Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_UPDATED),
                Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT_TITLE),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
