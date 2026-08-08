package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.pomodoro.PomodoroEngine;
import com.workpulsetracker.agent.pomodoro.PomodoroPhase;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Вкладка Pomodoro: интервалы, таймер, уведомления о смене фазы.
 */
public final class PomodoroPanel extends JPanel {

    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final PomodoroEngine pomodoroEngine = new PomodoroEngine();
    private final BiConsumer<String, String> notificationPublisher;

    private final JLabel titleLabel = new JLabel();
    private final JLabel enableLabel = new JLabel();
    private final JLabel enableHintLabel = new JLabel();
    private final JCheckBox enableToggle = new JCheckBox();
    private final JLabel workMinutesLabel = new JLabel();
    private final JLabel shortBreakLabel = new JLabel();
    private final JLabel longBreakLabel = new JLabel();
    private final JLabel sessionsUntilLongBreakLabel = new JLabel();
    private final JSpinner workMinutesSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 180, 1));
    private final JSpinner shortBreakSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 180, 1));
    private final JSpinner longBreakSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 180, 1));
    private final JSpinner sessionsUntilLongBreakSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 12, 1));
    private final JLabel phaseLabel = new JLabel();
    private final JLabel timerLabel = new JLabel("25:00");
    private final JLabel completedTodayLabel = new JLabel();
    private final JButton startPauseButton = new JButton();
    private final JButton skipButton = new JButton();
    private final JPanel settingsCard = new JPanel();
    private final JPanel timerCard = new JPanel();
    private final Timer tickTimer;
    private boolean suppressChangeEvents;
    private boolean phaseAlertVisible;

    public PomodoroPanel(
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            BiConsumer<String, String> notificationPublisher
    ) {
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        tickTimer = new Timer(1000, actionEvent -> pomodoroEngine.onTick());
        tickTimer.start();
        buildContent();
        wireEngine();
        syncControlsFromSettings();
        refresh();
    }

    private void buildContent() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(settingsCard);

        enableLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(enableHintLabel);
        UiTheme.styleToggleSwitch(enableToggle);
        enableToggle.addActionListener(actionEvent -> onEnableChanged());

        JPanel enableRow = new JPanel(new BorderLayout(12, 0));
        enableRow.setOpaque(false);
        enableRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        enableRow.add(createStackedTextPanel(enableLabel, enableHintLabel), BorderLayout.CENTER);
        JPanel enableToggleWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        enableToggleWrapper.setOpaque(false);
        enableToggleWrapper.add(enableToggle);
        enableRow.add(enableToggleWrapper, BorderLayout.EAST);

        workMinutesLabel.setForeground(UiTheme.TEXT_PRIMARY);
        shortBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        longBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        sessionsUntilLongBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);

        workMinutesSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        shortBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        longBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        sessionsUntilLongBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());

        JPanel durationsStackPanel = new JPanel();
        durationsStackPanel.setOpaque(false);
        durationsStackPanel.setLayout(new BoxLayout(durationsStackPanel, BoxLayout.Y_AXIS));
        durationsStackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        durationsStackPanel.add(createCompactDurationCell(workMinutesLabel, workMinutesSpinner));
        durationsStackPanel.add(Box.createVerticalStrut(10));
        durationsStackPanel.add(createCompactDurationCell(shortBreakLabel, shortBreakSpinner));
        durationsStackPanel.add(Box.createVerticalStrut(10));
        durationsStackPanel.add(createCompactDurationCell(longBreakLabel, longBreakSpinner));
        durationsStackPanel.add(Box.createVerticalStrut(10));
        durationsStackPanel.add(createCompactDurationCell(sessionsUntilLongBreakLabel, sessionsUntilLongBreakSpinner));

        settingsCard.add(enableRow);
        settingsCard.add(Box.createVerticalStrut(14));
        settingsCard.add(createDivider());
        settingsCard.add(Box.createVerticalStrut(14));
        settingsCard.add(durationsStackPanel);
        settingsCard.add(Box.createVerticalGlue());

        phaseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        phaseLabel.setHorizontalAlignment(SwingConstants.CENTER);
        UiTheme.styleMutedLabel(phaseLabel);

        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel.setForeground(UiTheme.TEXT_PRIMARY);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 60));

        completedTodayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        completedTodayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        UiTheme.styleMutedLabel(completedTodayLabel);

        startPauseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        skipButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startPauseButton.addActionListener(actionEvent -> onStartPauseClicked());
        skipButton.addActionListener(actionEvent -> pomodoroEngine.skip());
        UiTheme.stylePrimaryButton(startPauseButton);
        UiTheme.styleSecondaryButton(skipButton);
        constrainButtonWidth(startPauseButton, 280, 44);
        constrainButtonWidth(skipButton, 280, 44);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(startPauseButton);
        buttonsPanel.add(Box.createVerticalStrut(12));
        buttonsPanel.add(skipButton);

        JPanel timerContentPanel = new JPanel();
        timerContentPanel.setOpaque(false);
        timerContentPanel.setLayout(new BoxLayout(timerContentPanel, BoxLayout.Y_AXIS));
        timerContentPanel.add(phaseLabel);
        timerContentPanel.add(Box.createVerticalStrut(8));
        timerContentPanel.add(timerLabel);
        timerContentPanel.add(Box.createVerticalStrut(8));
        timerContentPanel.add(completedTodayLabel);
        timerContentPanel.add(Box.createVerticalStrut(18));
        timerContentPanel.add(buttonsPanel);

        timerCard.setLayout(new GridBagLayout());
        UiTheme.styleSurfaceCard(timerCard);
        GridBagConstraints timerContentConstraints = new GridBagConstraints();
        timerContentConstraints.gridx = 0;
        timerContentConstraints.gridy = 0;
        timerContentConstraints.weightx = 1;
        timerContentConstraints.weighty = 1;
        timerContentConstraints.anchor = GridBagConstraints.NORTH;
        timerContentConstraints.insets = new java.awt.Insets(48, 0, 0, 0);
        timerCard.add(timerContentPanel, timerContentConstraints);

        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        // Предпочтительные размеры обнуляем, чтобы weightx задал ровно 1/3 и 2/3.
        settingsCard.setPreferredSize(new Dimension(0, 0));
        timerCard.setPreferredSize(new Dimension(0, 0));

        JPanel contentSplitPanel = new JPanel(new GridBagLayout());
        contentSplitPanel.setOpaque(false);

        GridBagConstraints settingsConstraints = new GridBagConstraints();
        settingsConstraints.gridx = 0;
        settingsConstraints.gridy = 0;
        settingsConstraints.weightx = 1;
        settingsConstraints.weighty = 1;
        settingsConstraints.fill = GridBagConstraints.BOTH;
        settingsConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        contentSplitPanel.add(settingsCard, settingsConstraints);

        GridBagConstraints timerConstraints = new GridBagConstraints();
        timerConstraints.gridx = 1;
        timerConstraints.gridy = 0;
        timerConstraints.weightx = 2;
        timerConstraints.weighty = 1;
        timerConstraints.fill = GridBagConstraints.BOTH;
        timerConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        contentSplitPanel.add(timerCard, timerConstraints);

        add(titlePanel, BorderLayout.NORTH);
        add(contentSplitPanel, BorderLayout.CENTER);
        retranslate();
    }

    private static void constrainButtonWidth(JButton button, int preferredWidth, int preferredHeight) {
        Dimension preferredSize = button.getPreferredSize();
        int buttonWidth = Math.max(preferredWidth, preferredSize.width);
        int buttonHeight = Math.max(preferredHeight, preferredSize.height);
        Dimension constrainedSize = new Dimension(buttonWidth, buttonHeight);
        button.setPreferredSize(constrainedSize);
        button.setMaximumSize(constrainedSize);
        button.setMinimumSize(new Dimension(buttonWidth, buttonHeight));
    }

    private void wireEngine() {
        pomodoroEngine.addStateChangeListener(() -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(this::refresh);
                return;
            }
            refresh();
        });
        pomodoroEngine.addPhaseCompletedListener(this::onPhaseCompleted);
    }

    private void syncControlsFromSettings() {
        suppressChangeEvents = true;
        try {
            enableToggle.setSelected(userSettings.isPomodoroEnabled());
            workMinutesSpinner.setValue(userSettings.getPomodoroWorkMinutes());
            shortBreakSpinner.setValue(userSettings.getPomodoroShortBreakMinutes());
            longBreakSpinner.setValue(userSettings.getPomodoroLongBreakMinutes());
            sessionsUntilLongBreakSpinner.setValue(userSettings.getPomodoroSessionsUntilLongBreak());
            pomodoroEngine.updateDurations(
                    userSettings.getPomodoroWorkMinutes(),
                    userSettings.getPomodoroShortBreakMinutes(),
                    userSettings.getPomodoroLongBreakMinutes(),
                    userSettings.getPomodoroSessionsUntilLongBreak()
            );
            pomodoroEngine.setFeatureEnabled(userSettings.isPomodoroEnabled());
        } finally {
            suppressChangeEvents = false;
        }
    }

    public void reloadFromSettings() {
        syncControlsFromSettings();
        refresh();
    }

    public void retranslate() {
        titleLabel.setText(Messages.get(MessageCodes.UI_POMODORO_TITLE));
        enableLabel.setText(Messages.get(MessageCodes.UI_POMODORO_ENABLE));
        enableHintLabel.setText("<html><body style='width:180px'>"
                + Messages.get(MessageCodes.UI_POMODORO_ENABLE_HINT)
                + "</body></html>");
        workMinutesLabel.setText(formatDurationLabel(Messages.get(MessageCodes.UI_POMODORO_WORK)));
        shortBreakLabel.setText(formatDurationLabel(Messages.get(MessageCodes.UI_POMODORO_SHORT_BREAK)));
        longBreakLabel.setText(formatDurationLabel(Messages.get(MessageCodes.UI_POMODORO_LONG_BREAK)));
        sessionsUntilLongBreakLabel.setText(Messages.get(MessageCodes.UI_POMODORO_SESSIONS_UNTIL_LONG));
        skipButton.setText(Messages.get(MessageCodes.UI_POMODORO_SKIP));
        refresh();
    }

    private static String formatDurationLabel(String title) {
        return title + ", " + Messages.get(MessageCodes.UI_POMODORO_MINUTES);
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        UiTheme.styleSurfaceCard(settingsCard);
        UiTheme.styleSurfaceCard(timerCard);
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        enableLabel.setForeground(UiTheme.TEXT_PRIMARY);
        workMinutesLabel.setForeground(UiTheme.TEXT_PRIMARY);
        shortBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        longBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        sessionsUntilLongBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        timerLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(enableHintLabel);
        UiTheme.styleMutedLabel(phaseLabel);
        UiTheme.styleMutedLabel(completedTodayLabel);
        UiTheme.styleToggleSwitch(enableToggle);
        refresh();
    }

    public void refresh() {
        boolean featureEnabled = pomodoroEngine.isFeatureEnabled();
        boolean timerRunning = pomodoroEngine.isTimerRunning();

        workMinutesSpinner.setEnabled(featureEnabled && !timerRunning);
        shortBreakSpinner.setEnabled(featureEnabled && !timerRunning);
        longBreakSpinner.setEnabled(featureEnabled && !timerRunning);
        sessionsUntilLongBreakSpinner.setEnabled(featureEnabled && !timerRunning);
        startPauseButton.setEnabled(featureEnabled);
        skipButton.setEnabled(featureEnabled);

        timerLabel.setText(formatCountdown(pomodoroEngine.getRemainingSeconds()));
        phaseLabel.setText(resolvePhaseLabel(pomodoroEngine.getCurrentPhase()));
        completedTodayLabel.setText(
                Messages.get(
                        MessageCodes.UI_POMODORO_COMPLETED_TODAY,
                        pomodoroEngine.getCompletedPomodorosToday()
                )
        );

        if (!featureEnabled) {
            startPauseButton.setText(Messages.get(MessageCodes.UI_POMODORO_START));
            UiTheme.stylePrimaryButton(startPauseButton);
            constrainButtonWidth(startPauseButton, 280, 44);
            return;
        }
        if (timerRunning) {
            startPauseButton.setText(Messages.get(MessageCodes.UI_POMODORO_PAUSE));
            UiTheme.styleDangerButton(startPauseButton);
        } else {
            startPauseButton.setText(Messages.get(MessageCodes.UI_POMODORO_START));
            UiTheme.stylePrimaryButton(startPauseButton);
        }
        constrainButtonWidth(startPauseButton, 280, 44);
    }

    public void shutdown() {
        tickTimer.stop();
        pomodoroEngine.pause();
    }

    private void onEnableChanged() {
        if (suppressChangeEvents) {
            return;
        }
        boolean enabled = enableToggle.isSelected();
        userSettings.setPomodoroEnabled(enabled);
        userSettingsStore.save(userSettings);
        pomodoroEngine.setFeatureEnabled(enabled);
    }

    private void onDurationsChanged() {
        if (suppressChangeEvents) {
            return;
        }
        int workMinutes = ((Number) workMinutesSpinner.getValue()).intValue();
        int shortBreakMinutes = ((Number) shortBreakSpinner.getValue()).intValue();
        int longBreakMinutes = ((Number) longBreakSpinner.getValue()).intValue();
        int sessionsUntilLongBreak = ((Number) sessionsUntilLongBreakSpinner.getValue()).intValue();
        userSettings.setPomodoroWorkMinutes(workMinutes);
        userSettings.setPomodoroShortBreakMinutes(shortBreakMinutes);
        userSettings.setPomodoroLongBreakMinutes(longBreakMinutes);
        userSettings.setPomodoroSessionsUntilLongBreak(sessionsUntilLongBreak);
        userSettingsStore.save(userSettings);
        pomodoroEngine.updateDurations(workMinutes, shortBreakMinutes, longBreakMinutes, sessionsUntilLongBreak);
    }

    private void onStartPauseClicked() {
        if (pomodoroEngine.isTimerRunning()) {
            pomodoroEngine.pause();
        } else {
            pomodoroEngine.startOrResume();
        }
    }

    private void onPhaseCompleted(PomodoroPhase completedPhase) {
        Toolkit.getDefaultToolkit().beep();
        String caption = Messages.get(MessageCodes.UI_POMODORO_TITLE);
        String message = switch (completedPhase) {
            case WORK -> Messages.get(MessageCodes.UI_POMODORO_NOTIFY_WORK_DONE);
            case SHORT_BREAK -> Messages.get(MessageCodes.UI_POMODORO_NOTIFY_SHORT_BREAK_DONE);
            case LONG_BREAK -> Messages.get(MessageCodes.UI_POMODORO_NOTIFY_LONG_BREAK_DONE);
        };
        notificationPublisher.accept(caption, message);
        SwingUtilities.invokeLater(() -> showPhaseAlert(caption, message));
    }

    private void showPhaseAlert(String caption, String message) {
        if (phaseAlertVisible) {
            return;
        }
        phaseAlertVisible = true;
        try {
            pomodoroEngine.pause();
            PomodoroAlertDialog.show(caption, message);
            if (pomodoroEngine.isFeatureEnabled()) {
                pomodoroEngine.startOrResume();
            }
        } finally {
            phaseAlertVisible = false;
        }
    }

    private static String formatCountdown(int totalSeconds) {
        int safeSeconds = Math.max(totalSeconds, 0);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String resolvePhaseLabel(PomodoroPhase pomodoroPhase) {
        return switch (pomodoroPhase) {
            case WORK -> Messages.get(MessageCodes.UI_POMODORO_PHASE_WORK);
            case SHORT_BREAK -> Messages.get(MessageCodes.UI_POMODORO_PHASE_SHORT_BREAK);
            case LONG_BREAK -> Messages.get(MessageCodes.UI_POMODORO_PHASE_LONG_BREAK);
        };
    }

    private JPanel createCompactDurationCell(JLabel label, JSpinner spinner) {
        Dimension spinnerSize = new Dimension(72, 32);
        spinner.setPreferredSize(spinnerSize);
        spinner.setMinimumSize(spinnerSize);
        spinner.setMaximumSize(spinnerSize);

        label.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel cellPanel = new JPanel(new BorderLayout(8, 0));
        cellPanel.setOpaque(false);
        cellPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cellPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cellPanel.add(label, BorderLayout.CENTER);
        cellPanel.add(spinner, BorderLayout.EAST);
        return cellPanel;
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

    private JPanel createStackedTextPanel(JLabel titleLabel, JLabel hintLabel) {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(hintLabel);
        return textPanel;
    }
}
