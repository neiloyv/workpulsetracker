package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.workpulsetracker.agent.buffer.ActivityInterval;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Чтение legacy {@code intervals.json} (включая обрезанный файл) для миграции в SQLite.
 */
final class JsonActivityIntervalReader {

    private static final Logger logger = LoggerFactory.getLogger(JsonActivityIntervalReader.class);
    private static final Gson GSON = new Gson();

    private JsonActivityIntervalReader() {
    }

    static List<ActivityInterval> readBestAvailable(Path intervalsFilePath) {
        LoadedIntervals currentLoadedIntervals = loadIntervalsFromPath(
                intervalsFilePath,
                IntervalsFileSource.CURRENT
        );
        LoadedIntervals temporaryLoadedIntervals = loadIntervalsFromPath(
                intervalsFilePath.resolveSibling(intervalsFilePath.getFileName() + ".tmp"),
                IntervalsFileSource.TEMPORARY
        );
        LoadedIntervals backupLoadedIntervals = loadIntervalsFromPath(
                intervalsFilePath.resolveSibling(intervalsFilePath.getFileName() + ".bak"),
                IntervalsFileSource.BACKUP
        );
        return Stream.of(currentLoadedIntervals, temporaryLoadedIntervals, backupLoadedIntervals)
                .filter(LoadedIntervals::fileExists)
                .max(Comparator
                        .comparingInt(LoadedIntervals::size)
                        .thenComparing(LoadedIntervals::parsedCompletely)
                        .thenComparingInt(loadedIntervals -> loadedIntervals.intervalsFileSource().getSelectionPriority()))
                .map(LoadedIntervals::activityIntervals)
                .orElse(List.of());
    }

    private static LoadedIntervals loadIntervalsFromPath(Path filePath, IntervalsFileSource intervalsFileSource) {
        if (!Files.exists(filePath)) {
            return new LoadedIntervals(List.of(), true, false, intervalsFileSource);
        }
        List<ActivityInterval> loadedActivityIntervals = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                StoredActivityInterval storedActivityInterval =
                        GSON.fromJson(jsonReader, StoredActivityInterval.class);
                ActivityInterval activityInterval = toActivityInterval(storedActivityInterval);
                if (Objects.nonNull(activityInterval)) {
                    loadedActivityIntervals.add(activityInterval);
                }
            }
            jsonReader.endArray();
            return new LoadedIntervals(loadedActivityIntervals, true, true, intervalsFileSource);
        } catch (JsonParseException | IOException exception) {
            logger.warn(
                    "schema=local Failed to parse {}, recovered {} intervals: {}",
                    filePath.getFileName(),
                    loadedActivityIntervals.size(),
                    exception.getMessage()
            );
            return new LoadedIntervals(loadedActivityIntervals, false, true, intervalsFileSource);
        }
    }

    private static ActivityInterval toActivityInterval(StoredActivityInterval storedActivityInterval) {
        if (Objects.isNull(storedActivityInterval)
                || StringUtils.isBlank(storedActivityInterval.getStartInstant())
                || StringUtils.isBlank(storedActivityInterval.getEndInstant())) {
            return null;
        }
        try {
            return new ActivityInterval(
                    Instant.parse(storedActivityInterval.getStartInstant()),
                    Instant.parse(storedActivityInterval.getEndInstant()),
                    storedActivityInterval.getApplicationName(),
                    storedActivityInterval.getWindowTitle(),
                    storedActivityInterval.isIdle()
            );
        } catch (RuntimeException exception) {
            logger.warn("schema=local Skipping invalid interval: {}", exception.getMessage());
            return null;
        }
    }

    private enum IntervalsFileSource {
        CURRENT(3),
        TEMPORARY(2),
        BACKUP(1);

        private final int selectionPriority;

        IntervalsFileSource(int selectionPriority) {
            this.selectionPriority = selectionPriority;
        }

        private int getSelectionPriority() {
            return selectionPriority;
        }
    }

    private record LoadedIntervals(
            List<ActivityInterval> activityIntervals,
            boolean parsedCompletely,
            boolean fileExists,
            IntervalsFileSource intervalsFileSource
    ) {
        private int size() {
            return activityIntervals.size();
        }
    }
}
