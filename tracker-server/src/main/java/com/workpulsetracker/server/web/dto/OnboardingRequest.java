package com.workpulsetracker.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName
) {
}
