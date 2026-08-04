package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.workpulsetracker.agent.buffer.ActivityInterval;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Долговременное хранилище закрытых интервалов активности (для статистики и таймлайна).
 * Сохраняются и ACTIVE, и IDLE; статистика по приложениям фильтрует IDLE отдельно.
 */
public final class ActivityStore {

    private static final Logger logger = LoggerFactory.getLogger(ActivityStore.class);
    private static final Type STORED_INTERVAL_LIST_TYPE =
            new TypeToken<List<StoredActivityInterval>>() {
            }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path intervalsFilePath;
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final List<ActivityInterval> storedActivityIntervals = new ArrayList<>();

    public ActivityStore() {
        this(LocalDataDirectory.getIntervalsFilePath());
    }

    public ActivityStore(Path intervalsFilePath) {
        this.intervalsFilePath = intervalsFilePath;
    }

    public void load() {
        reentrantLock.lock();
        try {
            Files.createDirectories(intervalsFilePath.getParent());
            if (!Files.exists(intervalsFilePath)) {
                storedActivityIntervals.clear();
                return;
            }
            try (Reader reader = Files.newBufferedReader(intervalsFilePath, StandardCharsets.UTF_8)) {
                List<StoredActivityInterval> storedIntervals = gson.fromJson(reader, STORED_INTERVAL_LIST_TYPE);
                storedActivityIntervals.clear();
                if (Objects.nonNull(storedIntervals)) {
                    storedActivityIntervals.addAll(
                            storedIntervals.stream()
                                    .map(this::toActivityInterval)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList())
                    );
                }
            }
            logger.info("Loaded intervals from file: {}", storedActivityIntervals.size());
        } catch (IOException exception) {
            logger.warn("Failed to read intervals.json: {}", exception.getMessage());
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
            storedActivityIntervals.add(activityInterval);
            saveLocked();
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

    private void saveLocked() {
        try {
            Files.createDirectories(intervalsFilePath.getParent());
            List<StoredActivityInterval> storedIntervals = storedActivityIntervals.stream()
                    .map(this::toStoredActivityInterval)
                    .collect(Collectors.toList());
            try (Writer writer = Files.newBufferedWriter(intervalsFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(storedIntervals, writer);
            }
        } catch (IOException exception) {
            logger.error("Failed to save intervals.json: {}", exception.getMessage(), exception);
        }
    }

    private ActivityInterval toActivityInterval(StoredActivityInterval storedActivityInterval) {
        if (Objects.isNull(storedActivityInterval)
                || StringUtils.isBlank(storedActivityInterval.getStartInstant())
                || StringUtils.isBlank(storedActivityInterval.getEndInstant())) {
            return null;
        }
        return new ActivityInterval(
                Instant.parse(storedActivityInterval.getStartInstant()),
                Instant.parse(storedActivityInterval.getEndInstant()),
                storedActivityInterval.getApplicationName(),
                storedActivityInterval.getWindowTitle(),
                storedActivityInterval.isIdle()
        );
    }

    private StoredActivityInterval toStoredActivityInterval(ActivityInterval activityInterval) {
        return new StoredActivityInterval(
                activityInterval.getStartInstant().toString(),
                activityInterval.getEndInstant().toString(),
                activityInterval.getApplicationName(),
                activityInterval.getWindowTitle(),
                activityInterval.isIdle()
        );
    }
}
