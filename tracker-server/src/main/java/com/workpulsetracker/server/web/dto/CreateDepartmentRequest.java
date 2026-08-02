package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotNull Long branchId,
        @NotBlank @Size(max = 255) String name
) {
}
