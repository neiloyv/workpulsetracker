package com.workpulsetracker.server.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentFeedbackRequest(
        @NotBlank @Email @Size(max = 320) String replyEmail,
        @NotBlank @Size(max = 32) String category,
        @NotBlank @Size(max = 10000) String message,
        @Size(max = 64) String agentVersion,
        @Size(max = 100000) String diagnosticsText,
        @Valid List<Attachment> attachments
) {
    public record Attachment(
            @NotBlank @Size(max = 255) String fileName,
            @Size(max = 128) String contentType,
            @NotBlank @Size(max = 8_000_000) String base64Content
    ) {
    }
}
