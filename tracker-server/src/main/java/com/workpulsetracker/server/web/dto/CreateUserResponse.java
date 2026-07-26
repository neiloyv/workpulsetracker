package com.workpulsetracker.server.web.dto;

import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        String agentKey,
        String agentKeyPrefix
) {
}
