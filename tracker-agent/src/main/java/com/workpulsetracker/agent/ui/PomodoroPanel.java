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
import javax.swing.JComponent;
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
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
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
    private final List<JLabel> durationUnitLabels = new ArrayList<>();

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
        settingsCard.add(createSettingRow(
                createStackedTextPanel(enableLabel, enableHintLabel),
                enableToggle
        ));
        settingsCard.add(Box.createVerticalStrut(12));

        workMinutesLabel.setForeground(UiTheme.TEXT_PRIMARY);
        shortBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        longBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);
        sessionsUntilLongBreakLabel.setForeground(UiTheme.TEXT_PRIMARY);

        workMinutesSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        shortBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        longBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());
        sessionsUntilLongBreakSpinner.addChangeListener(changeEvent -> onDurationsChanged());

        settingsCard.add(createDurationRow(workMinutesLabel, workMinutesSpinner, true));
        settingsCard.add(Box.createVerticalStrut(8));
        settingsCard.add(createDurationRow(shortBreakLabel, shortBreakSpinner, true));
        settingsCard.add(Box.createVerticalStrut(8));
        settingsCard.add(createDurationRow(longBreakLabel, longBreakSpinner, true));
        settingsCard.add(Box.createVerticalStrut(8));
        settingsCard.add(createDurationRow(sessionsUntilLongBreakLabel, sessionsUntilLongBreakSpinner, false));

        timerCard.setLayout(new BoxLayout(timerCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(timerCard);

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
        startPauseButton.setMaximumSize(new Dimension(220, 44));
        skipButton.setMaximumSize(new Dimension(220, 40));
        startPauseButton.addActionListener(actionEvent -> onStartPauseClicked());
        skipButton.addActionListener(actionEvent -> pomodoroEngine.skip());
        UiTheme.stylePrimaryButton(startPauseButton);
        UiTheme.styleSecondaryButton(skipButton);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(startPauseButton);
        buttonsPanel.add(Box.createVerticalStrut(10));
        buttonsPanel.add(skipButton);

        timerCard.add(phaseLabel);
        timerCard.add(Box.createVerticalStrut(8));
        timerCard.add(timerLabel);
        timerCard.add(Box.createVerticalStrut(8));
        timerCard.add(completedTodayLabel);
        timerCard.add(Box.createVerticalStrut(18));
        timerCard.add(buttonsPanel);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        timerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(settingsCard);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(timerCard);

        add(contentPanel, BorderLayout.NORTH);
        retranslate();
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
        enableHintLabel.setText("<html><body style='width:420px'>"
                + Messages.get(MessageCodes.UI_POMODORO_ENABLE_HINT)
                + "</body></html>");
        workMinutesLabel.setText(Messages.get(MessageCodes.UI_POMODORO_WORK));
        shortBreakLabel.setText(Messages.get(MessageCodes.UI_POMODORO_SHORT_BREAK));
        longBreakLabel.setText(Messages.get(MessageCodes.UI_POMODORO_LONG_BREAK));
        sessionsUntilLongBreakLabel.setText(Messages.get(MessageCodes.UI_POMODORO_SESSIONS_UNTIL_LONG));
        String minutesUnit = Messages.get(MessageCodes.UI_POMODORO_MINUTES);
        durationUnitLabels.forEach(unitLabel -> unitLabel.setText(minutesUnit));
        skipButton.setText(Messages.get(MessageCodes.UI_POMODORO_SKIP));
        refresh();
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
        durationUnitLabels.forEach(UiTheme::styleMutedLabel);
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
            return;
        }
        if (timerRunning) {
            startPauseButton.setText(Messages.get(MessageCodes.UI_POMODORO_PAUSE));
            UiTheme.styleDangerButton(startPauseButton);
        } else {
            startPauseButton.setText(Messages.get(MessageCodes.UI_POMODORO_START));
            UiTheme.stylePrimaryButton(startPauseButton);
        }
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

    private JPanel createDurationRow(JLabel label, JSpinner spinner, boolean showMinutesUnit) {
        spinner.setPreferredSize(new Dimension(96, 32));
        spinner.setMinimumSize(new Dimension(96, 32));
        spinner.setMaximumSize(new Dimension(96, 32));

        JLabel unitLabel = new JLabel(" ");
        unitLabel.setPreferredSize(new Dimension(36, 32));
        unitLabel.setMinimumSize(new Dimension(36, 32));
        unitLabel.setMaximumSize(new Dimension(36, 32));
        unitLabel.setHorizontalAlignment(SwingConstants.LEFT);
        UiTheme.styleMutedLabel(unitLabel);
        if (showMinutesUnit) {
            durationUnitLabels.add(unitLabel);
        } else {
            unitLabel.setText("");
        }

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controlPanel.setOpaque(false);
        controlPanel.setPreferredSize(new Dimension(148, 36));
        controlPanel.setMinimumSize(new Dimension(148, 36));
        controlPanel.setMaximumSize(new Dimension(148, 36));
        controlPanel.add(spinner);
        controlPanel.add(unitLabel);
        return createSettingRow(label, controlPanel);
    }

    private JPanel createSettingRow(JComponent leftComponent, JComponent rightComponent) {
        JPanel rowPanel = new JPanel(new BorderLayout(16, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        rowPanel.add(leftComponent, BorderLayout.CENTER);
        rowPanel.add(rightComponent, BorderLayout.EAST);
        return rowPanel;
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
