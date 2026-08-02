package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;

public record WorkerResponse(
        Long id,
        String displayName,
        String email,
        Long branchId,
        String branchName,
        Long departmentId,
        String departmentName,
        String status,
        boolean agentInstalled,
        String agentVersion,
        String accessKeyPrefix,
        OffsetDateTime createdAt
) {
}
