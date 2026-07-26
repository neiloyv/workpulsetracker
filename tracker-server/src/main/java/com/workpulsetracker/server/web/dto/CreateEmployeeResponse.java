package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record CreateEmployeeResponse(
        UUID id,
        String displayName,
        String email,
        String phone,
        String role,
        UUID branchId,
        UUID departmentId,
        String agentKey,
        String agentKeyPrefix,
        String temporaryPassword
) {
}
