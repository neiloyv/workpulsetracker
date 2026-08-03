package com.workpulsetracker.server.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TelemetryIngestRequest(
        @NotEmpty @Valid List<AppActivitySample> apps
) {

    public record AppActivitySample(
            @NotBlank @Size(max = 512) String appIdentifier,
            @Size(max = 512) String displayName,
            @NotNull @PositiveOrZero Long currentValueSeconds
    ) {
    }
}
