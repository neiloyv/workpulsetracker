package com.workpulsetracker.server.web.dto;

public record AgentInfoResponse(
        Long workerId,
        String displayName,
        String email,
        String status,
        String accessKeyPrefix,
        boolean agentInstalled,
        String agentVersion
) {
}
