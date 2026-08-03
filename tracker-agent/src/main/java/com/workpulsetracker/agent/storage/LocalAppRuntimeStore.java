package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
    private static final Type STORE_TYPE = new TypeToken<Map<String, AppRuntimeCounter>>() {
    }.getType();

    private final Path storeFilePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, AppRuntimeCounter> countersByAppIdentifier = new ConcurrentHashMap<>();

    public LocalAppRuntimeStore() {
        this(LocalDataDirectory.getAppRuntimeCountersFilePath());
    }

    public LocalAppRuntimeStore(Path storeFilePath) {
        this.storeFilePath = storeFilePath;
    }

    public synchronized void load() {
        if (!Files.exists(storeFilePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(storeFilePath, StandardCharsets.UTF_8)) {
            Map<String, AppRuntimeCounter> loadedCounters = gson.fromJson(reader, STORE_TYPE);
            countersByAppIdentifier.clear();
            if (Objects.nonNull(loadedCounters)) {
                loadedCounters.entrySet().stream()
                        .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && Objects.nonNull(entry.getValue()))
                        .forEach(entry -> countersByAppIdentifier.put(
                                entry.getKey().toLowerCase(Locale.ROOT),
                                entry.getValue()
                        ));
            }
            logger.info("Loaded {} local app runtime counters", countersByAppIdentifier.size());
        } catch (IOException exception) {
            logger.warn("Failed to load app runtime counters: {}", exception.getMessage());
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(storeFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storeFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(new LinkedHashMap<>(countersByAppIdentifier), writer);
            }
        } catch (IOException exception) {
            logger.warn("Failed to save app runtime counters: {}", exception.getMessage());
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
        save();
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

    public synchronized Map<String, Long> getAllCurrentValues() {
        return countersByAppIdentifier.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getCurrentValueSeconds(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
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
                    }
                });
        save();
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

    public synchronized List<AppRuntimeSnapshot> getAllSnapshots() {
        return countersByAppIdentifier.entrySet().stream()
                .map(entry -> new AppRuntimeSnapshot(
                        entry.getKey(),
                        entry.getValue().getDisplayName(),
                        entry.getValue().getCurrentValueSeconds()
                ))
                .collect(Collectors.toList());
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
