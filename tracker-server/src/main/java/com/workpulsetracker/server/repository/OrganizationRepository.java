package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {
}
