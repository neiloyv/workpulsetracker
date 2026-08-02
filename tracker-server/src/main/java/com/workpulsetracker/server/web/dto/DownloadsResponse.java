package com.workpulsetracker.server.web.dto;

public record DownloadsResponse(
        String windowsUrl,
        boolean windowsAvailable,
        String macosUrl,
        boolean macosAvailable,
        String linuxUrl,
        boolean linuxAvailable
) {
}
