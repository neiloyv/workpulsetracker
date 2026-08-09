package com.workpulsetracker.agent.feedback;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Вложение обратной связи (изображение или лог).
 */
public final class FeedbackAttachment {

    private final Path filePath;
    private final String fileName;
    private final long fileSizeBytes;
    private final String contentType;
    private final boolean image;

    public FeedbackAttachment(
            Path filePath,
            String fileName,
            long fileSizeBytes,
            String contentType,
            boolean image
    ) {
        this.filePath = Objects.requireNonNull(filePath);
        this.fileName = StringUtils.defaultIfBlank(fileName, filePath.getFileName().toString());
        this.fileSizeBytes = Math.max(fileSizeBytes, 0L);
        this.contentType = StringUtils.defaultIfBlank(contentType, "application/octet-stream");
        this.image = image;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isImage() {
        return image;
    }
}
