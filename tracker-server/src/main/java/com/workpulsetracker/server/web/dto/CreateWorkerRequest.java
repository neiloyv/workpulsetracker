package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkerRequest(
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Email @Size(max = 320) String email,
        Long branchId,
        Long departmentId
) {
}
