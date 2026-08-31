package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.icons.ApplicationIconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Одноразовая миграция legacy JSON-файлов в {@code agent.db}.
 */
final class JsonToSqliteMigrator {

    private static final Logger logger = LoggerFactory.getLogger(JsonToSqliteMigrator.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type RUNTIME_COUNTERS_TYPE =
            new TypeToken<Map<String, LocalAppRuntimeStore.AppRuntimeCounter>>() {
            }.getType();
    private static final Type EXECUTABLE_PATHS_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private JsonToSqliteMigrator() {
    }

    static void migrateIfNeeded(Connection connection) throws SQLException {
        migrateSettings(connection, LocalDataDirectory.getSettingsFilePath());
        migrateIntervals(connection, LocalDataDirectory.getIntervalsFilePath());
        migrateRuntimeCounters(connection, LocalDataDirectory.getAppRuntimeCountersFilePath());
        migrateExecutablePaths(connection, LocalDataDirectory.getExecutablePathsFilePath());
    }

    static void migrateFromDirectory(Connection connection, Path backupDirectoryPath) throws SQLException {
        migrateSettings(connection, backupDirectoryPath.resolve("settings.json"));
        migrateIntervals(connection, backupDirectoryPath.resolve("intervals.json"));
        migrateRuntimeCounters(connection, backupDirectoryPath.resolve("app-runtime-counters.json"));
        migrateExecutablePaths(connection, backupDirectoryPath.resolve("executable-paths.json"));
    }

    static void clearUserData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM activity_interval");
            statement.execute("DELETE FROM app_runtime_counter");
            statement.execute("DELETE FROM executable_path");
            statement.execute("DELETE FROM user_settings");
        }
    }

    private static void migrateSettings(Connection connection, Path settingsFilePath) throws SQLException {
        if (!isTableEmpty(connection, "user_settings") || !Files.isRegularFile(settingsFilePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(settingsFilePath, StandardCharsets.UTF_8)) {
            UserSettings userSettings = GSON.fromJson(reader, UserSettings.class);
            if (Objects.isNull(userSettings)) {
                return;
            }
            UserSettingsStore.saveToConnection(connection, userSettings);
            logger.info("schema=local Migrated settings.json into agent.db");
        } catch (IOException exception) {
            logger.warn("schema=local Failed to migrate settings.json: {}", exception.getMessage());
        }
    }

    private static void migrateIntervals(Connection connection, Path intervalsFilePath) throws SQLException {
        if (!isTableEmpty(connection, "activity_interval")) {
            return;
        }
        List<ActivityInterval> activityIntervals = JsonActivityIntervalReader.readBestAvailable(intervalsFilePath);
        if (activityIntervals.isEmpty()) {
            return;
        }
        ActivityStore.insertIntervals(connection, activityIntervals);
        logger.info("schema=local Migrated {} intervals into agent.db", activityIntervals.size());
    }

    private static void migrateRuntimeCounters(Connection connection, Path runtimeCountersFilePath) throws SQLException {
        if (!isTableEmpty(connection, "app_runtime_counter") || !Files.isRegularFile(runtimeCountersFilePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(runtimeCountersFilePath, StandardCharsets.UTF_8)) {
            Map<String, LocalAppRuntimeStore.AppRuntimeCounter> loadedCounters = GSON.fromJson(reader, RUNTIME_COUNTERS_TYPE);
            if (Objects.isNull(loadedCounters) || loadedCounters.isEmpty()) {
                return;
            }
            LocalAppRuntimeStore.replaceAllOnConnection(connection, loadedCounters);
            logger.info("schema=local Migrated {} app runtime counters into agent.db", loadedCounters.size());
        } catch (IOException exception) {
            logger.warn("schema=local Failed to migrate app-runtime-counters.json: {}", exception.getMessage());
        }
    }

    private static void migrateExecutablePaths(Connection connection, Path executablePathsFilePath) throws SQLException {
        if (!isTableEmpty(connection, "executable_path") || !Files.isRegularFile(executablePathsFilePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(executablePathsFilePath, StandardCharsets.UTF_8)) {
            Map<String, String> loadedExecutablePaths = GSON.fromJson(reader, EXECUTABLE_PATHS_TYPE);
            if (Objects.isNull(loadedExecutablePaths) || loadedExecutablePaths.isEmpty()) {
                return;
            }
            ApplicationIconService.replaceAllExecutablePathsOnConnection(connection, loadedExecutablePaths);
            logger.info("schema=local Migrated {} executable paths into agent.db", loadedExecutablePaths.size());
        } catch (IOException exception) {
            logger.warn("schema=local Failed to migrate executable-paths.json: {}", exception.getMessage());
        }
    }

    private static boolean isTableEmpty(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }
}
