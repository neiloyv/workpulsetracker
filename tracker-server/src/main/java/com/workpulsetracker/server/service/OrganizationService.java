package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.EntityStatus;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.OrganizationSettingEntity;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.domain.WorkerEntity;
import com.workpulsetracker.server.repository.OrganizationRepository;
import com.workpulsetracker.server.repository.OrganizationSettingRepository;
import com.workpulsetracker.server.repository.WorkerRepository;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.OrganizationResponse;
import com.workpulsetracker.server.web.dto.OrganizationStatsResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);
    private static final String schema = "public";

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository organizationSettingRepository;
    private final WorkerRepository workerRepository;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationSettingRepository organizationSettingRepository,
            WorkerRepository workerRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationSettingRepository = organizationSettingRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return new MeResponse(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getDisplayName(),
                currentUser.getRole().name(),
                organizationEntity.getType().name(),
                organizationEntity.getId(),
                organizationEntity.getName(),
                currentUser.getWorkerId(),
                currentUser.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        return new OrganizationResponse(
                organizationEntity.getId(),
                organizationEntity.getName(),
                organizationEntity.getType().name(),
                organizationEntity.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettings(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        return organizationSettingRepository.findByOrganizationId(organizationEntity.getId()).stream()
                .collect(Collectors.toMap(
                        OrganizationSettingEntity::getSettingKey,
                        OrganizationSettingEntity::getSettingValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    @Transactional
    public Map<String, String> updateSettings(UserAccountEntity currentUser, Map<String, String> settings) {
        requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        if (Objects.isNull(settings)) {
            return getSettings(currentUser);
        }

        settings.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
                .forEach(entry -> {
                    String settingKey = entry.getKey().trim();
                    String settingValue = Objects.requireNonNullElse(entry.getValue(), "");
                    organizationSettingRepository
                            .findByOrganizationIdAndSettingKey(organizationEntity.getId(), settingKey)
                            .ifPresentOrElse(
                                    existing -> {
                                        existing.setSettingValue(settingValue);
                                        organizationSettingRepository.save(existing);
                                    },
                                    () -> organizationSettingRepository.save(
                                            new OrganizationSettingEntity(
                                                    organizationEntity.getId(),
                                                    settingKey,
                                                    settingValue
                                            )
                                    )
                            );
                });
        logger.info("schema={} Updated settings for organization {}", schema, organizationEntity.getId());
        return getSettings(currentUser);
    }

    @Transactional(readOnly = true)
    public OrganizationStatsResponse getStats(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        List<WorkerEntity> organizationWorkers =
                workerRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationEntity.getId());

        long activeWorkers = organizationWorkers.stream()
                .filter(workerEntity -> workerEntity.getStatus() == EntityStatus.ACTIVE)
                .count();

        List<OrganizationStatsResponse.WorkerStatItem> workerStatItems = organizationWorkers.stream()
                .map(workerEntity -> new OrganizationStatsResponse.WorkerStatItem(
                        workerEntity.getEmail(),
                        StringUtils.defaultIfBlank(workerEntity.getDisplayName(), workerEntity.getEmail()),
                        0L
                ))
                .collect(Collectors.toList());

        return new OrganizationStatsResponse(
                organizationWorkers.size(),
                activeWorkers,
                workerStatItems
        );
    }

    public OrganizationEntity requireOrganization(UserAccountEntity currentUser) {
        if (Objects.isNull(currentUser.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization is required");
        }
        return organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    public void requireOwnerOrManager(UserAccountEntity currentUser) {
        if (currentUser.getRole() != UserRole.OWNER && currentUser.getRole() != UserRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization owner or manager can perform this action");
        }
    }
}
