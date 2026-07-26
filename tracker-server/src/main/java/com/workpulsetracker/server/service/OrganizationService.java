package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AccountType;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.OrganizationSettingEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.AgentKeyRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import com.workpulsetracker.server.repository.OrganizationRepository;
import com.workpulsetracker.server.repository.OrganizationSettingRepository;
import com.workpulsetracker.server.web.dto.CreateEmployeeRequest;
import com.workpulsetracker.server.web.dto.CreateEmployeeResponse;
import com.workpulsetracker.server.web.dto.CreateUserRequest;
import com.workpulsetracker.server.web.dto.CreateUserResponse;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.OrganizationResponse;
import com.workpulsetracker.server.web.dto.OrganizationStatsResponse;
import com.workpulsetracker.server.web.dto.OrganizationUserResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
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

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final AgentKeyRepository agentKeyRepository;
    private final OrganizationSettingRepository organizationSettingRepository;
    private final EmployeeService employeeService;

    public OrganizationService(
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            AgentKeyRepository agentKeyRepository,
            OrganizationSettingRepository organizationSettingRepository,
            @Lazy EmployeeService employeeService
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.agentKeyRepository = agentKeyRepository;
        this.organizationSettingRepository = organizationSettingRepository;
        this.employeeService = employeeService;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(AppUserEntity currentUser) {
        String organizationName = null;
        if (Objects.nonNull(currentUser.getOrganizationId())) {
            organizationName = organizationRepository.findById(currentUser.getOrganizationId())
                    .map(OrganizationEntity::getName)
                    .orElse(null);
        }
        return new MeResponse(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getDisplayName(),
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getPhone(),
                currentUser.getRole().name(),
                currentUser.getAccountType().name(),
                currentUser.isOnboarded(),
                currentUser.getOrganizationId(),
                organizationName,
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.isAgentInstalled(),
                currentUser.getAgentVersion()
        );
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(AppUserEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        return new OrganizationResponse(organizationEntity.getId(), organizationEntity.getName());
    }

    @Transactional(readOnly = true)
    public List<OrganizationUserResponse> listUsers(AppUserEntity currentUser) {
        return employeeService.listEmployees(currentUser, null, null, null).stream()
                .map(employeeResponse -> new OrganizationUserResponse(
                        employeeResponse.id(),
                        employeeResponse.email(),
                        employeeResponse.displayName(),
                        employeeResponse.phone(),
                        employeeResponse.role(),
                        employeeResponse.branchId(),
                        employeeResponse.departmentId(),
                        true,
                        employeeResponse.agentKeyPrefix(),
                        employeeResponse.createdAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateUserResponse createUser(AppUserEntity currentUser, CreateUserRequest createUserRequest) {
        CreateEmployeeResponse createEmployeeResponse = employeeService.createEmployee(
                currentUser,
                new CreateEmployeeRequest(
                        createUserRequest.displayName(),
                        createUserRequest.email(),
                        createUserRequest.phone(),
                        createUserRequest.branchId(),
                        createUserRequest.departmentId(),
                        createUserRequest.password()
                )
        );
        return new CreateUserResponse(
                createEmployeeResponse.id(),
                createEmployeeResponse.email(),
                createEmployeeResponse.displayName(),
                createEmployeeResponse.role(),
                createEmployeeResponse.agentKey(),
                createEmployeeResponse.agentKeyPrefix()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettings(AppUserEntity currentUser) {
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
    public Map<String, String> updateSettings(AppUserEntity currentUser, Map<String, String> settings) {
        requireOwner(currentUser);
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
    public OrganizationStatsResponse getStats(AppUserEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        List<AppUserEntity> organizationUsers =
                appUserRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationEntity.getId());

        long activeUsersWithAgentKey = organizationUsers.stream()
                .filter(appUserEntity -> agentKeyRepository.findByUserIdAndRevokedAtIsNull(appUserEntity.getId()).isPresent())
                .count();

        List<OrganizationStatsResponse.UserStatItem> userStatItems = organizationUsers.stream()
                .map(appUserEntity -> new OrganizationStatsResponse.UserStatItem(
                        appUserEntity.getEmail(),
                        StringUtils.defaultIfBlank(appUserEntity.getDisplayName(), appUserEntity.getEmail()),
                        0L
                ))
                .collect(Collectors.toList());

        return new OrganizationStatsResponse(
                organizationUsers.size(),
                activeUsersWithAgentKey,
                userStatItems
        );
    }

    public OrganizationEntity requireOrganization(AppUserEntity currentUser) {
        if (currentUser.getAccountType() != AccountType.ORGANIZATION
                || Objects.isNull(currentUser.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization account is required");
        }
        return organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    public void requireOwner(AppUserEntity currentUser) {
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization owner can perform this action");
        }
    }
}
