package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.web.dto.CreateBranchRequest;
import com.workpulsetracker.server.web.dto.CreateDepartmentRequest;
import com.workpulsetracker.server.web.dto.StructureResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StructureService {

    private static final Logger logger = LoggerFactory.getLogger(StructureService.class);
    private static final String schema = "public";

    private final OrganizationService organizationService;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;

    public StructureService(
            OrganizationService organizationService,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository
    ) {
        this.organizationService = organizationService;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public StructureResponse getStructure(AppUserEntity currentUser) {
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        List<BranchEntity> branches =
                branchRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationEntity.getId());
        List<UUID> branchIds = branches.stream().map(BranchEntity::getId).collect(Collectors.toList());
        Map<UUID, List<DepartmentEntity>> departmentsByBranchId = branchIds.isEmpty()
                ? Map.of()
                : departmentRepository.findByBranchIdInOrderByCreatedAtAsc(branchIds).stream()
                .collect(Collectors.groupingBy(DepartmentEntity::getBranchId));

        List<StructureResponse.BranchNode> branchNodes = branches.stream()
                .map(branchEntity -> new StructureResponse.BranchNode(
                        branchEntity.getId(),
                        branchEntity.getName(),
                        departmentsByBranchId.getOrDefault(branchEntity.getId(), List.of()).stream()
                                .map(departmentEntity -> new StructureResponse.DepartmentNode(
                                        departmentEntity.getId(),
                                        departmentEntity.getName()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        return new StructureResponse(branchNodes);
    }

    @Transactional
    public StructureResponse.BranchNode createBranch(AppUserEntity currentUser, CreateBranchRequest createBranchRequest) {
        organizationService.requireOwner(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        String branchName = createBranchRequest.name().trim();
        if (branchRepository.existsByOrganizationIdAndNameIgnoreCase(organizationEntity.getId(), branchName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch with this name already exists");
        }
        BranchEntity branchEntity = branchRepository.save(new BranchEntity(
                UUID.randomUUID(),
                organizationEntity.getId(),
                branchName,
                OffsetDateTime.now()
        ));
        logger.info("schema={} Created branch {} for organization {}", schema, branchName, organizationEntity.getId());
        return new StructureResponse.BranchNode(branchEntity.getId(), branchEntity.getName(), List.of());
    }

    @Transactional
    public StructureResponse.DepartmentNode createDepartment(
            AppUserEntity currentUser,
            CreateDepartmentRequest createDepartmentRequest
    ) {
        organizationService.requireOwner(currentUser);
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        BranchEntity branchEntity = branchRepository
                .findByIdAndOrganizationId(createDepartmentRequest.branchId(), organizationEntity.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        String departmentName = createDepartmentRequest.name().trim();
        if (departmentRepository.existsByBranchIdAndNameIgnoreCase(branchEntity.getId(), departmentName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department with this name already exists");
        }
        DepartmentEntity departmentEntity = departmentRepository.save(new DepartmentEntity(
                UUID.randomUUID(),
                branchEntity.getId(),
                departmentName,
                OffsetDateTime.now()
        ));
        logger.info(
                "schema={} Created department {} in branch {}",
                schema,
                departmentName,
                branchEntity.getId()
        );
        return new StructureResponse.DepartmentNode(departmentEntity.getId(), departmentEntity.getName());
    }
}
