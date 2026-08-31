package com.workpulsetracker.agent.storage;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Долговременное хранилище закрытых интервалов активности в SQLite.
 */
public final class ActivityStore {

    private static final Logger logger = LoggerFactory.getLogger(ActivityStore.class);
    private static final String SELECT_ALL_INTERVALS_SQL =
            "SELECT start_instant, end_instant, application_name, window_title, idle "
                    + "FROM activity_interval ORDER BY id";
    private static final String INSERT_INTERVAL_SQL =
            "INSERT INTO activity_interval (start_instant, end_instant, application_name, window_title, idle) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final List<ActivityInterval> storedActivityIntervals = new ArrayList<>();

    public void load() {
        reentrantLock.lock();
        try {
            storedActivityIntervals.clear();
            storedActivityIntervals.addAll(LocalSqliteDatabase.getInstance().call(ActivityStore::loadIntervalsFromConnection));
            logger.info("schema=local Loaded intervals from agent.db: {}", storedActivityIntervals.size());
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to read activity_interval: {}", exception.getMessage());
            storedActivityIntervals.clear();
        } finally {
            reentrantLock.unlock();
        }
    }

    public void appendClosedInterval(ActivityInterval activityInterval) {
        if (Objects.isNull(activityInterval) || activityInterval.isOpen()) {
            return;
        }
        if (activityInterval.getDurationSeconds() <= 0L) {
            return;
        }

        reentrantLock.lock();
        try {
            LocalSqliteDatabase.getInstance().run(connection -> insertInterval(connection, activityInterval));
            storedActivityIntervals.add(activityInterval);
        } catch (SQLException exception) {
            logger.error("schema=local Failed to insert activity_interval: {}", exception.getMessage(), exception);
        } finally {
            reentrantLock.unlock();
        }
    }

    public List<ActivityInterval> getAllIntervals() {
        reentrantLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(storedActivityIntervals));
        } finally {
            reentrantLock.unlock();
        }
    }

    static List<ActivityInterval> loadIntervalsFromConnection(Connection connection) throws SQLException {
        List<ActivityInterval> loadedActivityIntervals = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_INTERVALS_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                ActivityInterval activityInterval = toActivityInterval(resultSet);
                if (Objects.nonNull(activityInterval)) {
                    loadedActivityIntervals.add(activityInterval);
                }
            }
        }
        return loadedActivityIntervals;
    }

    static void insertIntervals(Connection connection, List<ActivityInterval> activityIntervals) throws SQLException {
        if (Objects.isNull(activityIntervals) || activityIntervals.isEmpty()) {
            return;
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTERVAL_SQL)) {
            for (ActivityInterval activityInterval : activityIntervals) {
                bindInterval(preparedStatement, activityInterval);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    static void insertInterval(Connection connection, ActivityInterval activityInterval) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTERVAL_SQL)) {
            bindInterval(preparedStatement, activityInterval);
            preparedStatement.executeUpdate();
        }
    }

    private static void bindInterval(PreparedStatement preparedStatement, ActivityInterval activityInterval)
            throws SQLException {
        preparedStatement.setString(1, activityInterval.getStartInstant().toString());
        preparedStatement.setString(2, activityInterval.getEndInstant().toString());
        preparedStatement.setString(3, activityInterval.getApplicationName());
        preparedStatement.setString(4, activityInterval.getWindowTitle());
        preparedStatement.setInt(5, activityInterval.isIdle() ? 1 : 0);
    }

    private static ActivityInterval toActivityInterval(ResultSet resultSet) {
        try {
            return new ActivityInterval(
                    Instant.parse(resultSet.getString("start_instant")),
                    Instant.parse(resultSet.getString("end_instant")),
                    resultSet.getString("application_name"),
                    resultSet.getString("window_title"),
                    resultSet.getInt("idle") != 0
            );
        } catch (RuntimeException | SQLException exception) {
            logger.warn("schema=local Skipping invalid interval: {}", exception.getMessage());
            return null;
        }
    }
}
