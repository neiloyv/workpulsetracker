package com.workpulsetracker.agent.storage;

import com.workpulsetracker.agent.mode.AgentOperationMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsStoreTest {

    private Connection connection;

    @BeforeEach
    void openDatabase(@TempDir Path tempDir) throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + tempDir.resolve("settings-test.db").toAbsolutePath().toString().replace('\\', '/')
        );
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void roundTripsAllFieldsThroughJsonColumn() throws SQLException {
        createJsonSchema();

        UserSettings original = new UserSettings();
        original.setLanguageCode("uk");
        original.applyCredentials("USER@Example.com", "  access-key-123  ");
        original.applyAgentAuth("token-abc", "hw-1", 7L, 42L);
        original.setAutoStartTracking(true);
        original.setMinorUsageThresholdMinutes(11);
        original.setUsageChartMode("categories");
        original.setDailyWorkGoalHours(6);
        original.setPomodoroEnabled(true);
        original.setPomodoroWorkMinutes(30);
        original.addCustomProgramCategory("Reading");
        original.setApplicationCategoryId("idea64.exe", "Reading");
        original.setApplicationTracked("chrome", false);

        UserSettingsStore.saveToConnection(connection, original);
        UserSettings loaded = UserSettingsStore.loadFromConnection(connection);

        assertNotNull(loaded);
        assertEquals("uk", loaded.getLanguageCode());
        assertEquals("user@example.com", loaded.getEmail());
        assertEquals("access-key-123", loaded.getActivationKey());
        assertEquals("token-abc", loaded.getAccessToken());
        assertEquals(7L, loaded.getWorkerId());
        assertEquals(42L, loaded.getDeviceId());
        assertEquals(AgentOperationMode.NETWORK_SYNC, loaded.getOperationMode());
        assertTrue(loaded.isAutoStartTracking());
        assertEquals(11, loaded.getMinorUsageThresholdMinutes());
        assertEquals("CATEGORIES", loaded.getUsageChartMode());
        assertEquals(6, loaded.getDailyWorkGoalHours());
        assertTrue(loaded.isPomodoroEnabled());
        assertEquals(30, loaded.getPomodoroWorkMinutes());
        assertTrue(loaded.listAllProgramCategoryIds().contains("Reading"));
        assertEquals("Reading", loaded.getApplicationCategoryId("idea64"));
        assertFalse(loaded.isApplicationTracked("chrome"));
    }

    @Test
    void writesExactlyOneRow() throws SQLException {
        createJsonSchema();

        UserSettingsStore.saveToConnection(connection, new UserSettings());
        UserSettings second = new UserSettings();
        second.setLanguageCode("en");
        UserSettingsStore.saveToConnection(connection, second);

        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM user_settings")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
        assertEquals("en", UserSettingsStore.loadFromConnection(connection).getLanguageCode());
    }

    @Test
    void migratesLegacyColumnLayoutPreservingValues() throws SQLException {
        createLegacySchema();
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO user_settings ("
                            + "id, language_code, email, activation_key, access_token, operation_mode, "
                            + "setup_completed, auto_start_tracking, minor_usage_threshold_minutes, "
                            + "pomodoro_enabled, pomodoro_work_minutes, application_tracked_by_key_json"
                            + ") VALUES ("
                            + "1, 'uk', 'legacy@example.com', 'legacy-key', 'legacy-token', 'NETWORK_SYNC', "
                            + "1, 1, 9, 1, 45, '{\"chrome\":false}'"
                            + ")"
            );
        }

        UserSettingsStore.migrateLegacyColumnLayoutIfNeeded(connection);

        UserSettings loaded = UserSettingsStore.loadFromConnection(connection);
        assertNotNull(loaded);
        assertEquals("uk", loaded.getLanguageCode());
        assertEquals("legacy@example.com", loaded.getEmail());
        assertEquals("legacy-key", loaded.getActivationKey());
        assertEquals("legacy-token", loaded.getAccessToken());
        assertEquals(AgentOperationMode.NETWORK_SYNC, loaded.getOperationMode());
        assertTrue(loaded.isSetupCompleted());
        assertTrue(loaded.isAutoStartTracking());
        assertEquals(9, loaded.getMinorUsageThresholdMinutes());
        assertTrue(loaded.isPomodoroEnabled());
        assertEquals(45, loaded.getPomodoroWorkMinutes());
        assertFalse(loaded.isApplicationTracked("chrome"));

        // Повторный вызов на уже мигрированной базе — no-op.
        UserSettingsStore.migrateLegacyColumnLayoutIfNeeded(connection);
        assertEquals("uk", UserSettingsStore.loadFromConnection(connection).getLanguageCode());
    }

    private void createJsonSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE user_settings (id INTEGER PRIMARY KEY CHECK (id = 1), settings_json TEXT NOT NULL)"
            );
        }
    }

    private void createLegacySchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE user_settings ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                            + "language_code TEXT, email TEXT, activation_key TEXT, access_token TEXT, hardware_id TEXT,"
                            + "device_id INTEGER, worker_id INTEGER, operation_mode TEXT,"
                            + "local_only INTEGER NOT NULL DEFAULT 1, setup_completed INTEGER NOT NULL DEFAULT 0,"
                            + "auto_start_tracking INTEGER NOT NULL DEFAULT 0, launch_at_login INTEGER,"
                            + "minor_usage_threshold_minutes INTEGER, timeline_visible INTEGER,"
                            + "show_exceptions_on_timeline INTEGER, usage_chart_mode TEXT,"
                            + "show_statistics_table_percentages INTEGER, daily_work_goal_notification_enabled INTEGER,"
                            + "daily_work_goal_hours INTEGER, last_daily_work_goal_notification_date TEXT,"
                            + "minimize_to_tray INTEGER, last_report_directory_path TEXT, last_backup_directory_path TEXT,"
                            + "pomodoro_enabled INTEGER NOT NULL DEFAULT 0, pomodoro_work_minutes INTEGER,"
                            + "pomodoro_short_break_minutes INTEGER, pomodoro_long_break_minutes INTEGER,"
                            + "pomodoro_sessions_until_long_break INTEGER, pomodoro_tray_notifications INTEGER,"
                            + "pomodoro_confirmation_dialogs INTEGER, custom_program_categories_json TEXT,"
                            + "application_category_by_key_json TEXT, application_tracked_by_key_json TEXT"
                            + ")"
            );
        }
    }
}
