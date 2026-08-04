package com.workpulsetracker.agent.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.workpulsetracker.agent.storage.LocalAppRuntimeStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Клиент телеметрии и reverse sync с tracker-server.
 * При сетевых ошибках счётчики не сбрасываются — retry на следующем цикле.
 */
public final class AgentSyncClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentSyncClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final String serverBaseUrl;
    private final AgentAccessClient agentAccessClient;
    private final LocalAppRuntimeStore localAppRuntimeStore;
    private final UserSettingsStore userSettingsStore;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AgentSyncClient(
            String serverBaseUrl,
            AgentAccessClient agentAccessClient,
            LocalAppRuntimeStore localAppRuntimeStore,
            UserSettingsStore userSettingsStore
    ) {
        this.serverBaseUrl = Objects.requireNonNull(serverBaseUrl);
        this.agentAccessClient = Objects.requireNonNull(agentAccessClient);
        this.localAppRuntimeStore = Objects.requireNonNull(localAppRuntimeStore);
        this.userSettingsStore = Objects.requireNonNull(userSettingsStore);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public boolean isSyncConfigured(UserSettings userSettings) {
        return Objects.nonNull(userSettings)
                && userSettings.getOperationMode().isNetworkSync()
                && userSettings.isServerSyncEnabled();
    }

    /**
     * Отправляет batch текущих local counters на сервер (server считает delta).
     */
    public boolean uploadTelemetry(UserSettings userSettings) {
        Objects.requireNonNull(userSettings);
        if (userSettings.getOperationMode().isLocalSolo() || !isSyncConfigured(userSettings)) {
            return false;
        }
        List<LocalAppRuntimeStore.AppRuntimeSnapshot> pendingSnapshots =
                localAppRuntimeStore.snapshotPendingUpload();
        if (pendingSnapshots.isEmpty()) {
            return true;
        }

        ensureAccessToken(userSettings);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        List<Map<String, Object>> appsPayload = pendingSnapshots.stream()
                .map(snapshot -> {
                    Map<String, Object> appPayload = new LinkedHashMap<>();
                    appPayload.put("appIdentifier", snapshot.appIdentifier());
                    appPayload.put("displayName", snapshot.displayName());
                    appPayload.put("currentValueSeconds", snapshot.currentValueSeconds());
                    return appPayload;
                })
                .collect(Collectors.toList());
        requestBody.put("apps", appsPayload);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serverBaseUrl + "/api/agent/telemetry"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + userSettings.getAccessToken())
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(
                        httpRequest,
                        HttpResponse.BodyHandlers.ofString()
                );
                if (httpResponse.statusCode() == 401) {
                    userSettings.setAccessToken(null);
                    userSettingsStore.save(userSettings);
                    ensureAccessToken(userSettings);
                    continue;
                }
                if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "Telemetry upload failed with status " + httpResponse.statusCode()
                    );
                }
                List<String> syncedAppIdentifiers = pendingSnapshots.stream()
                        .map(LocalAppRuntimeStore.AppRuntimeSnapshot::appIdentifier)
                        .collect(Collectors.toList());
                localAppRuntimeStore.markSynced(syncedAppIdentifiers);
                logger.info(
                        "Telemetry uploaded: apps={}, attempt={}",
                        syncedAppIdentifiers.size(),
                        attempt
                );
                return true;
            } catch (Exception exception) {
                lastException = exception;
                logger.warn(
                        "Telemetry upload attempt {}/{} failed: {}",
                        attempt,
                        MAX_RETRY_ATTEMPTS,
                        exception.getMessage()
                );
                sleepQuietly(attempt * 500L);
            }
        }
        if (Objects.nonNull(lastException)) {
            logger.warn("Telemetry upload exhausted retries; counters kept for next cycle");
        }
        return false;
    }

    /**
     * Reverse sync: забирает агрегированные totals аккаунта и восстанавливает локальные counters.
     */
    public List<LocalAppRuntimeStore.AppRuntimeSnapshot> downloadAccountTotals(UserSettings userSettings) {
        return downloadAccountTotals(userSettings, true);
    }

    private List<LocalAppRuntimeStore.AppRuntimeSnapshot> downloadAccountTotals(
            UserSettings userSettings,
            boolean allowTokenRefresh
    ) {
        Objects.requireNonNull(userSettings);
        if (userSettings.getOperationMode().isLocalSolo() || !isSyncConfigured(userSettings)) {
            return List.of();
        }
        ensureAccessToken(userSettings);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/agent/sync?scope=account"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + userSettings.getAccessToken())
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 401 && allowTokenRefresh) {
                userSettings.setAccessToken(null);
                userSettingsStore.save(userSettings);
                ensureAccessToken(userSettings);
                return downloadAccountTotals(userSettings, false);
            }
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new IllegalStateException("Sync download failed with status " + httpResponse.statusCode());
            }
            JsonObject jsonObject = gson.fromJson(httpResponse.body(), JsonObject.class);
            JsonArray appsJsonArray = Objects.nonNull(jsonObject) && jsonObject.has("apps")
                    ? jsonObject.getAsJsonArray("apps")
                    : new JsonArray();
            List<LocalAppRuntimeStore.AppRuntimeSnapshot> snapshots = StreamSupport
                    .stream(appsJsonArray.spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .map(appJsonObject -> new LocalAppRuntimeStore.AppRuntimeSnapshot(
                            appJsonObject.get("appIdentifier").getAsString(),
                            appJsonObject.has("displayName")
                                    ? appJsonObject.get("displayName").getAsString()
                                    : appJsonObject.get("appIdentifier").getAsString(),
                            appJsonObject.has("totalSeconds")
                                    ? appJsonObject.get("totalSeconds").getAsLong()
                                    : 0L
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));
            localAppRuntimeStore.restoreFromServerTotals(snapshots);
            logger.info("Reverse sync restored {} apps", snapshots.size());
            return snapshots;
        } catch (IllegalStateException illegalStateException) {
            throw illegalStateException;
        } catch (Exception exception) {
            logger.warn("Reverse sync failed: {}", exception.getMessage());
            throw new IllegalStateException("Reverse sync failed: " + exception.getMessage(), exception);
        }
    }

    /**
     * Полный цикл: upload telemetry + reverse sync restore.
     */
    public void synchronize(UserSettings userSettings) {
        Objects.requireNonNull(userSettings);
        if (userSettings.getOperationMode().isLocalSolo() || !isSyncConfigured(userSettings)) {
            throw new IllegalStateException("Server sync is not configured");
        }
        uploadTelemetry(userSettings);
        downloadAccountTotals(userSettings);
    }

    private void ensureAccessToken(UserSettings userSettings) {
        if (userSettings.getOperationMode().isLocalSolo()) {
            throw new IllegalStateException("JWT refresh is disabled in LOCAL_SOLO mode");
        }
        if (userSettings.hasValidAccessToken()) {
            return;
        }
        AgentAccessClient.AgentAuthResult agentAuthResult = agentAccessClient.authenticate(
                userSettings.getEmail(),
                userSettings.getActivationKey(),
                System.getenv("COMPUTERNAME"),
                null
        );
        if (Objects.isNull(agentAuthResult) || StringUtils.isBlank(agentAuthResult.accessToken())) {
            throw new IllegalStateException("Unable to obtain agent access token");
        }
        userSettings.applyAgentAuth(
                agentAuthResult.accessToken(),
                agentAuthResult.hardwareId(),
                agentAuthResult.workerId(),
                agentAuthResult.deviceId()
        );
        userSettingsStore.save(userSettings);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
