package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AppCatalogEntity;
import com.workpulsetracker.server.domain.AppRuntimeStatEntity;
import com.workpulsetracker.server.domain.DeviceEntity;
import com.workpulsetracker.server.domain.EntityStatus;
import com.workpulsetracker.server.repository.AppCatalogRepository;
import com.workpulsetracker.server.repository.AppRuntimeStatRepository;
import com.workpulsetracker.server.repository.DeviceRepository;
import com.workpulsetracker.server.security.AgentDevicePrincipal;
import com.workpulsetracker.server.web.dto.AgentSyncResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AgentSyncService {

    private static final Logger logger = LoggerFactory.getLogger(AgentSyncService.class);
    private static final String schema = "public";

    private final DeviceRepository deviceRepository;
    private final AppRuntimeStatRepository appRuntimeStatRepository;
    private final AppCatalogRepository appCatalogRepository;

    public AgentSyncService(
            DeviceRepository deviceRepository,
            AppRuntimeStatRepository appRuntimeStatRepository,
            AppCatalogRepository appCatalogRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.appRuntimeStatRepository = appRuntimeStatRepository;
        this.appCatalogRepository = appCatalogRepository;
    }

    /**
     * Reverse sync: агрегированные totals по аккаунту (все устройства worker),
     * чтобы агент мог восстановить локальный вид статистики.
     */
    @Transactional(readOnly = true)
    public AgentSyncResponse getAccountTotals(AgentDevicePrincipal agentDevicePrincipal) {
        DeviceEntity deviceEntity = deviceRepository
                .findByIdAndWorkerId(agentDevicePrincipal.getDeviceId(), agentDevicePrincipal.getWorkerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device not found"));
        if (deviceEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
        }

        List<AppRuntimeStatRepository.AppTotalAggregate> aggregates =
                appRuntimeStatRepository.sumTotalSecondsByWorkerIdGroupedByApp(agentDevicePrincipal.getWorkerId());

        Map<String, String> displayNameByIdentifier = aggregates.stream()
                .map(AppRuntimeStatRepository.AppTotalAggregate::getAppIdentifier)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(appIdentifier -> appCatalogRepository.findByAppIdentifierIgnoreCase(appIdentifier)
                        .map(catalogEntity -> Map.entry(
                                appIdentifier.toLowerCase(Locale.ROOT),
                                catalogEntity.getDisplayName()
                        ))
                        .orElseGet(() -> Map.entry(appIdentifier.toLowerCase(Locale.ROOT), appIdentifier)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));

        List<AgentSyncResponse.AppRuntimeItem> apps = aggregates.stream()
                .sorted(Comparator.comparing(
                        AppRuntimeStatRepository.AppTotalAggregate::getTotalSeconds,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(aggregate -> {
                    String appIdentifier = aggregate.getAppIdentifier();
                    long totalSeconds = Objects.isNull(aggregate.getTotalSeconds()) ? 0L : aggregate.getTotalSeconds();
                    String displayName = displayNameByIdentifier.getOrDefault(
                            appIdentifier.toLowerCase(Locale.ROOT),
                            appIdentifier
                    );
                    return new AgentSyncResponse.AppRuntimeItem(appIdentifier, displayName, totalSeconds);
                })
                .collect(Collectors.toList());

        long totalSeconds = apps.stream().mapToLong(AgentSyncResponse.AppRuntimeItem::totalSeconds).sum();

        logger.info(
                "schema={} Reverse sync: workerId={}, deviceId={}, apps={}, totalSeconds={}",
                schema,
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId(),
                apps.size(),
                totalSeconds
        );

        return new AgentSyncResponse(
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId(),
                true,
                totalSeconds,
                apps
        );
    }

    @Transactional(readOnly = true)
    public AgentSyncResponse getDeviceTotals(AgentDevicePrincipal agentDevicePrincipal) {
        DeviceEntity deviceEntity = deviceRepository
                .findByIdAndWorkerId(agentDevicePrincipal.getDeviceId(), agentDevicePrincipal.getWorkerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device not found"));
        if (deviceEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
        }

        List<AppRuntimeStatEntity> runtimeStats = appRuntimeStatRepository.findByWorkerIdAndDeviceId(
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId()
        );

        List<AgentSyncResponse.AppRuntimeItem> apps = runtimeStats.stream()
                .sorted(Comparator.comparingLong(AppRuntimeStatEntity::getTotalSeconds).reversed())
                .map(runtimeStatEntity -> {
                    String displayName = appCatalogRepository
                            .findByAppIdentifierIgnoreCase(runtimeStatEntity.getAppIdentifier())
                            .map(AppCatalogEntity::getDisplayName)
                            .orElse(runtimeStatEntity.getAppIdentifier());
                    return new AgentSyncResponse.AppRuntimeItem(
                            runtimeStatEntity.getAppIdentifier(),
                            displayName,
                            runtimeStatEntity.getTotalSeconds()
                    );
                })
                .collect(Collectors.toList());

        long totalSeconds = apps.stream().mapToLong(AgentSyncResponse.AppRuntimeItem::totalSeconds).sum();

        return new AgentSyncResponse(
                agentDevicePrincipal.getWorkerId(),
                agentDevicePrincipal.getDeviceId(),
                false,
                totalSeconds,
                apps
        );
    }
}
