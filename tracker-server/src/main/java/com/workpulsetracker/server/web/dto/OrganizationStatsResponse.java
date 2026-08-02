package com.workpulsetracker.server.web.dto;

import java.util.List;

public record OrganizationStatsResponse(
        long totalWorkers,
        long activeWorkers,
        List<WorkerStatItem> workers
) {
    public record WorkerStatItem(
            String email,
            String displayName,
            long trackedSeconds
    ) {
    }
}
