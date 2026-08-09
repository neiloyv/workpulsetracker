package com.workpulsetracker.server.service;

import com.workpulsetracker.server.security.AgentDevicePrincipal;
import com.workpulsetracker.server.web.dto.AgentFeedbackRequest;
import com.workpulsetracker.server.web.dto.AgentFeedbackResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Приём feedback от агента: лог + сохранение вложений на диск (письмо — stub, как AccessKeyEmailService).
 */
@Service
public class AgentFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(AgentFeedbackService.class);
    private static final String schema = "public";

    private final String supportEmail;
    private final Path feedbackStorageDirectoryPath;

    public AgentFeedbackService(
            @Value("${app.support.email:workpulsetracker@gmail.com}") String supportEmail,
            @Value("${app.feedback.storage-dir:}") String feedbackStorageDirectory
    ) {
        this.supportEmail = StringUtils.defaultIfBlank(supportEmail, "workpulsetracker@gmail.com").trim().toLowerCase();
        this.feedbackStorageDirectoryPath = StringUtils.isNotBlank(feedbackStorageDirectory)
                ? Path.of(feedbackStorageDirectory.trim())
                : Path.of(System.getProperty("user.home"), ".workpulsetracker-server", "feedback");
    }

    public AgentFeedbackResponse accept(
            AgentDevicePrincipal agentDevicePrincipal,
            AgentFeedbackRequest agentFeedbackRequest
    ) {
        Objects.requireNonNull(agentDevicePrincipal);
        Objects.requireNonNull(agentFeedbackRequest);
        int attachmentCount = Objects.requireNonNullElse(agentFeedbackRequest.attachments(), List.of()).size();
        logger.info(
                "schema={} Feedback received supportEmail={} replyEmail={} category={} workerId={} deviceId={} agentVersion={} attachments={}",
                schema,
                supportEmail,
                agentFeedbackRequest.replyEmail(),
                agentFeedbackRequest.category(),
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId(),
                agentFeedbackRequest.agentVersion(),
                attachmentCount
        );
        persistFeedbackPackage(agentDevicePrincipal, agentFeedbackRequest);
        return new AgentFeedbackResponse(true);
    }

    private void persistFeedbackPackage(
            AgentDevicePrincipal agentDevicePrincipal,
            AgentFeedbackRequest agentFeedbackRequest
    ) {
        try {
            Files.createDirectories(feedbackStorageDirectoryPath);
            String packageId = Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
            Path packageDirectoryPath = feedbackStorageDirectoryPath.resolve(packageId);
            Files.createDirectories(packageDirectoryPath);
            String metaText = """
                    supportEmail=%s
                    replyEmail=%s
                    category=%s
                    workerId=%s
                    deviceId=%s
                    agentVersion=%s
                    message=
                    %s
                    
                    diagnostics=
                    %s
                    """.formatted(
                    supportEmail,
                    agentFeedbackRequest.replyEmail(),
                    agentFeedbackRequest.category(),
                    agentDevicePrincipal.getWorkerId(),
                    agentDevicePrincipal.getDeviceId(),
                    Objects.requireNonNullElse(agentFeedbackRequest.agentVersion(), ""),
                    agentFeedbackRequest.message(),
                    Objects.requireNonNullElse(agentFeedbackRequest.diagnosticsText(), "")
            );
            Files.writeString(packageDirectoryPath.resolve("feedback.txt"), metaText);
            List<AgentFeedbackRequest.Attachment> attachments =
                    Objects.requireNonNullElse(agentFeedbackRequest.attachments(), List.of());
            for (AgentFeedbackRequest.Attachment attachment : attachments) {
                byte[] fileBytes = Base64.getDecoder().decode(attachment.base64Content());
                String safeFileName = Path.of(attachment.fileName()).getFileName().toString();
                Files.write(packageDirectoryPath.resolve(safeFileName), fileBytes);
            }
            logger.info("schema={} Feedback package stored path={}", schema, packageDirectoryPath);
        } catch (Exception exception) {
            logger.error("schema={} Failed to persist feedback package: {}", schema, exception.getMessage(), exception);
        }
    }
}
