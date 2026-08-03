package com.workpulsetracker.agent.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.workpulsetracker.agent.util.HardwareIdProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Клиент проверки доступа агента и pairing устройства через tracker-server.
 */
public final class AgentAccessClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentAccessClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final String serverBaseUrl;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AgentAccessClient(String serverBaseUrl) {
        this.serverBaseUrl = Objects.requireNonNull(serverBaseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     * Проверяет, что для указанного email существует access key на веб-сервисе,
     * и одновременно выполняет pairing устройства.
     *
     * @param email     email пользователя
     * @param accessKey access key (роль пароля для агента)
     * @return {@code true}, если доступ разрешён
     */
    public boolean validateAccess(String email, String accessKey) {
        try {
            AgentAuthResult agentAuthResult = authenticate(email, accessKey, null, null);
            return Objects.nonNull(agentAuthResult) && StringUtils.isNotBlank(agentAuthResult.accessToken());
        } catch (Exception exception) {
            logger.warn("validateAccess failed: {}", exception.getMessage());
            return false;
        }
    }

    public AgentAuthResult authenticate(
            String email,
            String accessKey,
            String deviceDisplayName,
            String agentVersion
    ) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(accessKey);
        String hardwareId = HardwareIdProvider.resolveHardwareId();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("email", email.trim().toLowerCase());
        requestBody.put("accessKey", accessKey.trim());
        requestBody.put("hardwareId", hardwareId);
        if (StringUtils.isNotBlank(deviceDisplayName)) {
            requestBody.put("deviceDisplayName", deviceDisplayName.trim());
        }
        if (StringUtils.isNotBlank(agentVersion)) {
            requestBody.put("agentVersion", agentVersion.trim());
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/agent/auth"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                logger.info(
                        "Agent auth rejected: status={}, body={}",
                        httpResponse.statusCode(),
                        httpResponse.body()
                );
                return null;
            }
            JsonObject jsonObject = gson.fromJson(httpResponse.body(), JsonObject.class);
            if (Objects.isNull(jsonObject) || !jsonObject.has("accessToken")) {
                return null;
            }
            String accessToken = jsonObject.get("accessToken").getAsString();
            Long workerId = jsonObject.has("workerId") ? jsonObject.get("workerId").getAsLong() : null;
            Long deviceId = jsonObject.has("deviceId") ? jsonObject.get("deviceId").getAsLong() : null;
            String responseHardwareId = jsonObject.has("hardwareId")
                    ? jsonObject.get("hardwareId").getAsString()
                    : hardwareId;
            logger.info("Agent auth success: workerId={}, deviceId={}", workerId, deviceId);
            return new AgentAuthResult(accessToken, responseHardwareId, workerId, deviceId);
        } catch (Exception exception) {
            logger.warn("Agent auth request failed: {}", exception.getMessage());
            return null;
        }
    }

    public record AgentAuthResult(
            String accessToken,
            String hardwareId,
            Long workerId,
            Long deviceId
    ) {
    }
}
