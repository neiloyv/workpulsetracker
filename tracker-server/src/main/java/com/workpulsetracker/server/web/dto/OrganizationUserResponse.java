package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationUserResponse(
        UUID id,
        String email,
        String displayName,
        String phone,
        String role,
        UUID branchId,
        UUID departmentId,
        boolean onboarded,
        String agentKeyPrefix,
        OffsetDateTime createdAt
) {
}
