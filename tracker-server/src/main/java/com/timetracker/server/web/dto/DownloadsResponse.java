package com.timetracker.server.web.dto;

public record DownloadsResponse(
        String windowsUrl,
        String macosUrl,
        String linuxUrl
) {
}
