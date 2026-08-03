package com.workpulsetracker.agent.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Каталог локальных данных агента: {@code ~/.workpulsetracker/}.
 */
public final class LocalDataDirectory {

    private static final String DIRECTORY_NAME = ".workpulsetracker";

    private LocalDataDirectory() {
    }

    public static Path getRootPath() {
        return Paths.get(System.getProperty("user.home"), DIRECTORY_NAME);
    }

    public static Path getSettingsFilePath() {
        return getRootPath().resolve("settings.json");
    }

    public static Path getIntervalsFilePath() {
        return getRootPath().resolve("intervals.json");
    }

    public static Path getExecutablePathsFilePath() {
        return getRootPath().resolve("executable-paths.json");
    }

    public static Path getAppRuntimeCountersFilePath() {
        return getRootPath().resolve("app-runtime-counters.json");
    }
}
