package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;

public record ManagerResponse(
        Long id,
        String displayName,
        String email,
        String role,
        String status,
        OffsetDateTime createdAt
) {
}
