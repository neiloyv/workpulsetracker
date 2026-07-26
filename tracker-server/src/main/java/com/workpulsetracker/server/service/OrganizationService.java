package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AgentKeyEntity;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.OrganizationSettingEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.AgentKeyRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import com.workpulsetracker.server.repository.OrganizationRepository;
import com.workpulsetracker.server.repository.OrganizationSettingRepository;
import com.workpulsetracker.server.web.dto.CreateUserRequest;
import com.workpulsetracker.server.web.dto.CreateUserResponse;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.OnboardingRequest;
import com.workpulsetracker.server.web.dto.OrganizationResponse;
import com.workpulsetracker.server.web.dto.OrganizationStatsResponse;
import com.workpulsetracker.server.web.dto.OrganizationUserResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final AgentKeyRepository agentKeyRepository;
    private final OrganizationSettingRepository organizationSettingRepository;

    public OrganizationService(
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            AgentKeyRepository agentKeyRepository,
            OrganizationSettingRepository organizationSettingRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.agentKeyRepository = agentKeyRepository;
        this.organizationSettingRepository = organizationSettingRepository;
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
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getRole().name(),
                currentUser.isOnboarded(),
                currentUser.getOrganizationId(),
                organizationName
        );
    }

    @Transactional
    public MeResponse completeOnboarding(AppUserEntity currentUser, OnboardingRequest onboardingRequest) {
        if (currentUser.isOnboarded()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already onboarded");
        }

        OrganizationEntity organizationEntity = new OrganizationEntity(
                UUID.randomUUID(),
                onboardingRequest.companyName().trim(),
                OffsetDateTime.now()
        );
        organizationRepository.save(organizationEntity);

        currentUser.setOrganizationId(organizationEntity.getId());
        currentUser.setFirstName(onboardingRequest.firstName().trim());
        currentUser.setLastName(onboardingRequest.lastName().trim());
        currentUser.setRole(UserRole.OWNER);
        currentUser.setOnboarded(true);
        appUserRepository.save(currentUser);

        String plaintextKey = AgentKeyGenerator.generatePlaintextKey();
        agentKeyRepository.save(new AgentKeyEntity(
                UUID.randomUUID(),
                currentUser.getId(),
                AgentKeyGenerator.hashKey(plaintextKey),
                AgentKeyGenerator.prefixOf(plaintextKey),
                OffsetDateTime.now(),
                null
        ));

        logger.info("Onboarding completed for user {} org {}", currentUser.getEmail(), organizationEntity.getName());
        return getMe(currentUser);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(AppUserEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        return new OrganizationResponse(organizationEntity.getId(), organizationEntity.getName());
    }

    @Transactional(readOnly = true)
    public List<OrganizationUserResponse> listUsers(AppUserEntity currentUser) {
        OrganizationEntity organizationEntity = requireOrganization(currentUser);
        return appUserRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationEntity.getId()).stream()
                .map(appUserEntity -> {
                    String agentKeyPrefix = agentKeyRepository.findByUserIdAndRevokedAtIsNull(appUserEntity.getId())
                            .map(AgentKeyEntity::getKeyPrefix)
                            .orElse(null);
                    return new OrganizationUserResponse(
                            appUserEntity.getId(),
                            appUserEntity.getEmail(),
                            appUserEntity.getFirstName(),
                            appUserEntity.getLastName(),
                            appUserEntity.getRole().name(),
                            appUserEntity.isOnboarded(),
                            agentKeyPrefix,
                            appUserEntity.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateUserResponse createUser(AppUserEntity currentUser, CreateUserRequest createUserRequest) {
        requireOwner(currentUser);
        OrganizationEntity organizationEntity = requireOrganization(currentUser);

        String email = createUserRequest.email().trim().toLowerCase();
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        AppUserEntity createdAppUser = new AppUserEntity(
                UUID.randomUUID(),
                organizationEntity.getId(),
                null,
                email,
                createUserRequest.firstName().trim(),
                createUserRequest.lastName().trim(),
                UserRole.MEMBER,
                true,
                OffsetDateTime.now()
        );
        appUserRepository.save(createdAppUser);

        String plaintextKey = AgentKeyGenerator.generatePlaintextKey();
        String keyPrefix = AgentKeyGenerator.prefixOf(plaintextKey);
        agentKeyRepository.save(new AgentKeyEntity(
                UUID.randomUUID(),
                createdAppUser.getId(),
                AgentKeyGenerator.hashKey(plaintextKey),
                keyPrefix,
                OffsetDateTime.now(),
                null
        ));

        logger.info("Created organization user {} with agent key", createdAppUser.getEmail());
        return new CreateUserResponse(
                createdAppUser.getId(),
                createdAppUser.getEmail(),
                createdAppUser.getFirstName(),
                createdAppUser.getLastName(),
                createdAppUser.getRole().name(),
                plaintextKey,
                keyPrefix
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
                        buildFullName(appUserEntity),
                        0L
                ))
                .collect(Collectors.toList());

        return new OrganizationStatsResponse(
                organizationUsers.size(),
                activeUsersWithAgentKey,
                userStatItems
        );
    }

    private OrganizationEntity requireOrganization(AppUserEntity currentUser) {
        if (!currentUser.isOnboarded() || Objects.isNull(currentUser.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete onboarding first");
        }
        return organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private void requireOwner(AppUserEntity currentUser) {
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization owner can perform this action");
        }
    }

    private static String buildFullName(AppUserEntity appUserEntity) {
        String firstName = StringUtils.defaultString(appUserEntity.getFirstName());
        String lastName = StringUtils.defaultString(appUserEntity.getLastName());
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.isNotBlank(fullName) ? fullName : appUserEntity.getEmail();
    }
}
