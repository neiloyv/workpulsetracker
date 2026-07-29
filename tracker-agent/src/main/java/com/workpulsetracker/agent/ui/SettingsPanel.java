package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.common.i18n.AppLanguage;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Вкладка настроек: язык, автостарт и порог «Others».
 */
public final class SettingsPanel extends JPanel {

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final Consumer<AppLanguage> languageChangeListener;
    private final Consumer<Boolean> autoStartChangeListener;
    private final Runnable settingsChangedListener;

    private final JLabel settingsTitleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JLabel autoStartHintLabel = new JLabel();
    private final JLabel minorThresholdLabel = new JLabel();
    private final JLabel minorThresholdHintLabel = new JLabel();
    private final JLabel minorThresholdUnitLabel = new JLabel();
    private final JComboBox<LanguageItem> languageComboBox = new JComboBox<>();
    private final JCheckBox autoStartCheckBox = new JCheckBox();
    private final JSpinner minorThresholdSpinner = new JSpinner(
            new SpinnerNumberModel(ApplicationUsageFilter.DEFAULT_MINOR_USAGE_THRESHOLD_MINUTES, 0, 24 * 60, 1)
    );
    private final JPanel settingsCard = new JPanel();
    private boolean suppressChangeEvents;

    public SettingsPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            Consumer<AppLanguage> languageChangeListener,
            Consumer<Boolean> autoStartChangeListener,
            Runnable settingsChangedListener
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.languageChangeListener = languageChangeListener;
        this.autoStartChangeListener = autoStartChangeListener;
        this.settingsChangedListener = settingsChangedListener;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        syncControlsFromSettings();
    }

    private void buildContent() {
        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(settingsCard);

        settingsTitleLabel.setFont(settingsTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        settingsTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        languageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(languageLabel);
        languageComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        languageComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        languageComboBox.addItem(new LanguageItem(AppLanguage.ENGLISH, MessageCodes.UI_SETTINGS_LANGUAGE_EN));
        languageComboBox.addItem(new LanguageItem(AppLanguage.UKRAINIAN, MessageCodes.UI_SETTINGS_LANGUAGE_UK));
        languageComboBox.addActionListener(actionEvent -> onLanguageChanged());

        autoStartCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoStartCheckBox.setOpaque(false);
        autoStartCheckBox.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartCheckBox.addActionListener(actionEvent -> onAutoStartChanged());

        autoStartHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(autoStartHintLabel);

        minorThresholdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(minorThresholdLabel);
        UiTheme.styleMutedLabel(minorThresholdUnitLabel);

        minorThresholdSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        minorThresholdSpinner.setMaximumSize(new Dimension(120, 32));
        minorThresholdSpinner.addChangeListener(changeEvent -> onMinorThresholdChanged());

        JPanel minorThresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        minorThresholdRow.setOpaque(false);
        minorThresholdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        minorThresholdRow.add(minorThresholdSpinner);
        minorThresholdRow.add(minorThresholdUnitLabel);

        minorThresholdHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);

        settingsCard.add(settingsTitleLabel);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(languageLabel);
        settingsCard.add(Box.createVerticalStrut(6));
        settingsCard.add(languageComboBox);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(autoStartCheckBox);
        settingsCard.add(Box.createVerticalStrut(6));
        settingsCard.add(autoStartHintLabel);
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(minorThresholdLabel);
        settingsCard.add(Box.createVerticalStrut(6));
        settingsCard.add(minorThresholdRow);
        settingsCard.add(Box.createVerticalStrut(6));
        settingsCard.add(minorThresholdHintLabel);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(settingsCard, BorderLayout.NORTH);
        add(wrapperPanel, BorderLayout.CENTER);
        retranslate();
    }

    public void retranslate() {
        settingsTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TITLE));
        languageLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_LANGUAGE));
        autoStartCheckBox.setText(Messages.get(MessageCodes.UI_SETTINGS_AUTO_START));
        autoStartHintLabel.setText(
                "<html><body style='width:360px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_AUTO_START_HINT)
                        + "</body></html>"
        );
        minorThresholdLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD));
        minorThresholdUnitLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_UNIT));
        minorThresholdHintLabel.setText(
                "<html><body style='width:360px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_HINT)
                        + "</body></html>"
        );
        languageComboBox.repaint();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(settingsCard);
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartCheckBox.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(languageLabel);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleMutedLabel(minorThresholdLabel);
        UiTheme.styleMutedLabel(minorThresholdUnitLabel);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
    }

    private void syncControlsFromSettings() {
        suppressChangeEvents = true;
        selectLanguage(userSettings.getLanguage());
        autoStartCheckBox.setSelected(userSettings.isAutoStartTracking());
        minorThresholdSpinner.setValue(userSettings.getMinorUsageThresholdMinutes());
        suppressChangeEvents = false;
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
        boolean autoStartTracking = autoStartCheckBox.isSelected();
        userSettings.setAutoStartTracking(autoStartTracking);
        userSettingsStore.save(userSettings);
        autoStartChangeListener.accept(autoStartTracking);
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

    private record LanguageItem(AppLanguage appLanguage, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
