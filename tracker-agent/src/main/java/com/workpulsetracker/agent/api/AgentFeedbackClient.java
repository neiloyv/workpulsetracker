package com.workpulsetracker.agent.api;

import com.google.gson.Gson;
import com.workpulsetracker.agent.feedback.FeedbackAttachment;
import com.workpulsetracker.agent.feedback.FeedbackCategory;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.util.AgentVersion;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Отправка feedback на tracker-server (JWT).
 */
public final class AgentFeedbackClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentFeedbackClient.class);
    private static final String schema = "local";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final String serverBaseUrl;
    private final AgentAccessClient agentAccessClient;
    private final UserSettingsStore userSettingsStore;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AgentFeedbackClient(
            String serverBaseUrl,
            AgentAccessClient agentAccessClient,
            UserSettingsStore userSettingsStore
    ) {
        this.serverBaseUrl = Objects.requireNonNull(serverBaseUrl);
        this.agentAccessClient = Objects.requireNonNull(agentAccessClient);
        this.userSettingsStore = Objects.requireNonNull(userSettingsStore);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public void submit(
            UserSettings userSettings,
            String replyEmail,
            FeedbackCategory feedbackCategory,
            String message,
            String diagnosticsText,
            List<FeedbackAttachment> feedbackAttachments
    ) throws Exception {
        Objects.requireNonNull(userSettings);
        ensureAccessToken(userSettings);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("replyEmail", replyEmail);
        requestBody.put("category", feedbackCategory.name());
        requestBody.put("message", message);
        requestBody.put("agentVersion", AgentVersion.get());
        if (StringUtils.isNotBlank(diagnosticsText)) {
            requestBody.put("diagnosticsText", diagnosticsText);
        }
        List<Map<String, Object>> attachmentsPayload = Objects.requireNonNullElse(feedbackAttachments, List.<FeedbackAttachment>of())
                .stream()
                .map(this::toAttachmentPayload)
                .collect(Collectors.toCollection(ArrayList::new));
        requestBody.put("attachments", attachmentsPayload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/api/agent/feedback"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + userSettings.getAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == 401) {
            userSettings.setAccessToken(null);
            userSettingsStore.save(userSettings);
            ensureAccessToken(userSettings);
            HttpRequest retryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/agent/feedback"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + userSettings.getAccessToken())
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            httpResponse = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
        }
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new IllegalStateException("Feedback submit failed with status " + httpResponse.statusCode());
        }
        logger.info(
                "schema={} Feedback submitted category={} attachments={}",
                schema,
                feedbackCategory,
                attachmentsPayload.size()
        );
    }

    private Map<String, Object> toAttachmentPayload(FeedbackAttachment feedbackAttachment) {
        try {
            byte[] fileBytes = Files.readAllBytes(feedbackAttachment.getFilePath());
            Map<String, Object> attachmentPayload = new LinkedHashMap<>();
            attachmentPayload.put("fileName", feedbackAttachment.getFileName());
            attachmentPayload.put("contentType", feedbackAttachment.getContentType());
            attachmentPayload.put("base64Content", Base64.getEncoder().encodeToString(fileBytes));
            return attachmentPayload;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to read attachment: " + feedbackAttachment.getFileName(),
                    exception
            );
        }
    }

    private void ensureAccessToken(UserSettings userSettings) {
        if (StringUtils.isNotBlank(userSettings.getAccessToken())) {
            return;
        }
        AgentAccessClient.AgentAuthResult agentAuthResult = agentAccessClient.authenticate(
                userSettings.getEmail(),
                userSettings.getActivationKey(),
                null,
                AgentVersion.get()
        );
        userSettings.applyAgentAuth(
                agentAuthResult.accessToken(),
                agentAuthResult.hardwareId(),
                agentAuthResult.workerId(),
                agentAuthResult.deviceId()
        );
        userSettingsStore.save(userSettings);
    }
}
