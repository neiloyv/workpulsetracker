package com.workpulsetracker.server.web.dto;

public record DashboardWorkerResponse(
        Long id,
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
