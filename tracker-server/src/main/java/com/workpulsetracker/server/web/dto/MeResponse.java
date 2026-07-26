package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean onboarded,
        UUID organizationId,
        String organizationName
) {
}
