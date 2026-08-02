package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.EntityStatus;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.OrganizationType;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.UserAccountRepository;
import com.workpulsetracker.server.web.dto.CreateManagerRequest;
import com.workpulsetracker.server.web.dto.ManagerResponse;
import com.workpulsetracker.server.web.dto.UpdateManagerRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    private static final Logger logger = LoggerFactory.getLogger(ManagerService.class);
    private static final String schema = "public";

    private final OrganizationService organizationService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagerService(
            OrganizationService organizationService,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationService = organizationService;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ManagerResponse> listManagers(UserAccountEntity currentUser, String search) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);
        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim().toLowerCase();

        return userAccountRepository
                .findByOrganizationIdAndRoleOrderByCreatedAtAsc(organizationEntity.getId(), UserRole.MANAGER)
                .stream()
                .filter(managerAccount -> matchesSearch(managerAccount, normalizedSearch))
                .map(this::toManagerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ManagerResponse createManager(UserAccountEntity currentUser, CreateManagerRequest createManagerRequest) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);

        String email = createManagerRequest.email().trim().toLowerCase();
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        UserAccountEntity managerAccount = new UserAccountEntity(
                organizationEntity.getId(),
                email,
                passwordEncoder.encode(createManagerRequest.password()),
                createManagerRequest.displayName().trim(),
                UserRole.MANAGER,
                null,
                EntityStatus.ACTIVE,
                OffsetDateTime.now()
        );
        userAccountRepository.save(managerAccount);

        logger.info(
                "schema={} Created manager {} for organization {}",
                schema,
                email,
                organizationEntity.getId()
        );
        return toManagerResponse(managerAccount);
    }

    @Transactional
    public ManagerResponse updateManager(
            UserAccountEntity currentUser,
            Long managerId,
            UpdateManagerRequest updateManagerRequest
    ) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);
        UserAccountEntity managerAccount = requireManagerInOrganization(managerId, organizationEntity.getId());

        String email = updateManagerRequest.email().trim().toLowerCase();
        userAccountRepository.findByEmailIgnoreCase(email)
                .filter(existingAccount -> !Objects.equals(existingAccount.getId(), managerId))
                .ifPresent(existingAccount -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
                });

        managerAccount.setDisplayName(updateManagerRequest.displayName().trim());
        managerAccount.setEmail(email);
        if (StringUtils.isNotBlank(updateManagerRequest.password())) {
            managerAccount.setPasswordHash(passwordEncoder.encode(updateManagerRequest.password()));
        }
        if (Objects.nonNull(updateManagerRequest.status())) {
            managerAccount.setStatus(updateManagerRequest.status());
        }
        userAccountRepository.save(managerAccount);

        logger.info("schema={} Updated manager {}", schema, managerAccount.getEmail());
        return toManagerResponse(managerAccount);
    }

    @Transactional
    public void deleteManager(UserAccountEntity currentUser, Long managerId) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);
        UserAccountEntity managerAccount = requireManagerInOrganization(managerId, organizationEntity.getId());

        if (Objects.equals(managerAccount.getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }

        userAccountRepository.delete(managerAccount);
        logger.info(
                "schema={} Deleted manager {} from organization {}",
                schema,
                managerAccount.getEmail(),
                organizationEntity.getId()
        );
    }

    private UserAccountEntity requireManagerInOrganization(Long managerId, Long organizationId) {
        UserAccountEntity managerAccount = userAccountRepository.findByIdAndOrganizationId(managerId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));
        if (managerAccount.getRole() != UserRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not a manager");
        }
        return managerAccount;
    }

    private OrganizationEntity requireCompanyOrganization(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        if (organizationEntity.getType() != OrganizationType.COMPANY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Manager management is only available for company organizations"
            );
        }
        return organizationEntity;
    }

    private static boolean matchesSearch(UserAccountEntity managerAccount, String normalizedSearch) {
        if (StringUtils.isBlank(normalizedSearch)) {
            return true;
        }
        String displayName = StringUtils.defaultString(managerAccount.getDisplayName()).toLowerCase();
        String email = StringUtils.defaultString(managerAccount.getEmail()).toLowerCase();
        return displayName.contains(normalizedSearch) || email.contains(normalizedSearch);
    }

    private ManagerResponse toManagerResponse(UserAccountEntity managerAccount) {
        return new ManagerResponse(
                managerAccount.getId(),
                managerAccount.getDisplayName(),
                managerAccount.getEmail(),
                managerAccount.getRole().name(),
                managerAccount.getStatus().name(),
                managerAccount.getCreatedAt()
        );
    }
}
