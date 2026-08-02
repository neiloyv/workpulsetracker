package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    List<DepartmentEntity> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    List<DepartmentEntity> findByBranchIdOrderByCreatedAtAsc(Long branchId);

    List<DepartmentEntity> findByBranchIdInOrderByCreatedAtAsc(Collection<Long> branchIds);

    Optional<DepartmentEntity> findByIdAndBranchId(Long id, Long branchId);

    Optional<DepartmentEntity> findByOrganizationIdAndIsDefaultTrue(Long organizationId);

    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);
}
