package com.workpulsetracker.agent.feedback;

import com.workpulsetracker.agent.storage.LocalDataDirectory;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.util.AgentVersion;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Собирает анонимный диагностический текст (версия, ОС, режим, хвост логов).
 */
public final class FeedbackDiagnosticsBuilder {

    private static final long MAX_LOG_BYTES = 64L * 1024L;

    private FeedbackDiagnosticsBuilder() {
    }

    public static String build(UserSettings userSettings) {
        Objects.requireNonNull(userSettings);
        StringBuilder diagnosticsBuilder = new StringBuilder();
        diagnosticsBuilder.append("WorkPulseTracker diagnostics").append('\n');
        diagnosticsBuilder.append("agentVersion=").append(AgentVersion.get()).append('\n');
        diagnosticsBuilder.append("os.name=").append(System.getProperty("os.name")).append('\n');
        diagnosticsBuilder.append("os.arch=").append(System.getProperty("os.arch")).append('\n');
        diagnosticsBuilder.append("os.version=").append(System.getProperty("os.version")).append('\n');
        diagnosticsBuilder.append("java.version=").append(System.getProperty("java.version")).append('\n');
        diagnosticsBuilder.append("operationMode=").append(userSettings.getOperationMode().name()).append('\n');
        diagnosticsBuilder.append("language=").append(userSettings.getLanguage().getCode()).append('\n');
        if (Objects.nonNull(userSettings.getDeviceId())) {
            diagnosticsBuilder.append("deviceId=").append(userSettings.getDeviceId()).append('\n');
        }
        if (Objects.nonNull(userSettings.getWorkerId())) {
            diagnosticsBuilder.append("workerId=").append(userSettings.getWorkerId()).append('\n');
        }
        Path logFilePath = LocalDataDirectory.getRootPath().resolve("logs").resolve("agent.log");
        diagnosticsBuilder.append("logFile=").append(logFilePath).append('\n');
        diagnosticsBuilder.append("--- log tail ---").append('\n');
        diagnosticsBuilder.append(readLogTail(logFilePath));
        return diagnosticsBuilder.toString();
    }

    private static String readLogTail(Path logFilePath) {
        if (!Files.isRegularFile(logFilePath)) {
            return "(log file not found; console-only logging may be active)\n";
        }
        try {
            long fileSizeBytes = Files.size(logFilePath);
            if (fileSizeBytes <= 0L) {
                return "(empty log file)\n";
            }
            long startOffset = Math.max(0L, fileSizeBytes - MAX_LOG_BYTES);
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(logFilePath.toFile(), "r")) {
                randomAccessFile.seek(startOffset);
                byte[] buffer = new byte[(int) Math.min(MAX_LOG_BYTES, fileSizeBytes - startOffset)];
                int bytesRead = randomAccessFile.read(buffer);
                if (bytesRead <= 0) {
                    return "(empty log tail)\n";
                }
                String tailText = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                if (startOffset > 0L) {
                    int firstLineBreak = tailText.indexOf('\n');
                    if (firstLineBreak >= 0 && firstLineBreak + 1 < tailText.length()) {
                        tailText = tailText.substring(firstLineBreak + 1);
                    }
                }
                return tailText.endsWith("\n") ? tailText : tailText + "\n";
            }
        } catch (IOException exception) {
            return "(failed to read log file: " + exception.getMessage() + ")\n";
        }
    }
}
