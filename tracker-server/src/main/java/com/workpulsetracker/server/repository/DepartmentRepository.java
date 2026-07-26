package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    List<DepartmentEntity> findByBranchIdOrderByCreatedAtAsc(UUID branchId);

    List<DepartmentEntity> findByBranchIdInOrderByCreatedAtAsc(Collection<UUID> branchIds);

    Optional<DepartmentEntity> findByIdAndBranchId(UUID id, UUID branchId);

    boolean existsByBranchIdAndNameIgnoreCase(UUID branchId, String name);
}
