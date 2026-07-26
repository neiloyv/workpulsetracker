package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDepartmentRequest(
        @NotNull UUID branchId,
        @NotBlank @Size(max = 255) String name
) {
}
