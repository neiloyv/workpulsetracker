package com.workpulsetracker.server.web.dto;

import java.util.List;

public record AgentSyncResponse(
        Long workerId,
        Long deviceId,
        boolean accountTotals,
        long totalSeconds,
        List<AppRuntimeItem> apps
) {

    public record AppRuntimeItem(
            String appIdentifier,
            String displayName,
            long totalSeconds
    ) {
    }
}
