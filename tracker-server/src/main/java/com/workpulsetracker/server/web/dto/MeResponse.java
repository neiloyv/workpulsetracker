package com.workpulsetracker.server.web.dto;

public record MeResponse(
        Long id,
        String email,
        String displayName,
        String role,
        String organizationType,
        Long organizationId,
        String organizationName,
        Long workerId,
        String status
) {
}
