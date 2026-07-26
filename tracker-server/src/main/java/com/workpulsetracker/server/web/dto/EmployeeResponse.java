package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String displayName,
        String email,
        String phone,
        String role,
        UUID branchId,
        String branchName,
        UUID departmentId,
        String departmentName,
        boolean agentInstalled,
        String agentVersion,
        String agentKeyPrefix,
        OffsetDateTime createdAt
) {
}
