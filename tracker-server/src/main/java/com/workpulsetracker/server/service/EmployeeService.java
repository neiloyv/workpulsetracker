package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AccountType;
import com.workpulsetracker.server.domain.AgentKeyEntity;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.AgentKeyRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.web.dto.CreateEmployeeRequest;
import com.workpulsetracker.server.web.dto.CreateEmployeeResponse;
import com.workpulsetracker.server.web.dto.EmployeeResponse;
import com.workpulsetracker.server.web.dto.UpdateEmployeeRequest;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private static final String schema = "public";
    private static final String DEFAULT_TEMPORARY_PASSWORD = "ChangeMe123!";

    private final OrganizationService organizationService;
    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentKeyRepository agentKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(
            OrganizationService organizationService,
            AppUserRepository appUserRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            AgentKeyRepository agentKeyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationService = organizationService;
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.agentKeyRepository = agentKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees(
            AppUserEntity currentUser,
            String search,
            UUID branchId,
            UUID departmentId
    ) {
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        List<AppUserEntity> employees = findOrganizationEmployees(
                organizationEntity.getId(),
                branchId,
                departmentId
        );
        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim().toLowerCase();
        List<AppUserEntity> filteredEmployees = employees.stream()
                .filter(appUserEntity -> matchesSearch(appUserEntity, normalizedSearch))
                .collect(Collectors.toList());
        return mapToEmployeeResponses(filteredEmployees);
    }

    @Transactional
    public CreateEmployeeResponse createEmployee(AppUserEntity currentUser, CreateEmployeeRequest createEmployeeRequest) {
        organizationService.requireOwner(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);

        String email = createEmployeeRequest.email().trim().toLowerCase();
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        validateBranchAndDepartmentBelongToOrganization(
                organizationEntity.getId(),
                createEmployeeRequest.branchId(),
                createEmployeeRequest.departmentId()
        );

        String temporaryPassword = StringUtils.isBlank(createEmployeeRequest.password())
                ? DEFAULT_TEMPORARY_PASSWORD
                : createEmployeeRequest.password();
        String phone = StringUtils.isBlank(createEmployeeRequest.phone())
                ? null
                : createEmployeeRequest.phone().trim();

        AppUserEntity createdAppUser = new AppUserEntity(
                UUID.randomUUID(),
                organizationEntity.getId(),
                email,
                passwordEncoder.encode(temporaryPassword),
                createEmployeeRequest.displayName().trim(),
                null,
                null,
                phone,
                AccountType.ORGANIZATION,
                UserRole.MEMBER,
                createEmployeeRequest.branchId(),
                createEmployeeRequest.departmentId(),
                false,
                null,
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

        logger.info("schema={} Created employee {}", schema, createdAppUser.getEmail());
        return new CreateEmployeeResponse(
                createdAppUser.getId(),
                createdAppUser.getDisplayName(),
                createdAppUser.getEmail(),
                createdAppUser.getPhone(),
                createdAppUser.getRole().name(),
                createdAppUser.getBranchId(),
                createdAppUser.getDepartmentId(),
                plaintextKey,
                keyPrefix,
                temporaryPassword
        );
    }

    @Transactional
    public EmployeeResponse updateEmployee(
            AppUserEntity currentUser,
            UUID employeeId,
            UpdateEmployeeRequest updateEmployeeRequest
    ) {
        organizationService.requireOwner(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);

        AppUserEntity employeeEntity = appUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (!Objects.equals(employeeEntity.getOrganizationId(), organizationEntity.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }

        String email = updateEmployeeRequest.email().trim().toLowerCase();
        appUserRepository.findByEmailIgnoreCase(email)
                .filter(existingUser -> !Objects.equals(existingUser.getId(), employeeId))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
                });

        validateBranchAndDepartmentBelongToOrganization(
                organizationEntity.getId(),
                updateEmployeeRequest.branchId(),
                updateEmployeeRequest.departmentId()
        );

        employeeEntity.setDisplayName(updateEmployeeRequest.displayName().trim());
        employeeEntity.setEmail(email);
        employeeEntity.setPhone(StringUtils.isBlank(updateEmployeeRequest.phone())
                ? null
                : updateEmployeeRequest.phone().trim());
        employeeEntity.setBranchId(updateEmployeeRequest.branchId());
        employeeEntity.setDepartmentId(updateEmployeeRequest.departmentId());
        appUserRepository.save(employeeEntity);

        logger.info("schema={} Updated employee {}", schema, employeeEntity.getEmail());
        return mapToEmployeeResponses(List.of(employeeEntity)).get(0);
    }

    private List<AppUserEntity> findOrganizationEmployees(
            UUID organizationId,
            UUID branchId,
            UUID departmentId
    ) {
        if (Objects.nonNull(branchId) && Objects.nonNull(departmentId)) {
            return appUserRepository.findByOrganizationIdAndBranchIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    branchId,
                    departmentId
            );
        }
        if (Objects.nonNull(branchId)) {
            return appUserRepository.findByOrganizationIdAndBranchIdOrderByCreatedAtAsc(organizationId, branchId);
        }
        if (Objects.nonNull(departmentId)) {
            return appUserRepository.findByOrganizationIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    departmentId
            );
        }
        return appUserRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
    }

    private static boolean matchesSearch(AppUserEntity appUserEntity, String normalizedSearch) {
        if (StringUtils.isBlank(normalizedSearch)) {
            return true;
        }
        String displayName = StringUtils.defaultString(appUserEntity.getDisplayName()).toLowerCase();
        String email = StringUtils.defaultString(appUserEntity.getEmail()).toLowerCase();
        return displayName.contains(normalizedSearch) || email.contains(normalizedSearch);
    }

    private void validateBranchAndDepartmentBelongToOrganization(
            UUID organizationId,
            UUID branchId,
            UUID departmentId
    ) {
        if (Objects.nonNull(branchId)) {
            branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Branch does not belong to organization"
                    ));
        }
        if (Objects.nonNull(departmentId)) {
            DepartmentEntity departmentEntity = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            if (Objects.nonNull(branchId) && !Objects.equals(departmentEntity.getBranchId(), branchId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not belong to branch");
            }
            branchRepository.findByIdAndOrganizationId(departmentEntity.getBranchId(), organizationId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Department does not belong to organization"
                    ));
        }
    }

    private List<EmployeeResponse> mapToEmployeeResponses(List<AppUserEntity> employees) {
        List<UUID> branchIds = employees.stream()
                .map(AppUserEntity::getBranchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<UUID> departmentIds = employees.stream()
                .map(AppUserEntity::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, BranchEntity> branchesById = branchIds.isEmpty()
                ? Map.of()
                : branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
        Map<UUID, DepartmentEntity> departmentsById = departmentIds.isEmpty()
                ? Map.of()
                : departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, Function.identity()));

        return employees.stream()
                .map(appUserEntity -> {
                    String agentKeyPrefix = agentKeyRepository.findByUserIdAndRevokedAtIsNull(appUserEntity.getId())
                            .map(AgentKeyEntity::getKeyPrefix)
                            .orElse(null);
                    BranchEntity branchEntity = Objects.nonNull(appUserEntity.getBranchId())
                            ? branchesById.get(appUserEntity.getBranchId())
                            : null;
                    DepartmentEntity departmentEntity = Objects.nonNull(appUserEntity.getDepartmentId())
                            ? departmentsById.get(appUserEntity.getDepartmentId())
                            : null;
                    return new EmployeeResponse(
                            appUserEntity.getId(),
                            appUserEntity.getDisplayName(),
                            appUserEntity.getEmail(),
                            appUserEntity.getPhone(),
                            appUserEntity.getRole().name(),
                            appUserEntity.getBranchId(),
                            Objects.nonNull(branchEntity) ? branchEntity.getName() : null,
                            appUserEntity.getDepartmentId(),
                            Objects.nonNull(departmentEntity) ? departmentEntity.getName() : null,
                            appUserEntity.isAgentInstalled(),
                            appUserEntity.getAgentVersion(),
                            agentKeyPrefix,
                            appUserEntity.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}
