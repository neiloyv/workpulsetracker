package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    List<BranchEntity> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    Optional<BranchEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
