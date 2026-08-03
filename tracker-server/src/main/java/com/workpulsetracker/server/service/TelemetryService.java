package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.ActivitySampleEntity;
import com.workpulsetracker.server.domain.AppCatalogEntity;
import com.workpulsetracker.server.domain.AppRuntimeStatEntity;
import com.workpulsetracker.server.domain.DeviceEntity;
import com.workpulsetracker.server.domain.EntityStatus;
import com.workpulsetracker.server.repository.ActivitySampleRepository;
import com.workpulsetracker.server.repository.AppCatalogRepository;
import com.workpulsetracker.server.repository.AppRuntimeStatRepository;
import com.workpulsetracker.server.repository.DeviceRepository;
import com.workpulsetracker.server.security.AgentDevicePrincipal;
import com.workpulsetracker.server.web.dto.TelemetryIngestRequest;
import com.workpulsetracker.server.web.dto.TelemetryIngestResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TelemetryService {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryService.class);
    private static final String schema = "public";

    private final DeviceRepository deviceRepository;
    private final AppRuntimeStatRepository appRuntimeStatRepository;
    private final AppCatalogRepository appCatalogRepository;
    private final ActivitySampleRepository activitySampleRepository;

    public TelemetryService(
            DeviceRepository deviceRepository,
            AppRuntimeStatRepository appRuntimeStatRepository,
            AppCatalogRepository appCatalogRepository,
            ActivitySampleRepository activitySampleRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.appRuntimeStatRepository = appRuntimeStatRepository;
        this.appCatalogRepository = appCatalogRepository;
        this.activitySampleRepository = activitySampleRepository;
    }

    @Transactional
    public TelemetryIngestResponse ingest(
            AgentDevicePrincipal agentDevicePrincipal,
            TelemetryIngestRequest telemetryIngestRequest
    ) {
        DeviceEntity deviceEntity = deviceRepository
                .findByIdAndWorkerId(agentDevicePrincipal.getDeviceId(), agentDevicePrincipal.getWorkerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device not found"));
        if (deviceEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
        }
        if (!Objects.equals(deviceEntity.getHardwareId(), agentDevicePrincipal.getHardwareId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Hardware id mismatch");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate activityDate = now.toLocalDate();
        deviceEntity.setLastSeenAt(now);

        Map<String, TelemetryIngestRequest.AppActivitySample> uniqueSamplesByIdentifier =
                telemetryIngestRequest.apps().stream()
                        .filter(sample -> StringUtils.isNotBlank(sample.appIdentifier()))
                        .collect(Collectors.toMap(
                                sample -> sample.appIdentifier().trim().toLowerCase(),
                                sample -> sample,
                                (first, second) -> second,
                                LinkedHashMap::new
                        ));

        List<TelemetryIngestResponse.AppDeltaResult> results = new ArrayList<>();
        long totalDeltaSeconds = 0L;

        for (TelemetryIngestRequest.AppActivitySample appActivitySample : uniqueSamplesByIdentifier.values()) {
            String appIdentifier = appActivitySample.appIdentifier().trim();
            String displayName = StringUtils.isNotBlank(appActivitySample.displayName())
                    ? appActivitySample.displayName().trim()
                    : appIdentifier;
            long currentValueSeconds = Math.max(appActivitySample.currentValueSeconds(), 0L);

            ensureAppCatalogEntry(appIdentifier, displayName, now);

            AppRuntimeStatEntity runtimeStatEntity = appRuntimeStatRepository
                    .findByWorkerIdAndDeviceIdAndAppIdentifierIgnoreCase(
                            agentDevicePrincipal.getWorkerId(),
                            agentDevicePrincipal.getDeviceId(),
                            appIdentifier
                    )
                    .orElseGet(() -> new AppRuntimeStatEntity(
                            agentDevicePrincipal.getWorkerId(),
                            agentDevicePrincipal.getDeviceId(),
                            appIdentifier,
                            0L,
                            0L,
                            now
                    ));

            long lastAgentValue = runtimeStatEntity.getLastAgentValue();
            long deltaSeconds = currentValueSeconds >= lastAgentValue
                    ? currentValueSeconds - lastAgentValue
                    : currentValueSeconds;

            runtimeStatEntity.setTotalSeconds(runtimeStatEntity.getTotalSeconds() + deltaSeconds);
            runtimeStatEntity.setLastAgentValue(currentValueSeconds);
            runtimeStatEntity.setUpdatedAt(now);
            appRuntimeStatRepository.save(runtimeStatEntity);

            if (deltaSeconds > 0L) {
                applyDailyActivitySampleDelta(
                        agentDevicePrincipal.getWorkerId(),
                        displayName,
                        deltaSeconds,
                        activityDate
                );
            }

            totalDeltaSeconds += deltaSeconds;
            results.add(new TelemetryIngestResponse.AppDeltaResult(
                    appIdentifier,
                    deltaSeconds,
                    runtimeStatEntity.getTotalSeconds(),
                    runtimeStatEntity.getLastAgentValue()
            ));
        }

        logger.info(
                "schema={} Telemetry ingested: workerId={}, deviceId={}, apps={}, totalDeltaSeconds={}",
                schema,
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId(),
                results.size(),
                totalDeltaSeconds
        );

        return new TelemetryIngestResponse(results.size(), totalDeltaSeconds, results);
    }

    private void ensureAppCatalogEntry(String appIdentifier, String displayName, OffsetDateTime now) {
        appCatalogRepository.findByAppIdentifierIgnoreCase(appIdentifier)
                .ifPresentOrElse(
                        existingCatalogEntity -> {
                            if (StringUtils.isNotBlank(displayName)
                                    && !Objects.equals(existingCatalogEntity.getDisplayName(), displayName)) {
                                existingCatalogEntity.setDisplayName(displayName);
                            }
                        },
                        () -> appCatalogRepository.save(new AppCatalogEntity(appIdentifier, displayName, now))
                );
    }

    private void applyDailyActivitySampleDelta(
            Long workerId,
            String appName,
            long deltaSeconds,
            LocalDate activityDate
    ) {
        ActivitySampleEntity activitySampleEntity = activitySampleRepository
                .findByWorkerIdAndAppNameIgnoreCaseAndActivityDateAndIdle(workerId, appName, activityDate, false)
                .orElseGet(() -> new ActivitySampleEntity(workerId, appName, 0L, false, activityDate));
        activitySampleEntity.setSeconds(activitySampleEntity.getSeconds() + deltaSeconds);
        activitySampleRepository.save(activitySampleEntity);
    }
}
