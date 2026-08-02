package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<WorkerEntity, Long> {

    List<WorkerEntity> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    List<WorkerEntity> findByOrganizationIdAndBranchIdOrderByCreatedAtAsc(Long organizationId, Long branchId);

    List<WorkerEntity> findByOrganizationIdAndDepartmentIdOrderByCreatedAtAsc(Long organizationId, Long departmentId);

    List<WorkerEntity> findByOrganizationIdAndBranchIdAndDepartmentIdOrderByCreatedAtAsc(
            Long organizationId,
            Long branchId,
            Long departmentId
    );

    Optional<WorkerEntity> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<WorkerEntity> findByAccessKey(String accessKey);

    Optional<WorkerEntity> findByOrganizationIdAndEmailIgnoreCase(Long organizationId, String email);
}
