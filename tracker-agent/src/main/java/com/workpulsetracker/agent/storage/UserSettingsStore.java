package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Чтение/запись {@link UserSettings} в {@code settings.json}.
 */
public final class UserSettingsStore {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsStore.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path settingsFilePath;

    public UserSettingsStore() {
        this(LocalDataDirectory.getSettingsFilePath());
    }

    public UserSettingsStore(Path settingsFilePath) {
        this.settingsFilePath = settingsFilePath;
    }

    public UserSettings loadOrCreateDefault() {
        try {
            Files.createDirectories(settingsFilePath.getParent());
            if (!Files.exists(settingsFilePath)) {
                UserSettings userSettings = new UserSettings();
                save(userSettings);
                return userSettings;
            }
            try (Reader reader = Files.newBufferedReader(settingsFilePath, StandardCharsets.UTF_8)) {
                UserSettings userSettings = gson.fromJson(reader, UserSettings.class);
                if (Objects.isNull(userSettings)) {
                    return new UserSettings();
                }
                return userSettings;
            }
        } catch (IOException exception) {
            logger.warn("Failed to read settings.json: {}", exception.getMessage());
            return new UserSettings();
        }
    }

    public void save(UserSettings userSettings) {
        try {
            Files.createDirectories(settingsFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(settingsFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(userSettings, writer);
            }
        } catch (IOException exception) {
            logger.error("Failed to save settings.json: {}", exception.getMessage(), exception);
        }
    }
}
