package com.workpulsetracker.server.web.dto;

import com.workpulsetracker.server.enums.EntityStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkerRequest(
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Email @Size(max = 320) String email,
        Long branchId,
        Long departmentId,
        EntityStatus status
) {
}
