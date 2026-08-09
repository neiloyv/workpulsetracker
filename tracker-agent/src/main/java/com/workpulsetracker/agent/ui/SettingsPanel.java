package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.ApplicationUsageFilter;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.util.WindowsLaunchAtLoginService;
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
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Вкладка настроек: язык, запуск и параметры трекинга (две колонки).
 */
public final class SettingsPanel extends JPanel {

    private static final int HINT_HTML_WIDTH = 260;
    private static final int SETTING_ROW_MAX_HEIGHT = 88;

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final Consumer<AppLanguage> languageChangeListener;
    private final Consumer<Boolean> autoStartChangeListener;
    private final Runnable settingsChangedListener;

    private final JLabel settingsTitleLabel = new JLabel();
    private final JLabel generalTitleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JLabel trackingTitleLabel = new JLabel();
    private final JLabel launchAtLoginTitleLabel = new JLabel();
    private final JLabel launchAtLoginHintLabel = new JLabel();
    private final JLabel autoStartTitleLabel = new JLabel();
    private final JLabel autoStartHintLabel = new JLabel();
    private final JLabel minimizeToTrayTitleLabel = new JLabel();
    private final JLabel minimizeToTrayHintLabel = new JLabel();
    private final JLabel minorThresholdLabel = new JLabel();
    private final JLabel minorThresholdHintLabel = new JLabel();
    private final JLabel timelineVisibleTitleLabel = new JLabel();
    private final JLabel timelineVisibleHintLabel = new JLabel();
    private final JLabel showExceptionsOnTimelineTitleLabel = new JLabel();
    private final JLabel showExceptionsOnTimelineHintLabel = new JLabel();
    private final JComboBox<LanguageItem> languageComboBox = new JComboBox<>();
    private final JCheckBox launchAtLoginToggle = new JCheckBox();
    private final JCheckBox autoStartToggle = new JCheckBox();
    private final JCheckBox minimizeToTrayToggle = new JCheckBox();
    private final JCheckBox timelineVisibleToggle = new JCheckBox();
    private final JCheckBox showExceptionsOnTimelineToggle = new JCheckBox();
    private final JSpinner minorThresholdSpinner = new JSpinner(
            new SpinnerNumberModel(ApplicationUsageFilter.DEFAULT_MINOR_USAGE_THRESHOLD_MINUTES, 0, 24 * 60, 1)
    );
    private final JPanel generalCard = new JPanel();
    private final JPanel trackingCard = new JPanel();
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
        reloadFromSettings();
    }

    private void buildContent() {
        settingsTitleLabel.setFont(settingsTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        generalCard.setLayout(new BoxLayout(generalCard, BoxLayout.Y_AXIS));
        trackingCard.setLayout(new BoxLayout(trackingCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(generalCard);
        UiTheme.styleSurfaceCard(trackingCard);

        generalTitleLabel.setFont(generalTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        generalTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        generalTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        trackingTitleLabel.setFont(trackingTitleLabel.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        trackingTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trackingTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        languageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UiTheme.styleMutedLabel(languageLabel);
        languageComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        languageComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        languageComboBox.addItem(new LanguageItem(AppLanguage.ENGLISH, MessageCodes.UI_SETTINGS_LANGUAGE_EN));
        languageComboBox.addItem(new LanguageItem(AppLanguage.UKRAINIAN, MessageCodes.UI_SETTINGS_LANGUAGE_UK));
        languageComboBox.addActionListener(actionEvent -> onLanguageChanged());

        launchAtLoginTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(launchAtLoginHintLabel);
        UiTheme.styleToggleSwitch(launchAtLoginToggle);
        launchAtLoginToggle.setEnabled(WindowsLaunchAtLoginService.isSupported());
        launchAtLoginToggle.addActionListener(actionEvent -> onLaunchAtLoginChanged());

        minimizeToTrayTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(minimizeToTrayHintLabel);
        UiTheme.styleToggleSwitch(minimizeToTrayToggle);
        minimizeToTrayToggle.addActionListener(actionEvent -> onMinimizeToTrayChanged());

        autoStartTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleToggleSwitch(autoStartToggle);
        autoStartToggle.addActionListener(actionEvent -> onAutoStartChanged());

        minorThresholdLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
        minorThresholdSpinner.setMaximumSize(new Dimension(96, 32));
        minorThresholdSpinner.setPreferredSize(new Dimension(96, 32));
        minorThresholdSpinner.addChangeListener(changeEvent -> onMinorThresholdChanged());
        JPanel minorThresholdControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        minorThresholdControlPanel.setOpaque(false);
        minorThresholdControlPanel.add(minorThresholdSpinner);

        timelineVisibleTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(timelineVisibleHintLabel);
        UiTheme.styleToggleSwitch(timelineVisibleToggle);
        timelineVisibleToggle.addActionListener(actionEvent -> onTimelineVisibleChanged());

        showExceptionsOnTimelineTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(showExceptionsOnTimelineHintLabel);
        UiTheme.styleToggleSwitch(showExceptionsOnTimelineToggle);
        showExceptionsOnTimelineToggle.addActionListener(actionEvent -> onShowExceptionsOnTimelineChanged());

        generalCard.add(generalTitleLabel);
        generalCard.add(Box.createVerticalStrut(14));
        generalCard.add(languageLabel);
        generalCard.add(Box.createVerticalStrut(6));
        generalCard.add(languageComboBox);
        generalCard.add(Box.createVerticalStrut(16));
        generalCard.add(createDivider());
        generalCard.add(Box.createVerticalStrut(16));
        generalCard.add(createSettingRow(
                createStackedTextPanel(launchAtLoginTitleLabel, launchAtLoginHintLabel),
                launchAtLoginToggle
        ));
        generalCard.add(Box.createVerticalStrut(14));
        generalCard.add(createSettingRow(
                createStackedTextPanel(minimizeToTrayTitleLabel, minimizeToTrayHintLabel),
                minimizeToTrayToggle
        ));
        generalCard.add(Box.createVerticalGlue());

        trackingCard.add(trackingTitleLabel);
        trackingCard.add(Box.createVerticalStrut(14));
        trackingCard.add(createSettingRow(
                createStackedTextPanel(autoStartTitleLabel, autoStartHintLabel),
                autoStartToggle
        ));
        trackingCard.add(Box.createVerticalStrut(14));
        trackingCard.add(createSettingRow(
                createStackedTextPanel(minorThresholdLabel, minorThresholdHintLabel),
                minorThresholdControlPanel
        ));
        trackingCard.add(Box.createVerticalStrut(14));
        trackingCard.add(createSettingRow(
                createStackedTextPanel(timelineVisibleTitleLabel, timelineVisibleHintLabel),
                timelineVisibleToggle
        ));
        trackingCard.add(Box.createVerticalStrut(14));
        trackingCard.add(createSettingRow(
                createStackedTextPanel(showExceptionsOnTimelineTitleLabel, showExceptionsOnTimelineHintLabel),
                showExceptionsOnTimelineToggle
        ));
        trackingCard.add(Box.createVerticalGlue());

        JPanel columnsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        columnsPanel.setOpaque(false);
        columnsPanel.add(generalCard);
        columnsPanel.add(trackingCard);

        add(settingsTitleLabel, BorderLayout.NORTH);
        add(columnsPanel, BorderLayout.CENTER);
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
        JPanel rowPanel = new JPanel(new BorderLayout(12, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, SETTING_ROW_MAX_HEIGHT));
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

    private static String wrapHintHtml(String hintText) {
        return "<html><body style='width:" + HINT_HTML_WIDTH + "px'>" + hintText + "</body></html>";
    }

    public void retranslate() {
        settingsTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TITLE));
        generalTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_GENERAL));
        languageLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_LANGUAGE));
        trackingTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TRACKING));
        launchAtLoginTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_LAUNCH_AT_LOGIN));
        launchAtLoginHintLabel.setText(wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_LAUNCH_AT_LOGIN_HINT)));
        autoStartTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_AUTO_START));
        autoStartHintLabel.setText(wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_AUTO_START_HINT)));
        minimizeToTrayTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_MINIMIZE_TO_TRAY));
        minimizeToTrayHintLabel.setText(wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_MINIMIZE_TO_TRAY_HINT)));
        minorThresholdLabel.setText(
                Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD)
                        + ", "
                        + Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_UNIT)
        );
        minorThresholdHintLabel.setText(wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_MINOR_THRESHOLD_HINT)));
        timelineVisibleTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_TIMELINE_VISIBLE));
        timelineVisibleHintLabel.setText(wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_TIMELINE_VISIBLE_HINT)));
        showExceptionsOnTimelineTitleLabel.setText(Messages.get(MessageCodes.UI_SETTINGS_SHOW_EXCEPTIONS_ON_TIMELINE));
        showExceptionsOnTimelineHintLabel.setText(
                wrapHintHtml(Messages.get(MessageCodes.UI_SETTINGS_SHOW_EXCEPTIONS_ON_TIMELINE_HINT))
        );
        languageComboBox.repaint();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(generalCard);
        UiTheme.styleSurfaceCard(trackingCard);
        settingsTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        generalTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        trackingTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        launchAtLoginTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        minimizeToTrayTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        minorThresholdLabel.setForeground(UiTheme.TEXT_PRIMARY);
        timelineVisibleTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        showExceptionsOnTimelineTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(languageLabel);
        UiTheme.styleMutedLabel(launchAtLoginHintLabel);
        UiTheme.styleMutedLabel(autoStartHintLabel);
        UiTheme.styleMutedLabel(minimizeToTrayHintLabel);
        UiTheme.styleMutedLabel(minorThresholdHintLabel);
        UiTheme.styleMutedLabel(timelineVisibleHintLabel);
        UiTheme.styleMutedLabel(showExceptionsOnTimelineHintLabel);
        UiTheme.styleToggleSwitch(launchAtLoginToggle);
        UiTheme.styleToggleSwitch(autoStartToggle);
        UiTheme.styleToggleSwitch(minimizeToTrayToggle);
        UiTheme.styleToggleSwitch(timelineVisibleToggle);
        UiTheme.styleToggleSwitch(showExceptionsOnTimelineToggle);
    }

    public void reloadFromSettings() {
        suppressChangeEvents = true;
        try {
            selectLanguage(userSettings.getLanguage());
            launchAtLoginToggle.setSelected(userSettings.isLaunchAtLogin());
            autoStartToggle.setSelected(userSettings.isAutoStartTracking());
            minimizeToTrayToggle.setSelected(userSettings.isMinimizeToTray());
            timelineVisibleToggle.setSelected(userSettings.isTimelineVisible());
            showExceptionsOnTimelineToggle.setSelected(userSettings.isShowExceptionsOnTimeline());
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

    private void onLaunchAtLoginChanged() {
        if (suppressChangeEvents) {
            return;
        }
        boolean launchAtLoginEnabled = launchAtLoginToggle.isSelected();
        userSettings.setLaunchAtLogin(launchAtLoginEnabled);
        userSettingsStore.save(userSettings);
        WindowsLaunchAtLoginService.apply(launchAtLoginEnabled);
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

    private void onShowExceptionsOnTimelineChanged() {
        if (suppressChangeEvents) {
            return;
        }
        userSettings.setShowExceptionsOnTimeline(showExceptionsOnTimelineToggle.isSelected());
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
