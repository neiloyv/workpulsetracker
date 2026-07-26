package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String agentKey,
        String agentKeyPrefix
) {
}
