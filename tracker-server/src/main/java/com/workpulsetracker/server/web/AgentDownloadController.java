package com.workpulsetracker.server.web;

import com.workpulsetracker.server.config.AppProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Раздача установочных файлов агента из локальной папки {@code app.download.directory}.
 */
@RestController
public class AgentDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(AgentDownloadController.class);
    private static final String schema = "public";
    private static final Set<String> ALLOWED_EXTENSIONS = Stream.of(
                    ".msi",
                    ".exe",
                    ".dmg",
                    ".pkg",
                    ".deb",
                    ".rpm",
                    ".appimage",
                    ".zip"
            )
            .collect(Collectors.toUnmodifiableSet());

    private final Path downloadDirectoryPath;

    public AgentDownloadController(AppProperties appProperties) {
        this.downloadDirectoryPath = Path.of(appProperties.getDownload().getDirectory())
                .toAbsolutePath()
                .normalize();
        logger.info(
                "schema={} Agent download directory configured: {}",
                schema,
                downloadDirectoryPath
        );
    }

    @GetMapping("/downloads/{fileName}")
    public ResponseEntity<Resource> downloadAgentInstaller(@PathVariable("fileName") String fileName)
            throws IOException {
        if (StringUtils.isBlank(fileName) || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            logger.warn("schema={} Rejected download request with unsafe fileName={}", schema, fileName);
            return ResponseEntity.badRequest().build();
        }
        if (!hasAllowedExtension(fileName)) {
            logger.warn("schema={} Rejected download request with unsupported extension: {}", schema, fileName);
            return ResponseEntity.badRequest().build();
        }

        Path installerFilePath = downloadDirectoryPath.resolve(fileName).normalize();
        if (!installerFilePath.startsWith(downloadDirectoryPath) || !Files.isRegularFile(installerFilePath)) {
            logger.warn(
                    "schema={} Download file not found: fileName={}, directory={}",
                    schema,
                    fileName,
                    downloadDirectoryPath
            );
            return ResponseEntity.notFound().build();
        }

        FileSystemResource fileSystemResource = new FileSystemResource(installerFilePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileSystemResource.contentLength())
                .body(fileSystemResource);
    }

    private static boolean hasAllowedExtension(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }
}
