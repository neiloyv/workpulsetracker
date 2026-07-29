package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.api.AgentSyncClient;
import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.LocalDataBackupService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.tracking.TrackingEngine;
import com.workpulsetracker.common.i18n.AppLanguage;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Вкладка настроек: язык, трекинг, бэкап и заготовка sync.
 */
public final class SettingsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(SettingsPanel.class);

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final ActivityStore activityStore;
    private final TrackingEngine trackingEngine;
    private final LocalDataBackupService localDataBackupService = new LocalDataBackupService();
    private final AgentSyncClient agentSyncClient = new AgentSyncClient();
    private final Consumer<AppLanguage> languageChangeListener;
    private final Consumer<Boolean> autoStartChangeListener;
    private final Runnable settingsChangedListener;
    private final Runnable localDataRestoredListener;

    private final JLabel settingsTitleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JLabel trackingTitleLabel = new JLabel();
    private final JLabel autoStartTitleLabel = new JLabel();
    private final JLabel autoStartHintLabel = new JLabel();
    private final JLabel minimizeToTrayTitleLabel = new JLabel();
    private final JLabel minimizeToTrayHintLabel = new JLabel();
    private final JLabel minorThresholdLabel = new JLabel();
    private final JLabel minorThresholdHintLabel = new JLabel();
    private final JLabel minorThresholdUnitLabel = new JLabel();
    private final JLabel timelineVisibleTitleLabel = new JLabel();
    private final JLabel timelineVisibleHintLabel = new JLabel();
    private final JLabel dataTitleLabel = new JLabel();
    private final JLabel backupHintLabel = new JLabel();
    private final JLabel syncTitleLabel = new JLabel();
    private final JLabel syncHintLabel = new JLabel();
    private final JComboBox<LanguageItem> languageComboBox = new JComboBox<>();
    private final JCheckBox autoStartToggle = new JCheckBox();
    private final JCheckBox minimizeToTrayToggle = new JCheckBox();
    private final JCheckBox timelineVisibleToggle = new JCheckBox();
    private final JSpinner minorThresholdSpinner = new JSpinner(
            new SpinnerNumberModel(ApplicationUsageFilter.DEFAULT_MINOR_USAGE_THRESHOLD_MINUTES, 0, 24 * 60, 1)
    );
    private final JButton exportBackupButton = new JButton();
    private final JButton importBackupButton = new JButton();
    private final JButton syncNowButton = new JButton();
    private final JPanel settingsCard = new JPanel();
    private boolean suppressChangeEvents;

    public SettingsPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            ActivityStore activityStore,
            TrackingEngine trackingEngine,
            Consumer<AppLanguage> languageChangeListener,
            Consumer<Boolean> autoStartChangeListener,
            Runnable settingsChangedListener,
            Runnable localDataRestoredListener
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.activityStore = activityStore;
        this.trackingEngine = trackingEngine;
        this.languageChangeListener = languageChangeListener;
        this.autoStartChangeListener = autoStartChangeListener;
        this.settingsChangedListener = settingsChangedListener;
        this.localDataRestoredListener = localDataRestoredListener;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        reloadFromSettings();
    }

    private void buildContent() {
        settingsTitleLabel.setFont(settingsTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(settingsCard);

        languageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(languageLabel);
        languageComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        languageComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        languageComboBox.addItem(new LanguageItem(AppLanguage.ENGLISH, MessageCodes.UI_SETTINGS_LANGUAGE_EN));
        languageComboBox.addItem(new LanguageItem(AppLanguage.UKRAINIAN, MessageCodes.UI_SETTINGS_LANGUAGE_UK));
        languageComboBox.addActionListener(actionEvent -> onLanguageChanged());

        trackingTitleLabel.setFont(trackingTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        trackingTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trackingTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        autoStartTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleToggleSwitch(autoStartToggle);
        autoStartToggle.addActionListener(actionEvent -> onAutoStartChanged());
        JPanel autoStartRow = createSettingRow(
                createStackedTextPanel(autoStartTitleLabel, autoStartHintLabel),
                autoStartToggle
        );

        minimizeToTrayTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(minimizeToTrayHintLabel);
        UiTheme.styleToggleSwitch(minimizeToTrayToggle);
        minimizeToTrayToggle.addActionListener(actionEvent -> onMinimizeToTrayChanged());
        JPanel minimizeToTrayRow = createSettingRow(
                createStackedTextPanel(minimizeToTrayTitleLabel, minimizeToTrayHintLabel),
                minimizeToTrayToggle
        );

        minorThresholdLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
        UiTheme.styleMutedLabel(minorThresholdUnitLabel);
        minorThresholdSpinner.setMaximumSize(new Dimension(96, 32));
        minorThresholdSpinner.setPreferredSize(new Dimension(96, 32));
        minorThresholdSpinner.addChangeListener(changeEvent -> onMinorThresholdChanged());
        JPanel minorThresholdControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        minorThresholdControlPanel.setOpaque(false);
        minorThresholdControlPanel.add(minorThresholdSpinner);
        minorThresholdControlPanel.add(minorThresholdUnitLabel);
        JPanel minorThresholdRow = createSettingRow(
                createStackedTextPanel(minorThresholdLabel, minorThresholdHintLabel),
                minorThresholdControlPanel
        );

        timelineVisibleTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(timelineVisibleHintLabel);
        UiTheme.styleToggleSwitch(timelineVisibleToggle);
        timelineVisibleToggle.addActionListener(actionEvent -> onTimelineVisibleChanged());
        JPanel timelineVisibleRow = createSettingRow(
                createStackedTextPanel(timelineVisibleTitleLabel, timelineVisibleHintLabel),
                timelineVisibleToggle
        );

        dataTitleLabel.setFont(dataTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        dataTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dataTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(backupHintLabel);
        backupHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        UiTheme.styleSecondaryButton(exportBackupButton);
        UiTheme.styleSecondaryButton(importBackupButton);
        exportBackupButton.addActionListener(actionEvent -> onExportBackupClicked());
        importBackupButton.addActionListener(actionEvent -> onImportBackupClicked());
        JPanel backupButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        backupButtonsPanel.setOpaque(false);
        backupButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        backupButtonsPanel.add(exportBackupButton);
        backupButtonsPanel.add(importBackupButton);

        syncTitleLabel.setFont(syncTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        syncTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        syncTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(syncHintLabel);
        syncHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleSecondaryButton(syncNowButton);
        syncNowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        syncNowButton.addActionListener(actionEvent -> onSyncNowClicked());

        settingsCard.add(languageLabel);
        settingsCard.add(Box.createVerticalStrut(6));
        settingsCard.add(languageComboBox);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(trackingTitleLabel);
        settingsCard.add(Box.createVerticalStrut(14));
        settingsCard.add(autoStartRow);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(minimizeToTrayRow);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(minorThresholdRow);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(timelineVisibleRow);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(dataTitleLabel);
        settingsCard.add(Box.createVerticalStrut(8));
        settingsCard.add(backupHintLabel);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(backupButtonsPanel);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(syncTitleLabel);
        settingsCard.add(Box.createVerticalStrut(8));
        settingsCard.add(syncHintLabel);
        settingsCard.add(Box.createVerticalStrut(10));
        settingsCard.add(syncNowButton);

        add(settingsTitleLabel, BorderLayout.NORTH);
        add(settingsCard, BorderLayout.CENTER);
        retranslate();
    }

    private JPanel createStackedTextPanel(JLabel titleLabel, JLabel hintLabel) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setVerticalAlignment(SwingConstants.TOP);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(hintLabel);
        return textPanel;
    }

    private JPanel createSettingRow(JComponent leftComponent, JComponent rightComponent) {
        JPanel rowPanel = new JPanel(new BorderLayout(16, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        rowPanel.add(leftComponent, BorderLayout.CENTER);
        JPanel rightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrapper.setOpaque(false);
        rightWrapper.add(rightComponent);
        rowPanel.add(rightWrapper, BorderLayout.EAST);
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
        settingsTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TITLE));
        languageLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_LANGUAGE));
        trackingTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TRACKING));
        autoStartTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_AUTO_START));
        autoStartHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_AUTO_START_HINT)
                        + "</body></html>"
        );
        minimizeToTrayTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINIMIZE_TO_TRAY));
        minimizeToTrayHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_MINIMIZE_TO_TRAY_HINT)
                        + "</body></html>"
        );
        minorThresholdLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD));
        minorThresholdUnitLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_UNIT));
        minorThresholdHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_HINT)
                        + "</body></html>"
        );
        timelineVisibleTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TIMELINE_VISIBLE));
        timelineVisibleHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_TIMELINE_VISIBLE_HINT)
                        + "</body></html>"
        );
        dataTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_DATA));
        backupHintLabel.setText(
                "<html><body style='width:520px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_BACKUP_HINT)
                        + "</body></html>"
        );
        exportBackupButton.setText(Messages.get(MessageCodes.UI_SETTINGS_BACKUP_EXPORT));
        importBackupButton.setText(Messages.get(MessageCodes.UI_SETTINGS_BACKUP_IMPORT));
        syncTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_SYNC));
        syncHintLabel.setText(
                "<html><body style='width:520px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_SYNC_HINT)
                        + "</body></html>"
        );
        syncNowButton.setText(Messages.get(MessageCodes.UI_SETTINGS_SYNC_NOW));
        languageComboBox.repaint();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(settingsCard);
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        trackingTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        minimizeToTrayTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        minorThresholdLabel.setForeground(UiTheme.TEXT_PRIMARY);
        timelineVisibleTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        dataTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        syncTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(languageLabel);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleMutedLabel(minimizeToTrayHintLabel);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
        UiTheme.styleMutedLabel(minorThresholdUnitLabel);
        UiTheme.styleMutedLabel(timelineVisibleHintLabel);
        UiTheme.styleMutedLabel(backupHintLabel);
        UiTheme.styleMutedLabel(syncHintLabel);
        UiTheme.styleToggleSwitch(autoStartToggle);
        UiTheme.styleToggleSwitch(minimizeToTrayToggle);
        UiTheme.styleToggleSwitch(timelineVisibleToggle);
        UiTheme.styleSecondaryButton(exportBackupButton);
        UiTheme.styleSecondaryButton(importBackupButton);
        UiTheme.styleSecondaryButton(syncNowButton);
    }

    public void reloadFromSettings() {
        suppressChangeEvents = true;
        try {
            selectLanguage(userSettings.getLanguage());
            autoStartToggle.setSelected(userSettings.isAutoStartTracking());
            minimizeToTrayToggle.setSelected(userSettings.isMinimizeToTray());
            timelineVisibleToggle.setSelected(userSettings.isTimelineVisible());
            minorThresholdSpinner.setValue(userSettings.getMinorUsageThresholdMinutes());
        } finally {
            suppressChangeEvents = false;
        }
    }

    private void selectLanguage(AppLanguage appLanguage) {
        for (int itemIndex = 0; itemIndex < languageComboBox.getItemCount(); itemIndex++) {
            LanguageItem languageItem = languageComboBox.getItemAt(itemIndex);
            if (Objects.equals(languageItem.appLanguage(), appLanguage)) {
                languageComboBox.setSelectedIndex(itemIndex);
                return;
            }
        }
    }

    private void onLanguageChanged() {
        if (suppressChangeEvents) {
            return;
        }
        LanguageItem languageItem = (LanguageItem) languageComboBox.getSelectedItem();
        if (Objects.isNull(languageItem)) {
            return;
        }
        userSettings.setLanguageCode(languageItem.appLanguage().getCode());
        UserLocaleContext.setLanguage(languageItem.appLanguage());
        userSettingsStore.save(userSettings);
        languageChangeListener.accept(languageItem.appLanguage());
    }

    private void onAutoStartChanged() {
        if (suppressChangeEvents) {
            return;
        }
        boolean autoStartTracking = autoStartToggle.isSelected();
        userSettings.setAutoStartTracking(autoStartTracking);
        userSettingsStore.save(userSettings);
        autoStartChangeListener.accept(autoStartTracking);
    }

    private void onMinimizeToTrayChanged() {
        if (suppressChangeEvents) {
            return;
        }
        userSettings.setMinimizeToTray(minimizeToTrayToggle.isSelected());
        userSettingsStore.save(userSettings);
    }

    private void onMinorThresholdChanged() {
        if (suppressChangeEvents) {
            return;
        }
        int minorUsageThresholdMinutes = ((Number) minorThresholdSpinner.getValue()).intValue();
        userSettings.setMinorUsageThresholdMinutes(minorUsageThresholdMinutes);
        userSettingsStore.save(userSettings);
        settingsChangedListener.run();
    }

    private void onTimelineVisibleChanged() {
        if (suppressChangeEvents) {
            return;
        }
        userSettings.setTimelineVisible(timelineVisibleToggle.isSelected());
        userSettingsStore.save(userSettings);
        settingsChangedListener.run();
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
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_EXPORT_SUCCESS),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.warn("Failed to export backup: {}", exception.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onImportBackupClicked() {
        int confirmationResult = JOptionPane.showConfirmDialog(
                this,
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
            ApplicationIconService.getInstance().load();
            UserLocaleContext.setLanguage(userSettings.getLanguage());
            localDataRestoredListener.run();
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_SETTINGS_BACKUP_IMPORT_SUCCESS),
                    Messages.get(MessageCodes.UI_SETTINGS_DATA),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception exception) {
            logger.warn("Failed to import backup: {}", exception.getMessage());
            JOptionPane.showMessageDialog(
                    this,
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
        // TODO: вызвать AgentSyncClient.synchronize после реализации API sync на сервере
        try {
            agentSyncClient.synchronize(userSettings, activityStore.getAllIntervals());
        } catch (UnsupportedOperationException unsupportedOperationException) {
            JOptionPane.showMessageDialog(
                    this,
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC_NOT_IMPLEMENTED),
                    Messages.get(MessageCodes.UI_SETTINGS_SYNC),
                    JOptionPane.INFORMATION_MESSAGE
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

    private record LanguageItem(AppLanguage appLanguage, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
