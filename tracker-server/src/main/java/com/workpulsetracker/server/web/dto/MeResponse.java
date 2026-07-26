package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        String firstName,
        String lastName,
        String phone,
        String role,
        String accountType,
        boolean onboarded,
        UUID organizationId,
        String organizationName,
        UUID branchId,
        UUID departmentId,
        boolean agentInstalled,
        String agentVersion
) {
}
