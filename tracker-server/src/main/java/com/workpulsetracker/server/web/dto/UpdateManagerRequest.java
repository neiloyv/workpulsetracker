package com.workpulsetracker.server.web.dto;

import com.workpulsetracker.server.domain.EntityStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateManagerRequest(
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(min = 8, max = 128) String password,
        EntityStatus status
) {
}
