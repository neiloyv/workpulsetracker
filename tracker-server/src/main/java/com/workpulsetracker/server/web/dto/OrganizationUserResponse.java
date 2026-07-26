package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean onboarded,
        String agentKeyPrefix,
        OffsetDateTime createdAt
) {
}
