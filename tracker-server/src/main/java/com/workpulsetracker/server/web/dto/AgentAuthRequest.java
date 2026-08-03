package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentAuthRequest(
        @NotBlank @Email String email,
        @NotBlank String accessKey,
        @NotBlank @Size(max = 255) String hardwareId,
        @Size(max = 255) String deviceDisplayName,
        @Size(max = 32) String agentVersion
) {
}
