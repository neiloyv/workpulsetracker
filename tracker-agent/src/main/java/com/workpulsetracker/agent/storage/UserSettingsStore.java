package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.workpulsetracker.agent.mode.AgentOperationMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Чтение/запись {@link UserSettings} в SQLite {@code user_settings}.
 * <p>
 * Настройки хранятся одной JSON-строкой в единственной строке таблицы ({@code id = 1}):
 * это конфиг из одной записи, колоночная модель не давала выигрыша по запросам,
 * а стоила ~35 колонок и ручного маппинга каждого поля.
 */
public final class UserSettingsStore {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsStore.class);
    private static final Gson GSON = new GsonBuilder().create();

    private static final String SELECT_SETTINGS_SQL = "SELECT settings_json FROM user_settings WHERE id = 1";
    private static final String UPSERT_SETTINGS_SQL =
            "INSERT INTO user_settings (id, settings_json) VALUES (1, ?) "
                    + "ON CONFLICT(id) DO UPDATE SET settings_json = excluded.settings_json";

    public UserSettings loadOrCreateDefault() {
        try {
            UserSettings loadedUserSettings = LocalSqliteDatabase.getInstance().call(UserSettingsStore::loadFromConnection);
            if (Objects.nonNull(loadedUserSettings)) {
                return loadedUserSettings;
            }
            UserSettings userSettings = new UserSettings();
            save(userSettings);
            return userSettings;
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to read user_settings: {}", exception.getMessage());
            return new UserSettings();
        }
    }

    public void save(UserSettings userSettings) {
        try {
            LocalSqliteDatabase.getInstance().run(connection -> saveToConnection(connection, userSettings));
        } catch (SQLException exception) {
            logger.error("schema=local Failed to save user_settings: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Перечитывает настройки в уже существующий экземпляр (после импорта бэкапа).
     */
    public void reloadInto(UserSettings userSettings) {
        Objects.requireNonNull(userSettings);
        UserSettings loadedUserSettings = loadOrCreateDefault();
        userSettings.copyFrom(loadedUserSettings);
    }

    static UserSettings loadFromConnection(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SETTINGS_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }
            return parseSettingsJson(resultSet.getString("settings_json"));
        }
    }

    static void saveToConnection(Connection connection, UserSettings userSettings) throws SQLException {
        Objects.requireNonNull(userSettings);
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPSERT_SETTINGS_SQL)) {
            preparedStatement.setString(1, GSON.toJson(userSettings));
            preparedStatement.executeUpdate();
        }
    }

    private static UserSettings parseSettingsJson(String settingsJson) {
        if (StringUtils.isBlank(settingsJson)) {
            return null;
        }
        try {
            return GSON.fromJson(settingsJson, UserSettings.class);
        } catch (JsonSyntaxException exception) {
            logger.warn("schema=local Corrupted user_settings JSON, falling back to defaults: {}", exception.getMessage());
            return null;
        }
    }

    // --- Разовая миграция схемы: колоночная таблица user_settings -> одна JSON-строка. ---
    // Выполняется при открытии базы; после того как у всех пользователей база пересоздана,
    // этот блок и импорты java.sql.Statement можно удалить.

    static void migrateLegacyColumnLayoutIfNeeded(Connection connection) throws SQLException {
        if (!legacyColumnLayoutPresent(connection)) {
            return;
        }
        UserSettings legacyUserSettings = readLegacyRow(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE user_settings");
            statement.execute(
                    "CREATE TABLE user_settings ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                            + "settings_json TEXT NOT NULL"
                            + ")"
            );
        }
        if (Objects.nonNull(legacyUserSettings)) {
            saveToConnection(connection, legacyUserSettings);
            logger.info("schema=local Migrated column-based user_settings into JSON layout");
        }
    }

    private static boolean legacyColumnLayoutPresent(Connection connection) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "user_settings", "language_code")) {
            return columns.next();
        }
    }

    private static UserSettings readLegacyRow(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM user_settings WHERE id = 1")) {
            return resultSet.next() ? mapLegacyRow(resultSet) : null;
        }
    }

    private static UserSettings mapLegacyRow(ResultSet resultSet) throws SQLException {
        UserSettings userSettings = new UserSettings();
        userSettings.setLanguageCode(resultSet.getString("language_code"));
        userSettings.setEmail(resultSet.getString("email"));
        userSettings.setActivationKey(resultSet.getString("activation_key"));
        userSettings.setAccessToken(resultSet.getString("access_token"));
        userSettings.setHardwareId(resultSet.getString("hardware_id"));
        userSettings.setDeviceId(getNullableLong(resultSet, "device_id"));
        userSettings.setWorkerId(getNullableLong(resultSet, "worker_id"));
        userSettings.setOperationMode(AgentOperationMode.fromCode(resultSet.getString("operation_mode")));
        userSettings.setSetupCompleted(resultSet.getInt("setup_completed") != 0);
        userSettings.setAutoStartTracking(resultSet.getInt("auto_start_tracking") != 0);
        applyNullableBoolean(resultSet, "launch_at_login", userSettings::setLaunchAtLogin);
        applyNullableInt(resultSet, "minor_usage_threshold_minutes", userSettings::setMinorUsageThresholdMinutes);
        applyNullableBoolean(resultSet, "timeline_visible", userSettings::setTimelineVisible);
        applyNullableBoolean(resultSet, "show_exceptions_on_timeline", userSettings::setShowExceptionsOnTimeline);
        userSettings.setUsageChartMode(resultSet.getString("usage_chart_mode"));
        applyNullableBoolean(resultSet, "show_statistics_table_percentages",
                userSettings::setShowStatisticsTablePercentages);
        applyNullableBoolean(resultSet, "daily_work_goal_notification_enabled",
                userSettings::setDailyWorkGoalNotificationEnabled);
        applyNullableInt(resultSet, "daily_work_goal_hours", userSettings::setDailyWorkGoalHours);
        userSettings.setLastDailyWorkGoalNotificationDate(resultSet.getString("last_daily_work_goal_notification_date"));
        applyNullableBoolean(resultSet, "minimize_to_tray", userSettings::setMinimizeToTray);
        userSettings.setLastReportDirectoryPath(resultSet.getString("last_report_directory_path"));
        userSettings.setLastBackupDirectoryPath(resultSet.getString("last_backup_directory_path"));
        userSettings.setPomodoroEnabled(resultSet.getInt("pomodoro_enabled") != 0);
        applyNullableInt(resultSet, "pomodoro_work_minutes", userSettings::setPomodoroWorkMinutes);
        applyNullableInt(resultSet, "pomodoro_short_break_minutes", userSettings::setPomodoroShortBreakMinutes);
        applyNullableInt(resultSet, "pomodoro_long_break_minutes", userSettings::setPomodoroLongBreakMinutes);
        applyNullableInt(resultSet, "pomodoro_sessions_until_long_break",
                userSettings::setPomodoroSessionsUntilLongBreak);
        applyNullableBoolean(resultSet, "pomodoro_tray_notifications", userSettings::setPomodoroTrayNotifications);
        applyNullableBoolean(resultSet, "pomodoro_confirmation_dialogs", userSettings::setPomodoroConfirmationDialogs);
        userSettings.setCustomProgramCategories(
                legacyJson(resultSet.getString("custom_program_categories_json"), LEGACY_STRING_LIST_TYPE));
        userSettings.setApplicationCategoryByKey(
                legacyJson(resultSet.getString("application_category_by_key_json"), LEGACY_STRING_MAP_TYPE));
        userSettings.setApplicationTrackedByKey(
                legacyJson(resultSet.getString("application_tracked_by_key_json"), LEGACY_BOOLEAN_MAP_TYPE));
        return userSettings;
    }

    private static final Type LEGACY_STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final Type LEGACY_STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type LEGACY_BOOLEAN_MAP_TYPE = new TypeToken<Map<String, Boolean>>() {
    }.getType();

    private static <T> T legacyJson(String json, Type type) {
        return StringUtils.isBlank(json) ? null : GSON.fromJson(json, type);
    }

    private static Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private static void applyNullableInt(ResultSet resultSet, String columnName, java.util.function.IntConsumer setter)
            throws SQLException {
        int value = resultSet.getInt(columnName);
        if (!resultSet.wasNull()) {
            setter.accept(value);
        }
    }

    private static void applyNullableBoolean(
            ResultSet resultSet,
            String columnName,
            java.util.function.Consumer<Boolean> setter
    ) throws SQLException {
        int value = resultSet.getInt(columnName);
        if (!resultSet.wasNull()) {
            setter.accept(value != 0);
        }
    }
}
