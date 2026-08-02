package com.workpulsetracker.server.web.dto;

public record OrganizationResponse(
        Long id,
        String name,
        String organizationType,
        String status
) {
}
