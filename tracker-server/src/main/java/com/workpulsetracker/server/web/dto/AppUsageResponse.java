package com.workpulsetracker.server.web.dto;

public record AppUsageResponse(
        String appName,
        long seconds,
        boolean idle,
        double percent
) {
}
