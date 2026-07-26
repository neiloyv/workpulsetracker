package com.workpulsetracker.server.web.dto;

public record DownloadsResponse(
        String windowsUrl,
        String macosUrl,
        String linuxUrl
) {
}
