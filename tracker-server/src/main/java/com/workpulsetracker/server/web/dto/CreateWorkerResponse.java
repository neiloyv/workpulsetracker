package com.workpulsetracker.server.web.dto;

import java.time.OffsetDateTime;

public record CreateWorkerResponse(
        Long id,
        String displayName,
        String email,
        Long branchId,
        Long departmentId,
        String status,
        boolean accessKeySent,
        OffsetDateTime createdAt
) {
}
