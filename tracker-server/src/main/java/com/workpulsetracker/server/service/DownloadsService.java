package com.workpulsetracker.server.service;

import com.workpulsetracker.server.config.AppProperties;
import com.workpulsetracker.server.web.dto.DownloadsResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class DownloadsService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadsService.class);
    private static final String schema = "public";

    private final AppProperties appProperties;

    public DownloadsService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public DownloadsResponse getDownloads() {
        AppProperties.Download download = appProperties.getDownload();
        Path downloadsDirectory = Path.of(download.getDirectory()).toAbsolutePath().normalize();

        boolean windowsAvailable = isFileAvailable(downloadsDirectory, download.getWindowsFileName());
        boolean macosAvailable = isFileAvailable(downloadsDirectory, download.getMacosFileName());
        boolean linuxAvailable = isFileAvailable(downloadsDirectory, download.getLinuxFileName());

        logger.info(
                "schema={} Downloads availability windows={} macos={} linux={} dir={}",
                schema,
                windowsAvailable,
                macosAvailable,
                linuxAvailable,
                downloadsDirectory
        );

        return new DownloadsResponse(
                download.getWindowsUrl(),
                windowsAvailable,
                download.getMacosUrl(),
                macosAvailable,
                download.getLinuxUrl(),
                linuxAvailable
        );
    }

    private static boolean isFileAvailable(Path downloadsDirectory, String fileName) {
        if (Objects.isNull(downloadsDirectory) || StringUtils.isBlank(fileName)) {
            return false;
        }
        Path filePath = downloadsDirectory.resolve(fileName).normalize();
        if (!filePath.startsWith(downloadsDirectory)) {
            return false;
        }
        return Files.isRegularFile(filePath) && Files.isReadable(filePath);
    }
}
