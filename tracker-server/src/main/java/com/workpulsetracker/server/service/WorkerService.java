package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.enums.EntityStatus;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.enums.OrganizationType;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.domain.WorkerEntity;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.repository.WorkerRepository;
import com.workpulsetracker.server.util.AccessKeyGenerator;
import com.workpulsetracker.server.web.dto.AccessKeyResponse;
import com.workpulsetracker.server.web.dto.AgentInfoResponse;
import com.workpulsetracker.server.web.dto.CreateWorkerRequest;
import com.workpulsetracker.server.web.dto.CreateWorkerResponse;
import com.workpulsetracker.server.web.dto.UpdateWorkerRequest;
import com.workpulsetracker.server.web.dto.WorkerResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);
    private static final String schema = "public";

    private final OrganizationService organizationService;
    private final WorkerRepository workerRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AccessKeyEmailService accessKeyEmailService;

    public WorkerService(
            OrganizationService organizationService,
            WorkerRepository workerRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            AccessKeyEmailService accessKeyEmailService
    ) {
        this.organizationService = organizationService;
        this.workerRepository = workerRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.accessKeyEmailService = accessKeyEmailService;
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> listWorkers(
            UserAccountEntity currentUser,
            String search,
            Long branchId,
            Long departmentId
    ) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);
        List<WorkerEntity> workers = findOrganizationWorkers(organizationEntity.getId(), branchId, departmentId);
        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim().toLowerCase();
        List<WorkerEntity> filteredWorkers = workers.stream()
                .filter(workerEntity -> matchesSearch(workerEntity, normalizedSearch))
                .collect(Collectors.toList());
        return mapToWorkerResponses(filteredWorkers);
    }

    @Transactional
    public CreateWorkerResponse createWorker(UserAccountEntity currentUser, CreateWorkerRequest createWorkerRequest) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);

        String email = createWorkerRequest.email().trim().toLowerCase();
        if (workerRepository.findByOrganizationIdAndEmailIgnoreCase(organizationEntity.getId(), email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Worker with this email already exists");
        }

        Long branchId = Objects.nonNull(createWorkerRequest.branchId())
                ? createWorkerRequest.branchId()
                : requireDefaultBranch(organizationEntity.getId()).getId();
        Long departmentId = Objects.nonNull(createWorkerRequest.departmentId())
                ? createWorkerRequest.departmentId()
                : requireDefaultDepartment(organizationEntity.getId()).getId();
        validateBranchAndDepartmentBelongToOrganization(organizationEntity.getId(), branchId, departmentId);

        String plaintextAccessKey = AccessKeyGenerator.generatePlaintextKey();
        WorkerEntity workerEntity = new WorkerEntity(
                organizationEntity.getId(),
                branchId,
                departmentId,
                createWorkerRequest.displayName().trim(),
                email,
                plaintextAccessKey,
                AccessKeyGenerator.prefixOf(plaintextAccessKey),
                EntityStatus.ACTIVE,
                false,
                null,
                OffsetDateTime.now()
        );
        workerRepository.save(workerEntity);

        accessKeyEmailService.sendAccessKey(email, workerEntity.getDisplayName(), plaintextAccessKey);
        logger.info("schema={} Created worker {} for organization {}", schema, email, organizationEntity.getId());

        return new CreateWorkerResponse(
                workerEntity.getId(),
                workerEntity.getDisplayName(),
                workerEntity.getEmail(),
                workerEntity.getBranchId(),
                workerEntity.getDepartmentId(),
                workerEntity.getStatus().name(),
                true,
                workerEntity.getCreatedAt()
        );
    }

    @Transactional
    public WorkerResponse updateWorker(
            UserAccountEntity currentUser,
            Long workerId,
            UpdateWorkerRequest updateWorkerRequest
    ) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = requireCompanyOrganization(currentUser);
        WorkerEntity workerEntity = workerRepository.findByIdAndOrganizationId(workerId, organizationEntity.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        String email = updateWorkerRequest.email().trim().toLowerCase();
        workerRepository.findByOrganizationIdAndEmailIgnoreCase(organizationEntity.getId(), email)
                .filter(existingWorker -> !Objects.equals(existingWorker.getId(), workerId))
                .ifPresent(existingWorker -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Worker with this email already exists");
                });

        Long branchId = Objects.nonNull(updateWorkerRequest.branchId())
                ? updateWorkerRequest.branchId()
                : workerEntity.getBranchId();
        Long departmentId = Objects.nonNull(updateWorkerRequest.departmentId())
                ? updateWorkerRequest.departmentId()
                : workerEntity.getDepartmentId();
        validateBranchAndDepartmentBelongToOrganization(organizationEntity.getId(), branchId, departmentId);

        workerEntity.setDisplayName(updateWorkerRequest.displayName().trim());
        workerEntity.setEmail(email);
        workerEntity.setBranchId(branchId);
        workerEntity.setDepartmentId(departmentId);
        if (Objects.nonNull(updateWorkerRequest.status())) {
            workerEntity.setStatus(updateWorkerRequest.status());
        }
        workerRepository.save(workerEntity);

        logger.info("schema={} Updated worker {}", schema, workerEntity.getEmail());
        return mapToWorkerResponses(List.of(workerEntity)).get(0);
    }

    @Transactional(readOnly = true)
    public AccessKeyResponse getAccessKey(UserAccountEntity currentUser, Long workerId) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        WorkerEntity workerEntity = workerRepository.findByIdAndOrganizationId(workerId, organizationEntity.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        return new AccessKeyResponse(workerEntity.getAccessKey());
    }

    @Transactional
    public void resendAccessKey(UserAccountEntity currentUser, Long workerId) {
        organizationService.requireOwnerOrManager(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        WorkerEntity workerEntity = workerRepository.findByIdAndOrganizationId(workerId, organizationEntity.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        accessKeyEmailService.sendAccessKey(workerEntity.getEmail(), workerEntity.getDisplayName(), workerEntity.getAccessKey());
        logger.info("schema={} Resent access key for worker {}", schema, workerEntity.getId());
    }

    @Transactional(readOnly = true)
    public AgentInfoResponse getMyAgentInfo(UserAccountEntity currentUser) {
        WorkerEntity workerEntity = requireOwnWorker(currentUser);
        return new AgentInfoResponse(
                workerEntity.getId(),
                workerEntity.getDisplayName(),
                workerEntity.getEmail(),
                workerEntity.getStatus().name(),
                workerEntity.getAccessKeyPrefix(),
                workerEntity.isAgentInstalled(),
                workerEntity.getAgentVersion()
        );
    }

    @Transactional(readOnly = true)
    public AccessKeyResponse getMyAccessKey(UserAccountEntity currentUser) {
        WorkerEntity workerEntity = requireOwnWorker(currentUser);
        return new AccessKeyResponse(workerEntity.getAccessKey());
    }

    @Transactional
    public void resendMyAccessKey(UserAccountEntity currentUser) {
        WorkerEntity workerEntity = requireOwnWorker(currentUser);
        accessKeyEmailService.sendAccessKey(workerEntity.getEmail(), workerEntity.getDisplayName(), workerEntity.getAccessKey());
        logger.info("schema={} Resent own access key for worker {}", schema, workerEntity.getId());
    }

    private WorkerEntity requireOwnWorker(UserAccountEntity currentUser) {
        if (Objects.isNull(currentUser.getWorkerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No worker linked to current account");
        }
        return workerRepository.findById(currentUser.getWorkerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
    }

    private OrganizationEntity requireCompanyOrganization(UserAccountEntity currentUser) {
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        if (organizationEntity.getType() != OrganizationType.COMPANY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Worker management is only available for company organizations");
        }
        return organizationEntity;
    }

    private BranchEntity requireDefaultBranch(Long organizationId) {
        return branchRepository.findByOrganizationIdAndIsDefaultTrue(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default branch not found"));
    }

    private DepartmentEntity requireDefaultDepartment(Long organizationId) {
        return departmentRepository.findByOrganizationIdAndIsDefaultTrue(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default department not found"));
    }

    private List<WorkerEntity> findOrganizationWorkers(Long organizationId, Long branchId, Long departmentId) {
        if (Objects.nonNull(branchId) && Objects.nonNull(departmentId)) {
            return workerRepository.findByOrganizationIdAndBranchIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    branchId,
                    departmentId
            );
        }
        if (Objects.nonNull(branchId)) {
            return workerRepository.findByOrganizationIdAndBranchIdOrderByCreatedAtAsc(organizationId, branchId);
        }
        if (Objects.nonNull(departmentId)) {
            return workerRepository.findByOrganizationIdAndDepartmentIdOrderByCreatedAtAsc(organizationId, departmentId);
        }
        return workerRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
    }

    private static boolean matchesSearch(WorkerEntity workerEntity, String normalizedSearch) {
        if (StringUtils.isBlank(normalizedSearch)) {
            return true;
        }
        String displayName = StringUtils.defaultString(workerEntity.getDisplayName()).toLowerCase();
        String email = StringUtils.defaultString(workerEntity.getEmail()).toLowerCase();
        return displayName.contains(normalizedSearch) || email.contains(normalizedSearch);
    }

    private void validateBranchAndDepartmentBelongToOrganization(Long organizationId, Long branchId, Long departmentId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch does not belong to organization"));
        DepartmentEntity departmentEntity = departmentRepository.findByIdAndBranchId(departmentId, branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not belong to branch"));
        if (!Objects.equals(departmentEntity.getOrganizationId(), organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not belong to organization");
        }
    }

    private List<WorkerResponse> mapToWorkerResponses(List<WorkerEntity> workers) {
        List<Long> branchIds = workers.stream()
                .map(WorkerEntity::getBranchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> departmentIds = workers.stream()
                .map(WorkerEntity::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, BranchEntity> branchesById = branchIds.isEmpty()
                ? Map.of()
                : branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, Function.identity()));
        Map<Long, DepartmentEntity> departmentsById = departmentIds.isEmpty()
                ? Map.of()
                : departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, Function.identity()));

        return workers.stream()
                .map(workerEntity -> {
                    BranchEntity branchEntity = Objects.nonNull(workerEntity.getBranchId())
                            ? branchesById.get(workerEntity.getBranchId())
                            : null;
                    DepartmentEntity departmentEntity = Objects.nonNull(workerEntity.getDepartmentId())
                            ? departmentsById.get(workerEntity.getDepartmentId())
                            : null;
                    return new WorkerResponse(
                            workerEntity.getId(),
                            workerEntity.getDisplayName(),
                            workerEntity.getEmail(),
                            workerEntity.getBranchId(),
                            Objects.nonNull(branchEntity) ? branchEntity.getName() : null,
                            workerEntity.getDepartmentId(),
                            Objects.nonNull(departmentEntity) ? departmentEntity.getName() : null,
                            workerEntity.getStatus().name(),
                            workerEntity.isAgentInstalled(),
                            workerEntity.getAgentVersion(),
                            workerEntity.getAccessKeyPrefix(),
                            workerEntity.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}
