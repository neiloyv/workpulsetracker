package com.timetracker.server.repository;

import com.timetracker.server.domain.AgentKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentKeyRepository extends JpaRepository<AgentKeyEntity, UUID> {

    Optional<AgentKeyEntity> findByUserIdAndRevokedAtIsNull(UUID userId);
}
