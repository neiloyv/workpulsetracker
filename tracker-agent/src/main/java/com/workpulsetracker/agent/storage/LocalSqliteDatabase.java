package com.workpulsetracker.agent.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Локальная SQLite-база агента: {@code ~/.workpulsetracker/agent.db}.
 */
public final class LocalSqliteDatabase {

    private static final Logger logger = LoggerFactory.getLogger(LocalSqliteDatabase.class);
    private static final LocalSqliteDatabase INSTANCE = new LocalSqliteDatabase(LocalDataDirectory.getDatabaseFilePath());

    private final Path databaseFilePath;
    private Connection connection;

    public static LocalSqliteDatabase getInstance() {
        return INSTANCE;
    }

    LocalSqliteDatabase(Path databaseFilePath) {
        this.databaseFilePath = Objects.requireNonNull(databaseFilePath);
    }

    public Path getDatabaseFilePath() {
        return databaseFilePath;
    }

    public synchronized <T> T call(SqlWork<T> sqlWork) throws SQLException {
        Objects.requireNonNull(sqlWork);
        ensureOpen();
        return sqlWork.run(connection);
    }

    public synchronized void run(SqlAction sqlAction) throws SQLException {
        call(connection -> {
            sqlAction.run(connection);
            return null;
        });
    }

    public synchronized void copyDatabaseTo(Path targetFilePath) throws IOException, SQLException {
        Objects.requireNonNull(targetFilePath);
        ensureOpen();
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        }
        Path parentDirectoryPath = targetFilePath.getParent();
        if (Objects.nonNull(parentDirectoryPath)) {
            Files.createDirectories(parentDirectoryPath);
        }
        Files.copy(databaseFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public synchronized void replaceDatabaseFile(Path sourceDatabaseFilePath) throws IOException, SQLException {
        Objects.requireNonNull(sourceDatabaseFilePath);
        closeQuietly();
        Files.createDirectories(databaseFilePath.getParent());
        Files.deleteIfExists(getWalFilePath());
        Files.deleteIfExists(getSharedMemoryFilePath());
        Files.copy(sourceDatabaseFilePath, databaseFilePath, StandardCopyOption.REPLACE_EXISTING);
        ensureOpen();
        logger.info("schema=local Replaced agent.db from backup");
    }

    public synchronized void closeQuietly() {
        if (Objects.isNull(connection)) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to close agent.db: {}", exception.getMessage());
        } finally {
            connection = null;
        }
    }

    private void ensureOpen() throws SQLException {
        if (Objects.nonNull(connection) && !connection.isClosed()) {
            return;
        }
        try {
            Files.createDirectories(databaseFilePath.getParent());
        } catch (IOException exception) {
            throw new SQLException("Failed to create data directory for agent.db", exception);
        }
        connection = DriverManager.getConnection(buildJdbcUrl(databaseFilePath));
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA foreign_keys=ON");
        }
        createSchema(connection);
        JsonToSqliteMigrator.migrateIfNeeded(connection);
        logger.info("schema=local Opened agent.db at {}", databaseFilePath);
    }

    private Path getWalFilePath() {
        return databaseFilePath.resolveSibling(databaseFilePath.getFileName() + "-wal");
    }

    private Path getSharedMemoryFilePath() {
        return databaseFilePath.resolveSibling(databaseFilePath.getFileName() + "-shm");
    }

    private static String buildJdbcUrl(Path databaseFilePath) {
        return "jdbc:sqlite:" + databaseFilePath.toAbsolutePath().toString().replace('\\', '/');
    }

    private static void createSchema(Connection connection) throws SQLException {
        UserSettingsStore.migrateLegacyColumnLayoutIfNeeded(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS user_settings ("
                            + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                            + "settings_json TEXT NOT NULL"
                            + ")"
            );
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS activity_interval ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "start_instant TEXT NOT NULL,"
                            + "end_instant TEXT NOT NULL,"
                            + "application_name TEXT NOT NULL,"
                            + "window_title TEXT NOT NULL DEFAULT '',"
                            + "idle INTEGER NOT NULL DEFAULT 0"
                            + ")"
            );
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_activity_interval_start "
                            + "ON activity_interval(start_instant)"
            );
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS app_runtime_counter ("
                            + "app_identifier TEXT PRIMARY KEY,"
                            + "display_name TEXT,"
                            + "current_value_seconds INTEGER NOT NULL DEFAULT 0,"
                            + "last_synced_value_seconds INTEGER NOT NULL DEFAULT 0"
                            + ")"
            );
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS executable_path ("
                            + "application_name TEXT PRIMARY KEY,"
                            + "executable_path TEXT NOT NULL"
                            + ")"
            );
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlAction {
        void run(Connection connection) throws SQLException;
    }
}
