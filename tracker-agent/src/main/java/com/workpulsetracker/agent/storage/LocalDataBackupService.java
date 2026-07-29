package com.workpulsetracker.agent.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Экспорт/импорт локальных данных агента в ZIP-архив.
 */
public final class LocalDataBackupService {

    public static final int BACKUP_FORMAT_VERSION = 1;
    private static final String MANIFEST_ENTRY_NAME = "manifest.json";
    private static final String SETTINGS_ENTRY_NAME = "settings.json";
    private static final String INTERVALS_ENTRY_NAME = "intervals.json";
    private static final String EXECUTABLE_PATHS_ENTRY_NAME = "executable-paths.json";

    private static final Logger logger = LoggerFactory.getLogger(LocalDataBackupService.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path settingsFilePath;
    private final Path intervalsFilePath;
    private final Path executablePathsFilePath;

    public LocalDataBackupService() {
        this(
                LocalDataDirectory.getSettingsFilePath(),
                LocalDataDirectory.getIntervalsFilePath(),
                LocalDataDirectory.getExecutablePathsFilePath()
        );
    }

    public LocalDataBackupService(
            Path settingsFilePath,
            Path intervalsFilePath,
            Path executablePathsFilePath
    ) {
        this.settingsFilePath = Objects.requireNonNull(settingsFilePath);
        this.intervalsFilePath = Objects.requireNonNull(intervalsFilePath);
        this.executablePathsFilePath = Objects.requireNonNull(executablePathsFilePath);
    }

    public void exportToZip(Path zipFilePath) throws IOException {
        Objects.requireNonNull(zipFilePath);
        Files.createDirectories(zipFilePath.getParent());
        try (OutputStream fileOutputStream = Files.newOutputStream(zipFilePath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            writeManifest(zipOutputStream);
            copyFileToZipIfExists(zipOutputStream, SETTINGS_ENTRY_NAME, settingsFilePath);
            copyFileToZipIfExists(zipOutputStream, INTERVALS_ENTRY_NAME, intervalsFilePath);
            copyFileToZipIfExists(zipOutputStream, EXECUTABLE_PATHS_ENTRY_NAME, executablePathsFilePath);
        }
        logger.info("Local backup exported to {}", zipFilePath);
    }

    public void importFromZip(Path zipFilePath) throws IOException {
        Objects.requireNonNull(zipFilePath);
        if (!Files.isRegularFile(zipFilePath)) {
            throw new IOException("Backup file does not exist: " + zipFilePath);
        }

        Path temporaryDirectory = Files.createTempDirectory("workpulse-backup-import-");
        try {
            extractZip(zipFilePath, temporaryDirectory);
            validateExtractedBackup(temporaryDirectory);

            Path dataRootPath = LocalDataDirectory.getRootPath();
            Files.createDirectories(dataRootPath);

            replaceFile(temporaryDirectory.resolve(SETTINGS_ENTRY_NAME), settingsFilePath);
            replaceOptionalFile(temporaryDirectory.resolve(INTERVALS_ENTRY_NAME), intervalsFilePath, "[]");
            replaceOptionalFile(temporaryDirectory.resolve(EXECUTABLE_PATHS_ENTRY_NAME), executablePathsFilePath, "{}");
            logger.info("Local backup imported from {}", zipFilePath);
        } finally {
            deleteDirectoryQuietly(temporaryDirectory);
        }
    }

    private void writeManifest(ZipOutputStream zipOutputStream) throws IOException {
        BackupManifest backupManifest = new BackupManifest(
                BACKUP_FORMAT_VERSION,
                Instant.now().toString(),
                "WorkPulseTracker"
        );
        zipOutputStream.putNextEntry(new ZipEntry(MANIFEST_ENTRY_NAME));
        zipOutputStream.write(gson.toJson(backupManifest).getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private void copyFileToZipIfExists(
            ZipOutputStream zipOutputStream,
            String entryName,
            Path filePath
    ) throws IOException {
        if (!Files.isRegularFile(filePath)) {
            return;
        }
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(filePath, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private static void extractZip(Path zipFilePath, Path targetDirectory) throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(zipFilePath);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry zipEntry;
            while (Objects.nonNull(zipEntry = zipInputStream.getNextEntry())) {
                String entryName = zipEntry.getName();
                if (StringUtils.isBlank(entryName) || entryName.contains("..") || entryName.contains("/") || entryName.contains("\\")) {
                    throw new IOException("Unsupported backup entry: " + entryName);
                }
                Path entryPath = targetDirectory.resolve(entryName).normalize();
                if (!entryPath.startsWith(targetDirectory)) {
                    throw new IOException("Invalid backup entry path: " + entryName);
                }
                if (zipEntry.isDirectory()) {
                    continue;
                }
                Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                zipInputStream.closeEntry();
            }
        }
    }

    private void validateExtractedBackup(Path temporaryDirectory) throws IOException {
        Path manifestPath = temporaryDirectory.resolve(MANIFEST_ENTRY_NAME);
        Path settingsPath = temporaryDirectory.resolve(SETTINGS_ENTRY_NAME);
        if (!Files.isRegularFile(manifestPath)) {
            throw new IOException("Backup does not contain manifest.json");
        }
        if (!Files.isRegularFile(settingsPath)) {
            throw new IOException("Backup does not contain settings.json");
        }
        try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            BackupManifest backupManifest = gson.fromJson(reader, BackupManifest.class);
            if (Objects.isNull(backupManifest)
                    || backupManifest.formatVersion() < 1
                    || backupManifest.formatVersion() > BACKUP_FORMAT_VERSION) {
                throw new IOException("Unsupported backup format version");
            }
        }
    }

    private static void replaceFile(Path sourceFilePath, Path targetFilePath) throws IOException {
        Files.createDirectories(targetFilePath.getParent());
        Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void replaceOptionalFile(
            Path sourceFilePath,
            Path targetFilePath,
            String emptyContent
    ) throws IOException {
        Files.createDirectories(targetFilePath.getParent());
        if (Files.isRegularFile(sourceFilePath)) {
            Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (Writer writer = Files.newBufferedWriter(targetFilePath, StandardCharsets.UTF_8)) {
            writer.write(emptyContent);
        }
    }

    private static void deleteDirectoryQuietly(Path directoryPath) {
        if (Objects.isNull(directoryPath) || !Files.exists(directoryPath)) {
            return;
        }
        try (Stream<Path> pathStream = Files.walk(directoryPath)) {
            pathStream
                    .sorted((leftPath, rightPath) -> rightPath.compareTo(leftPath))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best-effort cleanup
                        }
                    });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private record BackupManifest(
            @SerializedName("formatVersion") int formatVersion,
            @SerializedName("exportedAt") String exportedAt,
            @SerializedName("app") String app
    ) {
    }
}
