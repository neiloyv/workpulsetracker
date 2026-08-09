package com.workpulsetracker.agent.feedback;

import com.workpulsetracker.agent.api.AgentFeedbackClient;
import com.workpulsetracker.agent.config.AgentConfig;
import com.workpulsetracker.agent.storage.UserSettings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Отправка feedback: API в NETWORK_SYNC, mailto в LOCAL_SOLO.
 */
public final class FeedbackSubmitService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackSubmitService.class);
    private static final String schema = "local";
    private static final int MAX_MAILTO_BODY_LENGTH = 1800;

    private final AgentConfig agentConfig;
    private final AgentFeedbackClient agentFeedbackClient;

    public FeedbackSubmitService(AgentConfig agentConfig, AgentFeedbackClient agentFeedbackClient) {
        this.agentConfig = Objects.requireNonNull(agentConfig);
        this.agentFeedbackClient = Objects.requireNonNull(agentFeedbackClient);
    }

    public FeedbackSubmitResult submit(
            UserSettings userSettings,
            String replyEmail,
            FeedbackCategory feedbackCategory,
            String message,
            boolean includeSystemInfo,
            List<FeedbackAttachment> feedbackAttachments
    ) throws Exception {
        Objects.requireNonNull(userSettings);
        String trimmedReplyEmail = StringUtils.trimToEmpty(replyEmail);
        String trimmedMessage = StringUtils.trimToEmpty(message);
        String diagnosticsText = includeSystemInfo ? FeedbackDiagnosticsBuilder.build(userSettings) : null;

        if (userSettings.isServerSyncEnabled()) {
            agentFeedbackClient.submit(
                    userSettings,
                    trimmedReplyEmail,
                    feedbackCategory,
                    trimmedMessage,
                    diagnosticsText,
                    feedbackAttachments
            );
            return FeedbackSubmitResult.apiSuccess();
        }

        Path attachmentsFolderPath = null;
        if (Objects.nonNull(feedbackAttachments) && !feedbackAttachments.isEmpty()) {
            attachmentsFolderPath = prepareLocalAttachmentsFolder(feedbackAttachments, diagnosticsText);
        } else if (StringUtils.isNotBlank(diagnosticsText)) {
            attachmentsFolderPath = prepareLocalAttachmentsFolder(List.of(), diagnosticsText);
        }
        openMailto(trimmedReplyEmail, feedbackCategory, trimmedMessage, diagnosticsText, attachmentsFolderPath);
        return FeedbackSubmitResult.mailtoOpened(attachmentsFolderPath);
    }

    private Path prepareLocalAttachmentsFolder(
            List<FeedbackAttachment> feedbackAttachments,
            String diagnosticsText
    ) throws Exception {
        Path folderPath = Files.createTempDirectory("workpulse-feedback-");
        if (StringUtils.isNotBlank(diagnosticsText)) {
            Files.writeString(folderPath.resolve("diagnostics.txt"), diagnosticsText, StandardCharsets.UTF_8);
        }
        for (FeedbackAttachment feedbackAttachment : feedbackAttachments) {
            Path targetPath = folderPath.resolve(feedbackAttachment.getFileName());
            Files.copy(feedbackAttachment.getFilePath(), targetPath);
        }
        return folderPath;
    }

    private void openMailto(
            String replyEmail,
            FeedbackCategory feedbackCategory,
            String message,
            String diagnosticsText,
            Path attachmentsFolderPath
    ) throws Exception {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            throw new IllegalStateException("Desktop mail is not supported on this system");
        }
        String subject = "[WorkPulseTracker] " + feedbackCategory.name() + " from " + replyEmail;
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("From: ").append(replyEmail).append("\n\n");
        bodyBuilder.append(message).append("\n\n");
        if (StringUtils.isNotBlank(diagnosticsText)) {
            String truncatedDiagnostics = diagnosticsText.length() > MAX_MAILTO_BODY_LENGTH
                    ? diagnosticsText.substring(0, MAX_MAILTO_BODY_LENGTH) + "\n...(truncated, see diagnostics.txt)"
                    : diagnosticsText;
            bodyBuilder.append("--- diagnostics ---\n").append(truncatedDiagnostics).append('\n');
        }
        if (Objects.nonNull(attachmentsFolderPath)) {
            bodyBuilder.append("\nPlease attach files from: ").append(attachmentsFolderPath).append('\n');
            try {
                Desktop.getDesktop().open(attachmentsFolderPath.toFile());
            } catch (Exception exception) {
                logger.warn("schema={} Failed to open attachments folder: {}", schema, exception.getMessage());
            }
        }
        String mailtoUri = "mailto:" + agentConfig.getSupportEmail()
                + "?subject=" + urlEncode(subject)
                + "&body=" + urlEncode(bodyBuilder.toString());
        Desktop.getDesktop().mail(URI.create(mailtoUri));
        logger.info(
                "schema={} Feedback mailto opened category={} attachmentsFolder={}",
                schema,
                feedbackCategory,
                Objects.nonNull(attachmentsFolderPath) ? attachmentsFolderPath : "-"
        );
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record FeedbackSubmitResult(boolean apiSubmitted, Path attachmentsFolderPath) {
        public static FeedbackSubmitResult apiSuccess() {
            return new FeedbackSubmitResult(true, null);
        }

        public static FeedbackSubmitResult mailtoOpened(Path attachmentsFolderPath) {
            return new FeedbackSubmitResult(false, attachmentsFolderPath);
        }

        public String describeAttachmentsHint() {
            if (Objects.isNull(attachmentsFolderPath)) {
                return "";
            }
            return attachmentsFolderPath.toAbsolutePath().toString();
        }
    }
}
