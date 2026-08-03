package com.workpulsetracker.server.web.dto;

public record AgentAuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long workerId,
        Long deviceId,
        String hardwareId
) {
}
