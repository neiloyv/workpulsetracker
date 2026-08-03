package com.workpulsetracker.server.web.dto;

import java.util.List;

public record TelemetryIngestResponse(
        int processedCount,
        long totalDeltaSeconds,
        List<AppDeltaResult> results
) {

    public record AppDeltaResult(
            String appIdentifier,
            long deltaSeconds,
            long totalSeconds,
            long lastAgentValue
    ) {
    }
}
