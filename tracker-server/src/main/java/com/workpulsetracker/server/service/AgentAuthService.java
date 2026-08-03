package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.DeviceEntity;
import com.workpulsetracker.server.domain.EntityStatus;
import com.workpulsetracker.server.domain.SubscriptionEntity;
import com.workpulsetracker.server.domain.SubscriptionPlan;
import com.workpulsetracker.server.domain.SubscriptionStatus;
import com.workpulsetracker.server.domain.WorkerEntity;
import com.workpulsetracker.server.repository.DeviceRepository;
import com.workpulsetracker.server.repository.SubscriptionRepository;
import com.workpulsetracker.server.repository.WorkerRepository;
import com.workpulsetracker.server.security.AgentJwtService;
import com.workpulsetracker.server.web.dto.AgentAuthRequest;
import com.workpulsetracker.server.web.dto.AgentAuthResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Service
public class AgentAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AgentAuthService.class);
    private static final String schema = "public";
    private static final int FREE_PLAN_MAX_DEVICES_PER_WORKER = 1;
    private static final int PRO_PLAN_MAX_DEVICES_PER_WORKER = 5;

    private final WorkerRepository workerRepository;
    private final DeviceRepository deviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AgentJwtService agentJwtService;

    public AgentAuthService(
            WorkerRepository workerRepository,
            DeviceRepository deviceRepository,
            SubscriptionRepository subscriptionRepository,
            AgentJwtService agentJwtService
    ) {
        this.workerRepository = workerRepository;
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.agentJwtService = agentJwtService;
    }

    @Transactional
    public AgentAuthResponse authenticate(AgentAuthRequest agentAuthRequest) {
        String email = agentAuthRequest.email().trim().toLowerCase();
        String accessKey = agentAuthRequest.accessKey().trim();
        String hardwareId = agentAuthRequest.hardwareId().trim();

        WorkerEntity workerEntity = workerRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!email.equalsIgnoreCase(workerEntity.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (workerEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is disabled");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<DeviceEntity> existingDevice = deviceRepository.findByWorkerIdAndHardwareId(
                workerEntity.getId(),
                hardwareId
        );

        DeviceEntity deviceEntity;
        if (existingDevice.isPresent()) {
            deviceEntity = existingDevice.get();
            if (deviceEntity.getStatus() != EntityStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is disabled");
            }
            deviceEntity.setLastSeenAt(now);
            if (StringUtils.isNotBlank(agentAuthRequest.deviceDisplayName())) {
                deviceEntity.setDisplayName(agentAuthRequest.deviceDisplayName().trim());
            }
        } else {
            long activeDeviceCount = deviceRepository.countByWorkerIdAndStatus(
                    workerEntity.getId(),
                    EntityStatus.ACTIVE
            );
            int maxDevices = resolveMaxDevicesPerWorker(workerEntity.getOrganizationId());
            if (activeDeviceCount >= maxDevices) {
                logger.info(
                        "schema={} Device limit exceeded: workerId={}, activeDevices={}, maxDevices={}",
                        schema,
                        workerEntity.getId(),
                        activeDeviceCount,
                        maxDevices
                );
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Device limit exceeded for this license"
                );
            }
            deviceEntity = new DeviceEntity(
                    workerEntity.getId(),
                    hardwareId,
                    StringUtils.isNotBlank(agentAuthRequest.deviceDisplayName())
                            ? agentAuthRequest.deviceDisplayName().trim()
                            : null,
                    now,
                    now,
                    EntityStatus.ACTIVE
            );
            deviceEntity = deviceRepository.save(deviceEntity);
            logger.info(
                    "schema={} Paired new device: workerId={}, deviceId={}, hardwareId={}",
                    schema,
                    workerEntity.getId(),
                    deviceEntity.getId(),
                    hardwareId
            );
        }

        workerEntity.setAgentInstalled(true);
        if (StringUtils.isNotBlank(agentAuthRequest.agentVersion())) {
            workerEntity.setAgentVersion(agentAuthRequest.agentVersion().trim());
        }

        String accessToken = agentJwtService.issueToken(
                workerEntity.getId(),
                deviceEntity.getId(),
                workerEntity.getOrganizationId(),
                hardwareId,
                workerEntity.getEmail()
        );

        return new AgentAuthResponse(
                accessToken,
                "Bearer",
                agentJwtService.getExpirationSeconds(),
                workerEntity.getId(),
                deviceEntity.getId(),
                hardwareId
        );
    }

    private int resolveMaxDevicesPerWorker(Long organizationId) {
        Optional<SubscriptionEntity> subscriptionOptional = subscriptionRepository.findByOrganizationId(organizationId);
        if (subscriptionOptional.isEmpty()) {
            return FREE_PLAN_MAX_DEVICES_PER_WORKER;
        }
        SubscriptionEntity subscriptionEntity = subscriptionOptional.get();
        if (subscriptionEntity.getStatus() != SubscriptionStatus.ACTIVE) {
            return FREE_PLAN_MAX_DEVICES_PER_WORKER;
        }
        if (Objects.equals(subscriptionEntity.getPlan(), SubscriptionPlan.PRO)) {
            return PRO_PLAN_MAX_DEVICES_PER_WORKER;
        }
        return FREE_PLAN_MAX_DEVICES_PER_WORKER;
    }
}
