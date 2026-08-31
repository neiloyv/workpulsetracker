package com.workpulsetracker.agent.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.workpulsetracker.agent.api.AgentAccessClient;
import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.LocalDataBackupService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.agent.util.WindowsLaunchAtLoginService;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final ActivityStore activityStore;
    private final TrackingEngine trackingEngine;
    private final LocalDataBackupService localDataBackupService = new LocalDataBackupService();
    private final Runnable settingsChangedListener;
    private final Runnable localDataRestoredListener;

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

    private final JLabel tariffValueLabel = new JLabel();
    private final JLabel dataTitleLabel = new JLabel();
    private final JLabel backupHintLabel = new JLabel();
    private final JButton exportBackupButton = new JButton();
    private final JButton importBackupButton = new JButton();
    private final JLabel syncTitleLabel = new JLabel();
    private final JLabel syncHintLabel = new JLabel();
    private final JButton syncNowButton = new JButton();
    private final JPanel connectionCard = new JPanel();
    private final JPanel dataSyncCard = new JPanel();
    private final JPanel soloConnectPanel = new JPanel();
    private final JPanel syncedDetailsPanel = new JPanel();

    public AccountPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            AgentAccessClient agentAccessClient,
            AgentSyncClient agentSyncClient,
            ActivityStore activityStore,
            TrackingEngine trackingEngine,
            Runnable settingsChangedListener,
            Runnable localDataRestoredListener
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.agentAccessClient = agentAccessClient;
        this.agentSyncClient = agentSyncClient;
        this.activityStore = activityStore;
        this.trackingEngine = trackingEngine;
        this.settingsChangedListener = settingsChangedListener;
        this.localDataRestoredListener = localDataRestoredListener;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        syncAccountLabels();
    }

    private void buildContent() {
        accountTitleLabel.setFont(accountTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        accountTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        tariffValueLabel.setFont(tariffValueLabel.getFont().deriveFont(java.awt.Font.PLAIN, 14f));
        tariffValueLabel.setForeground(UiTheme.TEXT_SECONDARY);
        tariffValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel accountHeaderPanel = new JPanel(new BorderLayout(16, 0));
        accountHeaderPanel.setOpaque(false);
        accountHeaderPanel.add(accountTitleLabel, BorderLayout.WEST);
        accountHeaderPanel.add(tariffValueLabel, BorderLayout.EAST);

        connectionCard.setLayout(new BoxLayout(connectionCard, BoxLayout.Y_AXIS));
        dataSyncCard.setLayout(new BoxLayout(dataSyncCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(connectionCard);
        UiTheme.styleSurfaceCard(dataSyncCard);

        modeBannerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeBannerLabel.setOpaque(true);
        modeBannerLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        modeBannerLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        soloConnectPanel.setOpaque(false);
        soloConnectPanel.setLayout(new BoxLayout(soloConnectPanel, BoxLayout.Y_AXIS));
        soloConnectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleFieldCaptionLabel(soloEmailLabel);
        styleFieldCaptionLabel(soloAccessKeyLabel);
        emailInputField.setPreferredSize(new Dimension(200, 36));
        accessKeyInputField.setPreferredSize(new Dimension(200, 36));
        accessKeyHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(accessKeyHintLabel);
        UiTheme.stylePrimaryButton(connectWebAccountButton);
        styleCompactActionButton(connectWebAccountButton, true);
        connectWebAccountButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        connectWebAccountButton.addActionListener(actionEvent -> onConnectWebAccount());

        soloConnectPanel.add(createFieldColumn(soloEmailLabel, emailInputField));
        soloConnectPanel.add(Box.createVerticalStrut(12));
        soloConnectPanel.add(createFieldColumn(soloAccessKeyLabel, accessKeyInputField));
        soloConnectPanel.add(Box.createVerticalStrut(8));
        soloConnectPanel.add(accessKeyHintLabel);
        soloConnectPanel.add(Box.createVerticalStrut(12));
        soloConnectPanel.add(connectWebAccountButton);

        syncedDetailsPanel.setOpaque(false);
        syncedDetailsPanel.setLayout(new BoxLayout(syncedDetailsPanel, BoxLayout.Y_AXIS));
        syncedDetailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        styleFieldCaptionLabel(syncedEmailLabel);
        styleFieldCaptionLabel(syncedAccessKeyLabel);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
        editAccessKeyButton.addActionListener(actionEvent -> onEditAccessKey());
        linkedDetailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(linkedDetailsLabel);
        UiTheme.styleSecondaryButton(disconnectCloudButton);
        styleCompactActionButton(disconnectCloudButton, false);
        disconnectCloudButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        disconnectCloudButton.addActionListener(actionEvent -> onDisconnectCloud());

        syncedDetailsPanel.add(createValueColumn(syncedEmailLabel, emailValueLabel, null));
        syncedDetailsPanel.add(Box.createVerticalStrut(12));
        syncedDetailsPanel.add(createValueColumn(syncedAccessKeyLabel, accessKeyValueLabel, editAccessKeyButton));
        syncedDetailsPanel.add(Box.createVerticalStrut(8));
        syncedDetailsPanel.add(linkedDetailsLabel);
        syncedDetailsPanel.add(Box.createVerticalStrut(12));
        syncedDetailsPanel.add(disconnectCloudButton);

        connectionCard.add(modeBannerLabel);
        connectionCard.add(Box.createVerticalStrut(16));
        connectionCard.add(soloConnectPanel);
        connectionCard.add(syncedDetailsPanel);
        connectionCard.add(Box.createVerticalGlue());

        dataTitleLabel.setFont(dataTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        dataTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        dataTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(backupHintLabel);
        backupHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleSecondaryButton(exportBackupButton);
        UiTheme.styleSecondaryButton(importBackupButton);
        exportBackupButton.addActionListener(actionEvent -> onExportBackupClicked());
        importBackupButton.addActionListener(actionEvent -> onImportBackupClicked());

        JPanel backupButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        backupButtonsPanel.setOpaque(false);
        backupButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        backupButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        backupButtonsPanel.add(exportBackupButton);
        backupButtonsPanel.add(importBackupButton);

        syncTitleLabel.setFont(syncTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        syncTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        syncTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(syncHintLabel);
        syncHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleSecondaryButton(syncNowButton);
        syncNowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        syncNowButton.addActionListener(actionEvent -> onSyncNowClicked());

        dataSyncCard.add(dataTitleLabel);
        dataSyncCard.add(Box.createVerticalStrut(8));
        dataSyncCard.add(backupHintLabel);
        dataSyncCard.add(Box.createVerticalStrut(12));
        dataSyncCard.add(backupButtonsPanel);
        dataSyncCard.add(Box.createVerticalStrut(16));
        dataSyncCard.add(createDivider());
        dataSyncCard.add(Box.createVerticalStrut(16));
        dataSyncCard.add(syncTitleLabel);
        dataSyncCard.add(Box.createVerticalStrut(8));
        dataSyncCard.add(syncHintLabel);
        dataSyncCard.add(Box.createVerticalStrut(12));
        dataSyncCard.add(syncNowButton);
        dataSyncCard.add(Box.createVerticalGlue());

        JPanel columnsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        columnsPanel.setOpaque(false);
        columnsPanel.add(connectionCard);
        columnsPanel.add(dataSyncCard);

        add(accountHeaderPanel, BorderLayout.NORTH);
        add(columnsPanel, BorderLayout.CENTER);
        retranslate();
    }

    private JPanel createFieldColumn(JLabel captionLabel, JComponent fieldComponent) {
        JPanel columnPanel = new JPanel();
        columnPanel.setOpaque(false);
        columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
        columnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        columnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldComponent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        columnPanel.add(captionLabel);
        columnPanel.add(Box.createVerticalStrut(4));
        columnPanel.add(fieldComponent);
        return columnPanel;
    }

    private static void styleFieldCaptionLabel(JLabel captionLabel) {
        captionLabel.setForeground(UiTheme.TEXT_PRIMARY);
        captionLabel.setFont(captionLabel.getFont().deriveFont(java.awt.Font.BOLD));
    }

    private static void styleCompactActionButton(JButton button, boolean primary) {
        if (primary) {
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999;"
                            + "background: #7458FF;"
                            + "foreground: #FFFFFF;"
                            + "hoverBackground: #6338F2;"
                            + "pressedBackground: #5329D1;"
                            + "font: bold;"
                            + "margin: 6,20,6,20");
        } else {
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999;"
                            + "background: #141422;"
                            + "foreground: #EDEDF6;"
                            + "borderColor: #2F2F47;"
                            + "hoverBackground: #1A1A2B;"
                            + "pressedBackground: #1A1A2B;"
                            + "margin: 6,20,6,20");
        }
        button.setPreferredSize(null);
        button.setMinimumSize(null);
        button.setMaximumSize(null);
        Dimension preferredSize = button.getPreferredSize();
        int buttonWidth = Math.max(220, preferredSize.width);
        Dimension buttonSize = new Dimension(buttonWidth, 36);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
    }

    private JPanel createValueColumn(JLabel captionLabel, JLabel valueLabel, JButton actionButton) {
        JPanel valueRow = new JPanel(new BorderLayout(8, 0));
        valueRow.setOpaque(false);
        valueRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueRow.add(valueLabel, BorderLayout.CENTER);
        if (Objects.nonNull(actionButton)) {
            valueRow.add(actionButton, BorderLayout.EAST);
        }

        JPanel columnPanel = new JPanel();
        columnPanel.setOpaque(false);
        columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
        columnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        columnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        columnPanel.add(captionLabel);
        columnPanel.add(Box.createVerticalStrut(4));
        columnPanel.add(valueRow);
        return columnPanel;
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
                "<html><body style='width:260px'>"
                        + Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_HINT)
                        + "</body></html>"
        );
        editAccessKeyButton.setText(Messages.get(MessageCodes.UI_SETTINGS_ACCESS_KEY_EDIT));
        connectWebAccountButton.setText(Messages.get(MessageCodes.UI_ACCOUNT_CONNECT_WEB));
        styleCompactActionButton(connectWebAccountButton, true);
        disconnectCloudButton.setText(Messages.get(MessageCodes.UI_ACCOUNT_DISCONNECT_CLOUD));
        styleCompactActionButton(disconnectCloudButton, false);
        dataTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_DATA));
        backupHintLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_BACKUP_HINT));
        exportBackupButton.setText(Messages.get(MessageCodes.UI_SETTINGS_BACKUP_EXPORT));
        importBackupButton.setText(Messages.get(MessageCodes.UI_SETTINGS_BACKUP_IMPORT));
        syncTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_SYNC));
        syncNowButton.setText(Messages.get(MessageCodes.UI_SETTINGS_SYNC_NOW));
        syncAccountLabels();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(connectionCard);
        UiTheme.styleSurfaceCard(dataSyncCard);
        accountTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        emailValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        accessKeyValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        tariffValueLabel.setForeground(UiTheme.TEXT_SECONDARY);
        dataTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        dataTitleLabel.setFont(dataTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        syncTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        styleFieldCaptionLabel(soloEmailLabel);
        styleFieldCaptionLabel(syncedEmailLabel);
        styleFieldCaptionLabel(soloAccessKeyLabel);
        styleFieldCaptionLabel(syncedAccessKeyLabel);
        UiTheme.styleMutedLabel(accessKeyHintLabel);
        UiTheme.styleMutedLabel(linkedDetailsLabel);
        UiTheme.styleMutedLabel(backupHintLabel);
        UiTheme.styleMutedLabel(syncHintLabel);
        UiTheme.styleCompactSecondaryButton(editAccessKeyButton);
        UiTheme.stylePrimaryButton(connectWebAccountButton);
        styleCompactActionButton(connectWebAccountButton, true);
        UiTheme.styleSecondaryButton(disconnectCloudButton);
        styleCompactActionButton(disconnectCloudButton, false);
        UiTheme.styleSecondaryButton(exportBackupButton);
        UiTheme.styleSecondaryButton(importBackupButton);
        UiTheme.styleSecondaryButton(syncNowButton);
        syncAccountLabels();
    }

    private void syncAccountLabels() {
        boolean localSoloMode = userSettings.getOperationMode().isLocalSolo();
        soloConnectPanel.setVisible(localSoloMode);
        syncedDetailsPanel.setVisible(!localSoloMode);

        if (localSoloMode) {
            modeBannerLabel.setText(
                    "<html><body style='width:260px'><b>"
                            + Messages.get(MessageCodes.UI_ACCOUNT_MODE_LOCAL_BANNER)
                            + "</b></body></html>"
            );
            modeBannerLabel.setBackground(LOCAL_BANNER_BACKGROUND);
            modeBannerLabel.setForeground(UiTheme.TEXT_PRIMARY);
            tariffValueLabel.setText(formatTariffHeader(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF_FREE_SOLO)));
        } else {
            modeBannerLabel.setText(
                    "<html><body style='width:260px'><b>"
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
            tariffValueLabel.setText(formatTariffHeader(Messages.get(MessageCodes.UI_ACCOUNT_TARIFF_PRO_SYNCED)));
            editAccessKeyButton.setEnabled(StringUtils.isNotBlank(userSettings.getEmail()));
        }
        updateSyncControlsState();
        revalidate();
        repaint();
    }

    private static String formatTariffHeader(String tariffValue) {
        return Messages.get(MessageCodes.UI_ACCOUNT_TARIFF) + ": " + tariffValue;
    }

    private void updateSyncControlsState() {
        boolean cloudSyncAllowed = userSettings.getOperationMode().isNetworkSync()
                && agentSyncClient.isSyncConfigured(userSettings);
        syncNowButton.setEnabled(cloudSyncAllowed);
        syncHintLabel.setText(
                cloudSyncAllowed
                        ? Messages.get(MessageCodes.UI_SETTINGS_SYNC_HINT)
                        : Messages.get(MessageCodes.UI_SETTINGS_SYNC_NOT_IMPLEMENTED)
        );
    }

    private String buildLinkedDetailsText() {
        StringBuilder linkedDetailsBuilder = new StringBuilder("<html><body style='width:260px'>");
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
        if (!userSettings.getOperationMode().isNetworkSync()) {
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

    private void onExportBackupClicked() {
        JFileChooser fileChooser = createBackupFileChooser(true);
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = ensureZipExtension(fileChooser.getSelectedFile());
        rememberBackupDirectory(selectedFile);
        try {
            localDataBackupService.exportToZip(selectedFile.toPath());
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_EXPORT_SUCCESS),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.warn("schema={} Failed to export backup: {}", "local", exception.getMessage());
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onImportBackupClicked() {
        int confirmationResult = UiDialogs.showConfirm(
                Messages.get(MessageCodes.UI_SETTINGS_BACKUP_IMPORT_CONFIRM),
                Messages.get(MessageCodes.UI_SETTINGS_DATA),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmationResult != JOptionPane.YES_OPTION) {
            return;
        }

        JFileChooser fileChooser = createBackupFileChooser(false);
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = fileChooser.getSelectedFile();
        rememberBackupDirectory(selectedFile);

        boolean wasTrackingEnabled = trackingEngine.isTrackingEnabled();
        try {
            if (wasTrackingEnabled) {
                trackingEngine.pauseTracking();
            }
            localDataBackupService.importFromZip(selectedFile.toPath());
            userSettingsStore.reloadInto(userSettings);
            activityStore.load();
            trackingEngine.getLocalAppRuntimeStore().load();
            ApplicationIconService.getInstance().load();
            UserLocaleContext.setLanguage(userSettings.getLanguage());
            WindowsLaunchAtLoginService.apply(userSettings.isLaunchAtLogin());
            localDataRestoredListener.run();
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_IMPORT_SUCCESS),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.warn("schema={} Failed to import backup: {}", "local", exception.getMessage());
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            if (wasTrackingEnabled && !trackingEngine.isTrackingEnabled()) {
                trackingEngine.startTracking();
            }
        }
    }

    private void onSyncNowClicked() {
        if (!userSettings.getOperationMode().isNetworkSync()
                || !agentSyncClient.isSyncConfigured(userSettings)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC_NOT_IMPLEMENTED),
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        try {
            agentSyncClient.synchronize(userSettings);
            settingsChangedListener.run();
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC),
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.warn("schema={} Manual sync failed: {}", "local", exception.getMessage());
            UiDialogs.showMessage(
                    exception.getMessage(),
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private JFileChooser createBackupFileChooser(boolean saveDialog) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP (*.zip)", "zip"));
        if (StringUtils.isNotBlank(userSettings.getLastBackupDirectoryPath())) {
            File lastBackupDirectory = new File(userSettings.getLastBackupDirectoryPath());
            if (lastBackupDirectory.isDirectory()) {
                fileChooser.setCurrentDirectory(lastBackupDirectory);
            }
        }
        if (saveDialog) {
            String defaultFileName = "workpulsetracker-backup-"
                    + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + ".zip";
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), defaultFileName));
        }
        return fileChooser;
    }

    private void rememberBackupDirectory(File selectedFile) {
        if (Objects.isNull(selectedFile) || Objects.isNull(selectedFile.getParentFile())) {
            return;
        }
        userSettings.setLastBackupDirectoryPath(selectedFile.getParentFile().getAbsolutePath());
        userSettingsStore.save(userSettings);
    }

    private static File ensureZipExtension(File selectedFile) {
        if (Objects.isNull(selectedFile)) {
            return null;
        }
        String fileName = selectedFile.getName();
        if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
            return selectedFile;
        }
        return new File(selectedFile.getParentFile(), fileName + ".zip");
    }
}
