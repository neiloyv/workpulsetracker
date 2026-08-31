package com.workpulsetracker.agent.storage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Локальные накопленные счётчики runtime по приложениям (для delta telemetry).
 * Не сбрасывает счётчики при ошибках сети — только после успешного ack сервера
 * lastSyncedValue обновляется, а currentValue остаётся монотонным.
 */
public final class LocalAppRuntimeStore {

    private static final Logger logger = LoggerFactory.getLogger(LocalAppRuntimeStore.class);
    private static final String SELECT_ALL_COUNTERS_SQL =
            "SELECT app_identifier, display_name, current_value_seconds, last_synced_value_seconds "
                    + "FROM app_runtime_counter";
    private static final String DELETE_ALL_COUNTERS_SQL = "DELETE FROM app_runtime_counter";
    private static final String UPSERT_COUNTER_SQL =
            "INSERT INTO app_runtime_counter ("
                    + "app_identifier, display_name, current_value_seconds, last_synced_value_seconds"
                    + ") VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(app_identifier) DO UPDATE SET "
                    + "display_name = excluded.display_name, "
                    + "current_value_seconds = excluded.current_value_seconds, "
                    + "last_synced_value_seconds = excluded.last_synced_value_seconds";

    private final Map<String, AppRuntimeCounter> countersByAppIdentifier = new ConcurrentHashMap<>();

    public synchronized void load() {
        try {
            Map<String, AppRuntimeCounter> loadedCounters =
                    LocalSqliteDatabase.getInstance().call(LocalAppRuntimeStore::loadCountersFromConnection);
            countersByAppIdentifier.clear();
            countersByAppIdentifier.putAll(loadedCounters);
            logger.info("schema=local Loaded {} local app runtime counters", countersByAppIdentifier.size());
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to load app runtime counters: {}", exception.getMessage());
        }
    }

    public synchronized void save() {
        try {
            LocalSqliteDatabase.getInstance().run(connection -> replaceAllOnConnection(connection, countersByAppIdentifier));
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to save app runtime counters: {}", exception.getMessage());
        }
    }

    public synchronized void addSeconds(String appIdentifier, String displayName, long seconds) {
        if (StringUtils.isBlank(appIdentifier) || seconds <= 0L) {
            return;
        }
        String normalizedIdentifier = appIdentifier.trim().toLowerCase(Locale.ROOT);
        AppRuntimeCounter appRuntimeCounter = countersByAppIdentifier.computeIfAbsent(
                normalizedIdentifier,
                key -> new AppRuntimeCounter(displayName, 0L, 0L)
        );
        if (StringUtils.isNotBlank(displayName)) {
            appRuntimeCounter.setDisplayName(displayName.trim());
        }
        appRuntimeCounter.setCurrentValueSeconds(appRuntimeCounter.getCurrentValueSeconds() + seconds);
        persistCounter(normalizedIdentifier, appRuntimeCounter);
    }

    public synchronized List<AppRuntimeSnapshot> snapshotPendingUpload() {
        return countersByAppIdentifier.entrySet().stream()
                .map(entry -> new AppRuntimeSnapshot(
                        entry.getKey(),
                        entry.getValue().getDisplayName(),
                        entry.getValue().getCurrentValueSeconds()
                ))
                .filter(snapshot -> snapshot.currentValueSeconds() > 0L)
                .collect(Collectors.toList());
    }

    /**
     * После успешной отправки помечает lastSyncedValue = currentValue.
     * Сами счётчики не обнуляются.
     */
    public synchronized void markSynced(List<String> appIdentifiers) {
        if (Objects.isNull(appIdentifiers) || appIdentifiers.isEmpty()) {
            return;
        }
        appIdentifiers.stream()
                .filter(StringUtils::isNotBlank)
                .map(appIdentifier -> appIdentifier.trim().toLowerCase(Locale.ROOT))
                .forEach(normalizedIdentifier -> {
                    AppRuntimeCounter appRuntimeCounter = countersByAppIdentifier.get(normalizedIdentifier);
                    if (Objects.nonNull(appRuntimeCounter)) {
                        appRuntimeCounter.setLastSyncedValueSeconds(appRuntimeCounter.getCurrentValueSeconds());
                        persistCounter(normalizedIdentifier, appRuntimeCounter);
                    }
                });
    }

    /**
     * Восстанавливает локальные totals из reverse sync, не уменьшая уже накопленное.
     */
    public synchronized void restoreFromServerTotals(List<AppRuntimeSnapshot> serverSnapshots) {
        if (Objects.isNull(serverSnapshots)) {
            return;
        }
        serverSnapshots.stream()
                .filter(snapshot -> StringUtils.isNotBlank(snapshot.appIdentifier()))
                .forEach(snapshot -> {
                    String normalizedIdentifier = snapshot.appIdentifier().trim().toLowerCase(Locale.ROOT);
                    AppRuntimeCounter appRuntimeCounter = countersByAppIdentifier.computeIfAbsent(
                            normalizedIdentifier,
                            key -> new AppRuntimeCounter(snapshot.displayName(), 0L, 0L)
                    );
                    if (StringUtils.isNotBlank(snapshot.displayName())) {
                        appRuntimeCounter.setDisplayName(snapshot.displayName().trim());
                    }
                    long restoredValue = Math.max(snapshot.currentValueSeconds(), appRuntimeCounter.getCurrentValueSeconds());
                    appRuntimeCounter.setCurrentValueSeconds(restoredValue);
                    appRuntimeCounter.setLastSyncedValueSeconds(restoredValue);
                });
        save();
    }

    static Map<String, AppRuntimeCounter> loadCountersFromConnection(Connection connection) throws SQLException {
        Map<String, AppRuntimeCounter> loadedCounters = new LinkedHashMap<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_COUNTERS_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String appIdentifier = resultSet.getString("app_identifier");
                if (StringUtils.isBlank(appIdentifier)) {
                    continue;
                }
                loadedCounters.put(
                        appIdentifier.toLowerCase(Locale.ROOT),
                        new AppRuntimeCounter(
                                resultSet.getString("display_name"),
                                resultSet.getLong("current_value_seconds"),
                                resultSet.getLong("last_synced_value_seconds")
                        )
                );
            }
        }
        return loadedCounters;
    }

    static void replaceAllOnConnection(
            Connection connection,
            Map<String, AppRuntimeCounter> countersByAppIdentifier
    ) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_ALL_COUNTERS_SQL);
             PreparedStatement upsertStatement = connection.prepareStatement(UPSERT_COUNTER_SQL)) {
            deleteStatement.executeUpdate();
            countersByAppIdentifier.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && Objects.nonNull(entry.getValue()))
                    .forEach(entry -> {
                        try {
                            bindCounter(
                                    upsertStatement,
                                    entry.getKey().toLowerCase(Locale.ROOT),
                                    entry.getValue()
                            );
                            upsertStatement.addBatch();
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
            upsertStatement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void persistCounter(String appIdentifier, AppRuntimeCounter appRuntimeCounter) {
        try {
            LocalSqliteDatabase.getInstance().run(connection -> {
                try (PreparedStatement preparedStatement = connection.prepareStatement(UPSERT_COUNTER_SQL)) {
                    bindCounter(preparedStatement, appIdentifier, appRuntimeCounter);
                    preparedStatement.executeUpdate();
                }
            });
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to persist app runtime counter {}: {}", appIdentifier, exception.getMessage());
        }
    }

    private static void bindCounter(
            PreparedStatement preparedStatement,
            String appIdentifier,
            AppRuntimeCounter appRuntimeCounter
    ) throws SQLException {
        preparedStatement.setString(1, appIdentifier);
        preparedStatement.setString(2, appRuntimeCounter.getDisplayName());
        preparedStatement.setLong(3, appRuntimeCounter.getCurrentValueSeconds());
        preparedStatement.setLong(4, appRuntimeCounter.getLastSyncedValueSeconds());
    }

    public static final class AppRuntimeCounter {
        private String displayName;
        private long currentValueSeconds;
        private long lastSyncedValueSeconds;

        public AppRuntimeCounter() {
        }

        public AppRuntimeCounter(String displayName, long currentValueSeconds, long lastSyncedValueSeconds) {
            this.displayName = displayName;
            this.currentValueSeconds = currentValueSeconds;
            this.lastSyncedValueSeconds = lastSyncedValueSeconds;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public long getCurrentValueSeconds() {
            return currentValueSeconds;
        }

        public void setCurrentValueSeconds(long currentValueSeconds) {
            this.currentValueSeconds = currentValueSeconds;
        }

        public long getLastSyncedValueSeconds() {
            return lastSyncedValueSeconds;
        }

        public void setLastSyncedValueSeconds(long lastSyncedValueSeconds) {
            this.lastSyncedValueSeconds = lastSyncedValueSeconds;
        }
    }

    public record AppRuntimeSnapshot(String appIdentifier, String displayName, long currentValueSeconds) {
    }
}
