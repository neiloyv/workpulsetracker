package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 64) String phone,
        UUID branchId,
        UUID departmentId,
        @Size(min = 8, max = 128) String password
) {
}
