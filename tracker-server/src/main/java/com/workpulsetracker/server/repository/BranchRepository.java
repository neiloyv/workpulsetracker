package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

    List<BranchEntity> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    Optional<BranchEntity> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<BranchEntity> findByOrganizationIdAndIsDefaultTrue(Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
}
