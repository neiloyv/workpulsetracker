package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record DashboardWorkerResponse(
        UUID id,
        String displayName,
        String email,
        String departmentName,
        String branchName,
        long todaySeconds,
        long weekSeconds,
        long monthSeconds,
        long yearSeconds,
        boolean agentInstalled,
        String agentVersion
) {
}
