package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Проверяет дневной счётчик активной работы и показывает уведомление при достижении цели.
 */
public final class DailyWorkGoalNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DailyWorkGoalNotifier.class);
    private static final DateTimeFormatter NOTIFICATION_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StatisticsService statisticsService;
    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;

    private boolean notificationDialogVisible;

    public DailyWorkGoalNotifier(
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore
    ) {
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
    }

    public void checkAndNotify() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::checkAndNotify);
            return;
        }
        if (!userSettings.isDailyWorkGoalNotificationEnabled() || notificationDialogVisible) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String todayDateText = NOTIFICATION_DATE_FORMATTER.format(today);
        if (Objects.equals(todayDateText, userSettings.getLastDailyWorkGoalNotificationDate())) {
            return;
        }

        int dailyWorkGoalHours = userSettings.getDailyWorkGoalHours();
        long dailyWorkGoalSeconds = dailyWorkGoalHours * 3600L;
        long todayActiveSeconds = statisticsService.buildTodayActiveSeconds();
        if (todayActiveSeconds < dailyWorkGoalSeconds) {
            return;
        }

        showNotificationDialog(dailyWorkGoalHours, todayDateText);
    }

    private void showNotificationDialog(int dailyWorkGoalHours, String todayDateText) {
        notificationDialogVisible = true;
        try {
            String notificationTitle = Messages.get(MessageCodes.UI_DAILY_WORK_GOAL_NOTIFICATION_TITLE);
            String notificationMessage = Messages.get(
                    MessageCodes.UI_DAILY_WORK_GOAL_NOTIFICATION_MESSAGE,
                    dailyWorkGoalHours
            );
            PomodoroAlertDialog.show(notificationTitle, notificationMessage);
            userSettings.setLastDailyWorkGoalNotificationDate(todayDateText);
            userSettingsStore.save(userSettings);
            logger.info(
                    "schema={} Daily work goal notification shown: goalHours={} notificationDate={}",
                    "local",
                    dailyWorkGoalHours,
                    todayDateText
            );
        } finally {
            notificationDialogVisible = false;
        }
    }
}
