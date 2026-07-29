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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Вкладка настроек: язык и параметры трекинга.
 */
public final class SettingsPanel extends JPanel {

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final Consumer<AppLanguage> languageChangeListener;
    private final Consumer<Boolean> autoStartChangeListener;
    private final Runnable settingsChangedListener;

    private final JLabel settingsTitleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JLabel trackingTitleLabel = new JLabel();
    private final JLabel autoStartTitleLabel = new JLabel();
    private final JLabel autoStartHintLabel = new JLabel();
    private final JLabel minorThresholdLabel = new JLabel();
    private final JLabel minorThresholdHintLabel = new JLabel();
    private final JLabel minorThresholdUnitLabel = new JLabel();
    private final JComboBox<LanguageItem> languageComboBox = new JComboBox<>();
    private final JCheckBox autoStartToggle = new JCheckBox();
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
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        syncControlsFromSettings();
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
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(16));
        settingsCard.add(minorThresholdRow);

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
        minorThresholdLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD));
        minorThresholdUnitLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_UNIT));
        minorThresholdHintLabel.setText(
                "<html><body style='width:420px'>"
                        + Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_HINT)
                        + "</body></html>"
        );
        languageComboBox.repaint();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(settingsCard);
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        trackingTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        minorThresholdLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(languageLabel);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
        UiTheme.styleMutedLabel(minorThresholdUnitLabel);
        UiTheme.styleToggleSwitch(autoStartToggle);
    }

    private void syncControlsFromSettings() {
        suppressChangeEvents = true;
        selectLanguage(userSettings.getLanguage());
        autoStartToggle.setSelected(userSettings.isAutoStartTracking());
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
        boolean autoStartTracking = autoStartToggle.isSelected();
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
